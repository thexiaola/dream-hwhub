package top.thexiaola.dreamhwhub.module.login.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.util.AESEncryptionUtil;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;

import jakarta.annotation.PostConstruct;

/**
 * 用户登录服务实现类
 */
@Service
public class LoginUserServiceImpl implements LoginUserService {

    private static final Logger log = LoggerFactory.getLogger(LoginUserServiceImpl.class);
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AESEncryptionUtil aesEncryptionUtil;

    @Value("${app.verification-code.expiry-minutes:30}")
    private int expiryMinutes;

    @PostConstruct
    public void init() {
        log.info("LoginUserService initialized");
    }

    @Override
    public User register(RegisterRequest registerRequest) {
        String operation = "User registration";
        
        // 检查唯一性
        if (isUserNoExists(registerRequest.getUserNo())) {
            log.warn(LogUtil.getFailureLog(operation, "user_no already exists: " + registerRequest.getUserNo(), null));
            throw new RuntimeException("学号已被注册");
        }

        if (isUsernameExists(registerRequest.getUsername())) {
            log.warn(LogUtil.getFailureLog(operation, "username already exists: " + registerRequest.getUsername(), null));
            throw new RuntimeException("用户名已被注册");
        }

        if (isEmailExists(registerRequest.getEmail())) {
            log.warn(LogUtil.getFailureLog(operation, "email already exists: " + registerRequest.getEmail(), null));
            throw new RuntimeException("邮箱已被注册");
        }

        // 验证邮箱验证码
        if (!emailService.verifyCode(registerRequest.getEmail(), registerRequest.getEmailCode())) {
            log.warn(LogUtil.getFailureLog(operation, "invalid or expired email verification code", null));
            throw new RuntimeException("邮箱验证码无效或已过期");
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
            log.info(LogUtil.getSuccessLog(operation, user));
            
            return user;
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "database insert failed: " + e.getMessage(), user), e);
            throw new RuntimeException("注册失败: " + e.getMessage());
        }
    }

    @Override
    public User login(LoginRequest loginRequest) {
        String operation = "User login";
        
        User user = findByAccount(loginRequest.getAccount());
        if (user == null) {
            log.warn(LogUtil.getFailureLog(operation, "user not found: " + loginRequest.getAccount(), null));
            throw new RuntimeException("账号或密码错误");
        }

        // 由于AES加密是不可逆的，我们无法直接验证密码
        // 这里抛出异常表示不支持密码验证
        log.warn(LogUtil.getFailureLog(operation, "password verification not supported for irreversible encryption", user));
        throw new RuntimeException("系统不支持密码验证功能");
    }

    @Override
    public void sendEmailCode(String email) {
        String operation = "Send email verification code";
        
        try {
            emailService.sendVerificationCode(email);
            log.info(LogUtil.getSuccessLog(operation + " to " + email, null));
        } catch (Exception e) {
            log.error(LogUtil.getFailureLog(operation, "email sending failed: " + e.getMessage(), null), e);
            throw new RuntimeException("验证码发送失败: " + e.getMessage());
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