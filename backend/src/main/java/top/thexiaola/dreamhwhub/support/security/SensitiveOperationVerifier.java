package top.thexiaola.dreamhwhub.support.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.support.password.PasswordUtil;

/**
 * 敏感操作身份二次验证器
 * <p>
 * 解散班级、退出学校、踢出成员，以及平台管理员的高危/权限类操作，都要求操作者本人
 * 再次确认身份：凭「登录密码」或「邮箱验证码」之一即可。校验失败属于用户输入错误
 * （返回 400），而非登录失效，避免前端误触发登出。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveOperationVerifier {

    /** 验证方式：登录密码 */
    public static final String METHOD_PASSWORD = "password";
    /** 验证方式：邮箱验证码 */
    public static final String METHOD_EMAIL_CODE = "email_code";

    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final EmailService emailService;

    /**
     * 向当前操作用户的绑定邮箱发送「敏感操作」身份验证码
     *
     * @param user 操作者（当前登录用户）
     * @return 验证码发送冷却时间（秒），供前端展示倒计时
     */
    public int sendCode(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }
        if (dbUser.getEmail() == null || dbUser.getEmail().isBlank()) {
            throw new BusinessException(BusinessErrorCode.OPERATION_VERIFICATION_FAILED,
                    "当前账号未绑定邮箱，无法使用邮箱验证码，请改用登录密码验证", null);
        }
        emailService.sendSensitiveOperationCode(dbUser.getEmail(), dbUser.getUsername());
        return emailService.getCooldownSeconds();
    }

    /**
     * 校验敏感操作的二次验证凭据。
     * <p>
     * 是否验证、以及允许哪些方式，由用户自身的「安全验证设置」决定：
     * <ul>
     *   <li>两种方式都关闭：不进行验证（直接放行）；</li>
     *   <li>仅开启密码：只能使用登录密码；</li>
     *   <li>仅开启邮箱验证码：只能使用邮箱验证码；</li>
     *   <li>两种都开启：任一种通过即可。</li>
     * </ul>
     *
     * @param user      操作者（当前登录用户）
     * @param method    验证方式：password / email_code（为空时按 password 处理）
     * @param password  登录密码（method=password 时）
     * @param emailCode 邮箱验证码（method=email_code 时）
     * @throws BusinessException 未提供/不支持的验证方式，或使用了已关闭的方式，或凭据错误时抛出
     */
    public void verify(User user, String method, String password, String emailCode) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 回库取最新用户，确保密码哈希、邮箱与安全设置均为最新值
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }

        boolean byPassword = Boolean.TRUE.equals(dbUser.getVerifyByPassword());
        boolean byEmail = Boolean.TRUE.equals(dbUser.getVerifyByEmailCode());

        // 两种方式都关闭：关闭危险操作验证功能，直接放行
        if (!byPassword && !byEmail) {
            log.info("Sensitive operation allowed for user {} (verification disabled)", dbUser.getUsername());
            return;
        }

        String normalized = trim(method);
        if (normalized == null) {
            // 未声明方式时按密码处理，兼容仅传密码的调用
            normalized = METHOD_PASSWORD;
        }

        switch (normalized) {
            case METHOD_PASSWORD -> {
                if (!byPassword) {
                    throw new BusinessException(BusinessErrorCode.OPERATION_VERIFICATION_METHOD_DISABLED,
                            "你已关闭密码验证，请使用邮箱验证码验证身份", null);
                }
                verifyByPassword(dbUser, password);
            }
            case METHOD_EMAIL_CODE -> {
                if (!byEmail) {
                    throw new BusinessException(BusinessErrorCode.OPERATION_VERIFICATION_METHOD_DISABLED,
                            "你已关闭邮箱验证码验证，请使用登录密码验证身份", null);
                }
                verifyByEmailCode(dbUser, emailCode);
            }
            default -> throw new BusinessException(BusinessErrorCode.OPERATION_VERIFICATION_METHOD_UNSUPPORTED,
                    "不支持的身份验证方式：" + normalized, null);
        }

        log.info("Sensitive operation verified for user {} via {}", dbUser.getUsername(), normalized);
    }

    /**
     * 校验「修改安全验证设置」时的凭据：**每个被改动的开关都必须用该方式自身的凭据验证身份**。
     * <p>
     * 与普通危险操作不同，此处不看当前开关是否开启，而是看本次变更是否改动了对应开关：
     * 改动密码验证 → 校验登录密码；改动邮箱验证码验证 → 校验邮箱验证码；两者都改动则两者都校验。
     * 未改动的方式无需凭据。
     *
     * @param user            操作者（当前登录用户）
     * @param passwordChanged 本次是否改动了「密码验证」开关
     * @param password        登录密码（passwordChanged 时必填）
     * @param emailChanged    本次是否改动了「邮箱验证码验证」开关
     * @param emailCode       邮箱验证码（emailChanged 时必填）
     * @throws BusinessException 需要验证但未提供凭据(6100)或凭据错误(6101)时抛出
     */
    public void verifyForSettingChange(User user, boolean passwordChanged, String password,
                                       boolean emailChanged, String emailCode) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }
        if (!passwordChanged && !emailChanged) {
            // 未改动任何开关：无需验证
            return;
        }

        // 回库取最新用户，确保密码哈希与邮箱为最新值
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }

        if (passwordChanged) {
            verifyByPassword(dbUser, password);
        }
        if (emailChanged) {
            verifyByEmailCode(dbUser, emailCode);
        }

        log.info("Security verification settings change verified for user {} (passwordChanged={}, emailChanged={})",
                dbUser.getUsername(), passwordChanged, emailChanged);
    }

    private void verifyByPassword(User user, String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(BusinessErrorCode.OPERATION_VERIFICATION_REQUIRED,
                    "请输入登录密码以验证身份", null);
        }
        if (!passwordUtil.matches(password, user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.OPERATION_VERIFICATION_FAILED,
                    "登录密码错误，身份验证失败", null);
        }
    }

    private void verifyByEmailCode(User user, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(BusinessErrorCode.OPERATION_VERIFICATION_REQUIRED,
                    "请输入邮箱验证码以验证身份", null);
        }
        boolean ok = emailService.verifySensitiveOperationCode(user.getEmail(), code, user.getUsername());
        if (!ok) {
            throw new BusinessException(BusinessErrorCode.OPERATION_VERIFICATION_FAILED,
                    "邮箱验证码错误或已过期，身份验证失败", null);
        }
    }

    private String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
