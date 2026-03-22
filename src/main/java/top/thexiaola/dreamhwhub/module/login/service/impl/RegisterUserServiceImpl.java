package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.RegisterUserService;
import top.thexiaola.dreamhwhub.util.AESEncryptionUtil;
import top.thexiaola.dreamhwhub.util.LogUtil;

import java.time.LocalDateTime;

/**
 * 用户注册服务实现类
 */
@Service
public class RegisterUserServiceImpl implements RegisterUserService {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserServiceImpl.class);
    
    private final UserMapper userMapper;

    private final EmailService emailService;

    private final AESEncryptionUtil aesEncryptionUtil;

    public RegisterUserServiceImpl(AESEncryptionUtil aesEncryptionUtil, UserMapper userMapper, EmailService emailService) {
        this.aesEncryptionUtil = aesEncryptionUtil;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Override
    public User register(RegisterRequest registerRequest) {
        String operation = "User registration";
        
        if (isUserNoExists(registerRequest.getUserNo())) {
            log.info(LogUtil.getFailureLog(operation, "user_no already exists: " + registerRequest.getUserNo(), null));
            throw new BusinessException(BusinessErrorCode.USER_NO_EXISTS, "学号已存在", null);
        }

        if (isUsernameExists(registerRequest.getUsername())) {
            log.info(LogUtil.getFailureLog(operation, "username already exists: " + registerRequest.getUsername(), null));
            throw new BusinessException(BusinessErrorCode.USERNAME_EXISTS, "用户名已存在", null);
        }

        if (isEmailExists(registerRequest.getEmail())) {
            log.info(LogUtil.getFailureLog(operation, "email already exists: " + registerRequest.getEmail(), null));
            throw new BusinessException(BusinessErrorCode.EMAIL_EXISTS, "邮箱已存在", null);
        }

        if (!verifyEmailCode(registerRequest.getEmail(), registerRequest.getEmailCode(), registerRequest.getUserNo(), registerRequest.getUsername())) {
            log.info(LogUtil.getFailureLog(operation, "invalid or expired email verification code", null));
            throw new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID, "验证码无效或已过期", null);
        }

        User user = new User();
        user.setUserNo(registerRequest.getUserNo());
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(aesEncryptionUtil.encrypt(registerRequest.getPassword()));
        user.setPermission((short) 1);
        user.setIsBanned(false);
        LocalDateTime timeNow = LocalDateTime.now();
        user.setRegisterTime(timeNow);
        user.setLastLoginTime(timeNow);

        try {
            userMapper.insert(user);
            log.info(LogUtil.getSuccessLog(operation + " - user created in database", user));
            return user;
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "database insert failed: " + e.getMessage(), user), e);
            throw new BusinessException(BusinessErrorCode.REGISTRATION_FAILED, "注册失败：" + e.getMessage());
        }
    }

    @Override
    public void sendEmailCode(String email, String userNo, String username) {
        String operation = "Send registration verification code";

        if (isUserNoExists(userNo)) {
            log.warn(LogUtil.getFailureLog(operation, "user_no already exists: " + userNo, null));
            throw new BusinessException(BusinessErrorCode.USER_NO_EXISTS, "学号已存在", null);
        }
            
        if (isUsernameExists(username)) {
            log.warn(LogUtil.getFailureLog(operation, "username already exists: " + username, null));
            throw new BusinessException(BusinessErrorCode.USERNAME_EXISTS, "用户名已存在", null);
        }
            
        if (isEmailExists(email)) {
            log.warn(LogUtil.getFailureLog(operation, "email already exists: " + email, null));
            throw new BusinessException(BusinessErrorCode.EMAIL_EXISTS, "邮箱已存在", null);
        }
            
        try {
            try {
            emailService.sendVerificationCode(email, userNo, username);
            log.info(LogUtil.getSuccessLog(operation + " - verification code sent to email: " + email, null));
        } catch (BusinessException e) {
            // 重新抛出异常，由全局异常处理器处理
            throw e;
        }
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "failed to send verification code: " + e.getMessage(), null), e);
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "验证码发送失败：" + e.getMessage());
        }
    }

    /**
     * 验证注册验证码（需要匹配 userNo、username、email）
     */
    @Override
    public boolean verifyEmailCode(String email, String code, String userNo, String username) {
        return emailService.verifyCode(email, code, userNo, username);
    }

    @Override
    public boolean isUserNoExists(String userNo) {
        return userMapper.selectCount(
                new QueryWrapper<User>().eq("user_no", userNo)
        ) > 0;
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userMapper.selectCount(
                new QueryWrapper<User>().eq("username", username)
        ) > 0;
    }

    @Override
    public boolean isEmailExists(String email) {
        return userMapper.selectCount(
                new QueryWrapper<User>().eq("email", email)
        ) > 0;
    }
}
