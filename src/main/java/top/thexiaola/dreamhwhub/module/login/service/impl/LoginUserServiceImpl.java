package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.AESEncryptionUtil;
import top.thexiaola.dreamhwhub.util.LogUtil;

import java.time.LocalDateTime;

/**
 * 用户登录服务实现类
 */
@Service
public class LoginUserServiceImpl implements LoginUserService {

    private static final Logger log = LoggerFactory.getLogger(LoginUserServiceImpl.class);
    
    private final UserMapper userMapper;

    private final AESEncryptionUtil aesEncryptionUtil;

    public LoginUserServiceImpl(AESEncryptionUtil aesEncryptionUtil, UserMapper userMapper) {
        this.aesEncryptionUtil = aesEncryptionUtil;
        this.userMapper = userMapper;
    }

    @Override
    public User login(LoginRequest loginRequest) {
        String operation = "User login";
            
        User user = findByAccount(loginRequest.getAccount());
        if (user == null) {
            log.warn(LogUtil.getFailureLog(operation, "invalid account or password: " + loginRequest.getAccount(), null));
            throw new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS, "账号或密码错误", null);
        }
        
        // 检查用户是否被封禁
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            log.warn(LogUtil.getFailureLog(operation, "user is banned", user));
            throw new BusinessException(BusinessErrorCode.USER_BANNED, "用户已被封禁", null);
        }
        
        if (aesEncryptionUtil.verifyPassword(loginRequest.getPassword(), user.getPassword())) {
            log.info(LogUtil.getSuccessLog(operation + " - password verified", user));
            
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
            
            return user;
        } else {
            log.warn(LogUtil.getFailureLog(operation, "password mismatch for user: " + loginRequest.getAccount(), user));
            throw new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS, "账号或密码错误", null);
        }
    }

    @Override
    public User findByAccount(String account) {
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("user_no", account)
        );
        if (user != null) return user;

        user = userMapper.selectOne(
                new QueryWrapper<User>().eq("username", account)
        );
        if (user != null) return user;

        user = userMapper.selectOne(
                new QueryWrapper<User>().eq("email", account)
        );
        return user;
    }
}
