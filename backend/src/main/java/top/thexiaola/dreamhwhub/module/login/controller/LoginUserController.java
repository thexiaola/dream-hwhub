package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.UserInfoResponse;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.support.jwt.JwtUtil;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

/**
 * 用户登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class LoginUserController {
    private final LoginUserService loginUserService;
    private final UserMapper userMapper;
    private final top.thexiaola.dreamhwhub.support.mapper.UserMapper userResponseMapper;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(HttpServletRequest request,
            @Valid @RequestBody LoginRequest loginRequest) {
        String ip = LogUtil.getCurrentClientIp();

        try {
            User user = loginUserService.login(loginRequest, request);
            UserResponse userResponse = userResponseMapper.toUserResponse(user);

            // 生成JWT Token并设置到响应中
            String token = jwtUtil.generateToken(user);
            userResponse.setToken(token);

            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) login successful, JWT token generated", userInfo);

            return ResponseEntity.ok(ApiResponse.success(userResponse, "登录成功"));
        } catch (BusinessException e) {
            // 区分不同的错误类型
            if (e.getErrorCode() == BusinessErrorCode.USER_BANNED) {
                String userInfo = String.format("ip: %s, account: %s", ip, loginRequest.getAccount());
                log.warn("User ({}) login failed: account is banned, reason: {}", userInfo, e.getMessage());
                return ResponseEntity.status(403).body(ApiResponse.error(
                        BusinessErrorCode.USER_BANNED.getCode(),
                        e.getMessage()));
            } else {
                // 其他错误统一返回 INVALID_CREDENTIALS
                return ResponseEntity.badRequest().body(ApiResponse.error(
                        BusinessErrorCode.INVALID_CREDENTIALS.getCode(),
                        BusinessErrorCode.INVALID_CREDENTIALS.getMessage()));
            }
        }
    }

    /**
     * 用户登出（需携带有效JWT Token，用户由AuthInterceptor解析）
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        String ip = LogUtil.getCurrentClientIp();

        try {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "用户未登录"));
            }
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);

            loginUserService.logout(currentUser.getId());

            log.info("User ({}) logout successful", userInfo);
            return ResponseEntity.ok(ApiResponse.success(null, "登出成功"));
        } catch (BusinessException e) {
            log.warn("User logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("User logout failed", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "登出失败"));
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUserInfo() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录"));
        }
        String userInfo = LogUtil.getUserInfo(currentUser);
        log.info("User ({}) fetched user info", userInfo);

        // 从数据库查询完整用户数据，确保 registerTime/lastLoginTime 等字段为最新值
        User fullUser = userMapper.selectById(currentUser.getId());
        if (fullUser == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "用户不存在"));
        }
        UserInfoResponse userInfoResponse = userResponseMapper.toUserInfoResponse(fullUser);
        return ResponseEntity.ok(ApiResponse.success(userInfoResponse, "获取用户信息成功"));
    }

}