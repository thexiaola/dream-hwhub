package top.thexiaola.dreamhwhub.module.login.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmail(String to, String subject, String content);

    /**
     * 生成并发送验证码（用于注册，绑定 username、email）
     * @param email 收件人邮箱
     * @param username 用户名
     */
    void sendVerificationCode(String email, String username);

    /**
     * 验证注册验证码 (需要匹配 username、email)
     * @param email 邮箱地址
     * @param code 验证码
     * @param username 用户名
     * @return 验证是否成功
     */
    boolean verifyRegistrationCode(String email, String code, String username);

    /**
     * 验证换绑验证码 (需要匹配 username、email)
     * @param email 邮箱地址
     * @param code 验证码
     * @param username 用户名
     * @return 验证是否成功
     */
    boolean verifyModifyCode(String email, String code, String username);

    /**
     * 生成并发送换绑验证码
     * @param email 收件人邮箱
     * @param username 用户名
     */
    void sendModifyEmailCode(String email, String username);

    /**
     * 生成并发送找回密码验证码
     * @param email 收件人邮箱
     * @param username 用户名
     */
    void sendRetrievePasswordEmailCode(String email, String username);

    /**
     * 验证找回密码验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @param username 用户名
     * @return 验证是否成功
     */
    boolean verifyRetrievePasswordCode(String email, String code, String username);

    /**
     * 生成并发送「敏感操作」身份验证码（用于解散班级、退出学校、踢出成员、管理员高危操作等的二次验证）
     * @param email 收件人邮箱
     * @param username 用户名
     */
    void sendSensitiveOperationCode(String email, String username);

    /**
     * 验证「敏感操作」身份验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @param username 用户名
     * @return 验证是否成功
     */
    boolean verifySensitiveOperationCode(String email, String code, String username);

    /**
     * 获取验证码发送冷却时间（秒），前端用于展示倒计时
     * @return 冷却秒数
     */
    int getCooldownSeconds();
}
