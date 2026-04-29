package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.mapper.UserMapper;

/**
 * 用户登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class LoginUserController {
    private final LoginUserService loginUserService;
    private final UserMapper userResponseMapper;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(HttpServletRequest request, @Valid @RequestBody LoginRequest loginRequest) {
        String ip = LogUtil.getCurrentClientIp();
        
        try {
            User user = loginUserService.login(loginRequest, request);
            UserResponse userResponse = userResponseMapper.toUserResponse(user);

            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) login successful, session created", userInfo);
            
            return ResponseEntity.ok(ApiResponse.success(userResponse, "登录成功"));
        } catch (BusinessException e) {
            // 区分不同的错误类型
            if (e.getErrorCode() == BusinessErrorCode.USER_BANNED) {
                String userInfo = String.format("ip: %s, account: %s", ip, loginRequest.getAccount());
                log.warn("User ({}) login failed: account is banned, reason: {}", userInfo, e.getMessage());
                return ResponseEntity.status(403).body(ApiResponse.error(
                    BusinessErrorCode.USER_BANNED.getCode(),
                    e.getMessage()
                ));
            } else {
                // 其他错误统一返回 INVALID_CREDENTIALS
                return ResponseEntity.status(401).body(ApiResponse.error(
                    BusinessErrorCode.INVALID_CREDENTIALS.getCode(),
                    BusinessErrorCode.INVALID_CREDENTIALS.getMessage()
                ));
            }
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        
        try {
            // 获取当前用户
            User currentUser = loginUserService.getCurrentUser(request);
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            
            // 登出
            loginUserService.logout(currentUser.getId(), request);
            
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

}