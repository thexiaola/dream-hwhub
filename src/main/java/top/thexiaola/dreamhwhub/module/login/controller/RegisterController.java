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
import top.thexiaola.dreamhwhub.module.login.dto.EmailCodeRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.service.RegisterUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.SessionManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户注册控制器
 */
@RestController
@RequestMapping("/api/users")
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);
    
    private final RegisterUserService registerUserService;

    public RegisterController(RegisterUserService registerUserService) {
        this.registerUserService = registerUserService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(HttpServletRequest request, @Valid @RequestBody RegisterRequest registerRequest) {
        ServiceResult<User> result = registerUserService.register(registerRequest);
        
        if (result.isSuccess()) {
            User user = result.getData();
            UserResponse userResponse = UserResponse.fromEntity(user);
        
            String ip = LogUtil.getCurrentClientIp();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) registration successful, auto-login initiated", userInfo);
            SessionManager.addSession(user.getId(), request.getSession());
            request.getSession().setAttribute("user", user);
            request.getSession().setAttribute("username", user.getUsername());
            
            Map<String, Object> responseData = createUserLoginResponse(userResponse);
            Map<String, Object> response = createSuccessResponse("注册成功并已自动登录！", responseData);
            
            return ResponseEntity.ok(response);
        } else {
            String errorMessage = result.getMessage();
            Map<String, Object> response = createErrorResponse(errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送注册验证码
     */
    @PostMapping("/getregcode")
    public ResponseEntity<Map<String, Object>> sendRegisterCode(@Valid @RequestBody EmailCodeRequest emailCodeRequest) {
        ServiceResult<Void> result = registerUserService.sendEmailCode(emailCodeRequest.getEmail(), emailCodeRequest.getUserNo(), emailCodeRequest.getUsername());

        if (result.isSuccess()) {
            Map<String, Object> response = createSuccessResponse("验证码发送成功！", null);
            return ResponseEntity.ok(response);
        } else {
            String errorMessage = result.getMessage();
            Map<String, Object> response = createErrorResponse(errorMessage);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 用户登录响应数据
     */
    private Map<String, Object> createUserLoginResponse(UserResponse userResponse) {
        Map<String, Object> responseData = createBaseResponseMap();
        responseData.put("user", userResponse);
        responseData.put("isLoggedIn", true);
        return responseData;
    }

    /**
     * 成功响应
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = createBaseResponseMap();
        response.put("code", 200);
        response.put("msg", message);
        response.put("data", data);
        return response;
    }

    /**
     * 错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = createBaseResponseMap();
        response.put("code", 400);
        response.put("msg", message);
        response.put("data", null);
        return response;
    }

    /**
     * 基础响应Map
     */
    private Map<String, Object> createBaseResponseMap() {
        return new LinkedHashMap<>();
    }
}