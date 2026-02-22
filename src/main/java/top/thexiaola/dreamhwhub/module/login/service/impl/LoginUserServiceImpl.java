package top.thexiaola.dreamhwhub.module.login.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.AESEncryptionUtil;
import top.thexiaola.dreamhwhub.util.LogUtil;

/**
 * 用户登录服务实现类
 */
@Service
public class LoginUserServiceImpl implements LoginUserService {

    private static final Logger log = LoggerFactory.getLogger(LoginUserServiceImpl.class);
    
    private final UserMapper userMapper;

    private final EmailService emailService;

    private final AESEncryptionUtil aesEncryptionUtil;

    public LoginUserServiceImpl(AESEncryptionUtil aesEncryptionUtil, UserMapper userMapper, EmailService emailService) {
        this.aesEncryptionUtil = aesEncryptionUtil;
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Override
    public ServiceResult<User> register(RegisterRequest registerRequest) {
        String operation = "User registration";
        
        // 检查唯一性
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

        // 验证邮箱验证码
        if (!emailService.verifyCode(registerRequest.getEmail(), registerRequest.getEmailCode())) {
            log.warn(LogUtil.getFailureLog(operation, "invalid or expired email verification code", null));
            return ServiceResult.failure(BusinessErrorCode.VERIFICATION_CODE_INVALID);
        }

        // 创建用户
        User user = new User();
        user.setUserNo(registerRequest.getUserNo());
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(aesEncryptionUtil.encrypt(registerRequest.getPassword()));
        user.setPermission((short) 1);

        try {
            userMapper.insert(user);
            log.info(LogUtil.getSuccessLog(operation + " - user created in database", user));
            return ServiceResult.success(user);
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "database insert failed: " + e.getMessage(), user), e);
            return ServiceResult.failure(BusinessErrorCode.REGISTRATION_FAILED, "注册失败: " + e.getMessage());
        }
    }

    @Override
    public ServiceResult<User> login(LoginRequest loginRequest) {
        String operation = "User login";
        
        User user = findByAccount(loginRequest.getAccount());
        if (user == null) {
            log.warn(LogUtil.getFailureLog(operation, "user not found: " + loginRequest.getAccount(), null));
            return ServiceResult.failure(BusinessErrorCode.USER_NOT_FOUND);
        }

        // 使用AES解密验证密码
        if (aesEncryptionUtil.verifyPassword(loginRequest.getPassword(), user.getPassword())) {
            log.info(LogUtil.getSuccessLog(operation + " - password verified", user));
            return ServiceResult.success(user);
        } else {
            log.warn(LogUtil.getFailureLog(operation, "password mismatch for user: " + loginRequest.getAccount(), user));
            return ServiceResult.failure(BusinessErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    public ServiceResult<Void> sendEmailCode(String email) {
        String operation = "Send email verification code";
        
        try {
            emailService.sendVerificationCode(email);
            log.info(LogUtil.getSuccessLog(operation, null));
            return ServiceResult.success(null);
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "email sending failed: " + e.getMessage(), null), e);
            return ServiceResult.failure(BusinessErrorCode.EMAIL_SENDING_FAILED, "验证码发送失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyEmailCode(String email, String code) {
        return emailService.verifyCode(email, code);
    }

    @Override
    public boolean isUserNoExists(String userNo) {
        return userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("user_no", userNo)
        ) > 0;
    }

    @Override
    public boolean isUsernameExists(String username) {
        return userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", username)
        ) > 0;
    }

    @Override
    public boolean isEmailExists(String email) {
        return userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("email", email)
        ) > 0;
    }

    @Override
    public User findByAccount(String account) {
        // 尝试按学号查找
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("user_no", account)
        );
        if (user != null) return user;

        // 尝试按用户名查找
        user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", account)
        );
        if (user != null) return user;

        // 尝试按邮箱查找
        user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("email", account)
        );
        return user;
    }


}