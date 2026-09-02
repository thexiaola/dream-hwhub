package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.RegisterUserService;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.password.PasswordUtil;

import java.time.LocalDateTime;

/**
 * 用户注册服务实现类 - 使用BCrypt加密密码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserServiceImpl implements RegisterUserService {
    
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final PasswordUtil passwordUtil;

    @Override
    public User register(RegisterRequest registerRequest) {
        String operation = "User registration";
        
        String userNo = registerRequest.getUserNo();
        String username = registerRequest.getUsername();
        String email = registerRequest.getEmail();

        // 学号允许重复，仅校验用户名与邮箱唯一
        checkUsernameExists(username);
        checkEmailExists(email);

        if (!verifyEmailCode(registerRequest.getEmail(), registerRequest.getEmailCode(), userNo, username)) {
            throw new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID, "验证码无效或已过期", null);
        }

        User user = new User();
        user.setUserNo(userNo);
        user.setUsername(username);
        user.setEmail(email);
        // 使用BCrypt加密密码
        user.setPassword(passwordUtil.encode(registerRequest.getPassword()));
        user.setPermission((short) 1);
        user.setIsBanned(false);
        LocalDateTime timeNow = LocalDateTime.now();
        user.setRegisterTime(timeNow);
        user.setLastLoginTime(timeNow);

        try {
            userMapper.insert(user);
            return user;
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "database insert failed: " + e.getMessage(), user), e);
            throw new BusinessException(BusinessErrorCode.REGISTRATION_FAILED, "注册失败：" + e.getMessage());
        }
    }

    @Override
    public void sendEmailCode(String email, String userNo, String username) {
        String operation = "Send registration verification code";

        // 学号允许重复，仅校验用户名与邮箱唯一
        checkUsernameExists(username);
        checkEmailExists(email);
            
        try {
            emailService.sendVerificationCode(email, userNo, username);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "failed to send verification code: " + e.getMessage(), null), e);
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "验证码发送失败：" + e.getMessage());
        }
    }

    private void checkUsernameExists(String username) {
        if (isUsernameExists(username)) {
            throw new BusinessException(BusinessErrorCode.USERNAME_EXISTS, "用户名已存在", null);
        }
    }

    private void checkEmailExists(String email) {
        if (isEmailExists(email)) {
            throw new BusinessException(BusinessErrorCode.EMAIL_EXISTS, "邮箱已存在", null);
        }
    }

    /**
     * 验证注册验证码（需要匹配 userNo、username、email）
     */
    @Override
    public boolean verifyEmailCode(String email, String code, String userNo, String username) {
        return emailService.verifyRegistrationCode(email, code, userNo, username);
    }

    @Override
    public boolean isUsernameExists(String username) {
        // 用户名不区分大小写唯一：TheXiaoLa 占用后，thexiaola/Thexiaola 等均视为重复
        return userMapper.selectCount(
                new QueryWrapper<User>().apply("LOWER(username) = LOWER({0})", username)
        ) > 0;
    }

    @Override
    public boolean isEmailExists(String email) {
        return userMapper.selectCount(
                new QueryWrapper<User>().eq("email", email)
        ) > 0;
    }
}
