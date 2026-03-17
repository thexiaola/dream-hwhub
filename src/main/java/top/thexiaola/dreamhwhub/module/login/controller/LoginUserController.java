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
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.SessionManager;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> login(HttpServletRequest request, @Valid @RequestBody LoginRequest loginRequest) {
        String ip = LogUtil.getCurrentClientIp();
        
        ServiceResult<User> result = loginUserService.login(loginRequest);
        
        if (result.isSuccess()) {
            User user = result.getData();
            UserResponse userResponse = UserResponse.fromEntity(user);

            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) login successful, generating JWT token", userInfo);

            // 使用 Session 管理
            SessionManager.addSession(user.getId(), request.getSession());
            request.getSession().setAttribute("user", user);
            request.getSession().setAttribute("username", user.getUsername());
            
            return buildSuccessResponse(userResponse);
        } else {
            String errorMessage = result.getMessage();
            return buildErrorResponse(errorMessage);
        }
    }

    /**
     * 成功响应
     */
    private ResponseEntity<Map<String, Object>> buildSuccessResponse(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 200);
        response.put("msg", "登录成功！");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * 错误响应
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 401);
        response.put("msg", message);
        response.put("data", null);
        return ResponseEntity.status(401).body(response);
    }
}