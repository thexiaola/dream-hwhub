package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;

/**
 * 用户登录控制器
 */
@RestController
@RequestMapping("/api/users")
public class LoginUserController {
    private static final Logger log = LoggerFactory.getLogger(LoginUserController.class);
    private final LoginUserService loginUserService;
    
    public LoginUserController(LoginUserService loginUserService) {
        this.loginUserService = loginUserService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(HttpServletRequest request, @Valid @RequestBody LoginRequest loginRequest) {
        String ip = LogUtil.getCurrentClientIp();
        
        try {
            User user = loginUserService.login(loginRequest, request);
            UserResponse userResponse = UserResponse.fromEntity(user);

            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) login successful, session created", userInfo);
            
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (BusinessException e) {
            // 统一返回 INVALID_CREDENTIALS
            return ResponseEntity.status(401).body(ApiResponse.error(
                BusinessErrorCode.INVALID_CREDENTIALS.getCode(),
                BusinessErrorCode.INVALID_CREDENTIALS.getMessage()
            ));
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
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            log.warn("User logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("User logout failed", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "登出失败"));
        }
    }

}