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
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.SessionManager;


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
            User user = loginUserService.login(loginRequest);
            UserResponse userResponse = UserResponse.fromEntity(user);

            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) login successful, session created", userInfo);

            // 使用 Session 管理
            SessionManager.addSession(user.getId(), request.getSession());
            request.getSession().setAttribute("user", user);
            request.getSession().setAttribute("username", user.getUsername());
            
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (BusinessException e) {
            String errorMessage = e.getMessage();
            return ResponseEntity.status(401).body(ApiResponse.error(401, errorMessage));
        }
    }

}