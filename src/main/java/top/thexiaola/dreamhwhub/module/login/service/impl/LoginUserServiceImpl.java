package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.support.jwt.JwtUtil;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.password.PasswordUtil;

import java.time.LocalDateTime;

/**
 * 用户登录服务实现类 - 使用JWT Token认证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUserServiceImpl implements LoginUserService {
    
    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    @Override
    public User login(LoginRequest loginRequest, HttpServletRequest request) {
        String operation = "User login";
            
        User user = findByAccount(loginRequest.getAccount());
        if (user == null) {
            log.warn(LogUtil.getFailureLog(operation, "invalid account or password: " + loginRequest.getAccount(), null));
            throw new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS, "账号或密码错误", null);
        }
        
        // 检查用户是否被封禁
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            String banReason = user.getBanReason() != null ? user.getBanReason() : "未提供封禁原因";
            log.warn(LogUtil.getFailureLog(operation, "user is banned, reason: " + banReason, user));
            throw new BusinessException(BusinessErrorCode.USER_BANNED, "用户已被封禁：" + banReason, null);
        }
        
        // 使用BCrypt验证密码
        if (passwordUtil.matches(loginRequest.getPassword(), user.getPassword())) {
            log.info(LogUtil.getSuccessLog(operation + " - password verified", user));
            
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);
            
            // 清除敏感信息(不返回密码哈希)
            String passwordHash = user.getPassword();
            user.setPassword(null);
            
            return user;
        } else {
            log.warn(LogUtil.getFailureLog(operation, "password mismatch for user: " + loginRequest.getAccount(), user));
            throw new BusinessException(BusinessErrorCode.INVALID_CREDENTIALS, "账号或密码错误", null);
        }
    }

    @Override
    public User findByAccount(String account) {
        return getUser(account, userMapper);
    }

    static User getUser(String account, UserMapper userMapper) {
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

    @Override
    public void logout(Integer userId, HttpServletRequest request) {
        String operation = "User logout";
        
        if (userId == null) {
            log.warn(LogUtil.getFailureLog(operation, "user ID is null", null));
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }
        
        // JWT是无状态的，客户端只需删除Token即可
        // 服务端无需额外操作，Token会在过期后自动失效
        log.info(LogUtil.getSuccessLog(operation, null));
    }
    
    @Override
    public User getCurrentUser(HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute("user");
        if (currentUser == null) {
            log.warn(LogUtil.getFailureLog("Get current user", "user not logged in", null));
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }
        return currentUser;
    }
}
