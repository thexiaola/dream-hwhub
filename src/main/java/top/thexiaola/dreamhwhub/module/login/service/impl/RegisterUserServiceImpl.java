package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
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
    public ServiceResult<User> register(RegisterRequest registerRequest) {
        String operation = "User registration";
        
        if (isUserNoExists(registerRequest.getUserNo())) {
            log.warn(LogUtil.getFailureLog(operation, "user_no already exists: " + registerRequest.getUserNo(), null));
            return ServiceResult.failure(BusinessErrorCode.USER_NO_EXISTS);
        }

        if (isUsernameExists(registerRequest.getUsername())) {
            log.warn(LogUtil.getFailureLog(operation, "username already exists: " + registerRequest.getUsername(), null));
            return ServiceResult.failure(BusinessErrorCode.USERNAME_EXISTS);
        }

        if (isEmailExists(registerRequest.getEmail())) {
            log.warn(LogUtil.getFailureLog(operation, "email already exists: " + registerRequest.getEmail(), null));
            return ServiceResult.failure(BusinessErrorCode.EMAIL_EXISTS);
        }

        if (!verifyEmailCode(registerRequest.getEmail(), registerRequest.getEmailCode(), registerRequest.getUserNo(), registerRequest.getUsername())) {
            log.warn(LogUtil.getFailureLog(operation, "invalid or expired email verification code", null));
            return ServiceResult.failure(BusinessErrorCode.VERIFICATION_CODE_INVALID);
        }

        User user = new User();
        user.setUserNo(registerRequest.getUserNo());
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(aesEncryptionUtil.encrypt(registerRequest.getPassword()));
        user.setPermission((short) 1);
        user.setIsBanned(false);
        user.setRegisterTime(LocalDateTime.now());

        try {
            userMapper.insert(user);
            log.info(LogUtil.getSuccessLog(operation + " - user created in database", user));
            return ServiceResult.success(user);
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "database insert failed: " + e.getMessage(), user), e);
            return ServiceResult.failure(BusinessErrorCode.REGISTRATION_FAILED, "注册失败：" + e.getMessage());
        }
    }

    @Override
    public ServiceResult<Void> sendEmailCode(String email, String userNo, String username) {
        String operation = "Send registration verification code";

        if (isUserNoExists(userNo)) {
            log.warn(LogUtil.getFailureLog(operation, "user_no already exists: " + userNo, null));
            return ServiceResult.failure(BusinessErrorCode.USER_NO_EXISTS);
        }
            
        if (isUsernameExists(username)) {
            log.warn(LogUtil.getFailureLog(operation, "username already exists: " + username, null));
            return ServiceResult.failure(BusinessErrorCode.USERNAME_EXISTS);
        }
            
        if (isEmailExists(email)) {
            log.warn(LogUtil.getFailureLog(operation, "email already exists: " + email, null));
            return ServiceResult.failure(BusinessErrorCode.EMAIL_EXISTS);
        }
            
        try {
            ServiceResult<Void> result = emailService.sendVerificationCode(email, userNo, username);
            if (result.isSuccess()) {
                log.info(LogUtil.getSuccessLog(operation + " - verification code sent to email: " + email, null));
                return ServiceResult.success(null);
            } else {
                // 对于邮箱不存在等特定错误，使用服务层返回的详细消息
                String errorMessage = result.getMessage();
                log.warn(LogUtil.getFailureLog(operation, "failed to send verification code: " + errorMessage, null));
                return ServiceResult.failure(result.getErrorCode(), errorMessage, result.getExtraData());
            }
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "failed to send verification code: " + e.getMessage(), null), e);
            return ServiceResult.failure(BusinessErrorCode.EMAIL_SENDING_FAILED, "验证码发送失败：" + e.getMessage());
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
