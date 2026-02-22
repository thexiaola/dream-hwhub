package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.JwtUtil;
import top.thexiaola.dreamhwhub.util.LogUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户注册控制器
 */
@RestController
@RequestMapping("/api/users")
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);
    
    private final LoginUserService loginUserService;
    private final JwtUtil jwtUtil;
    
    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    public RegisterController(LoginUserService loginUserService, JwtUtil jwtUtil) {
        this.loginUserService = loginUserService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        String ip = LogUtil.getCurrentClientIp();
        String userInfo = LogUtil.getUserInfoString(ip, null);
        
        ServiceResult<User> result = loginUserService.register(registerRequest);
        
        if (result.isSuccess()) {
            User user = result.getData();
            UserResponse userResponse = UserResponse.fromEntity(user);

            userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) registration successful, auto-login initiated", userInfo);
            
            // 注册成功后自动登录
            String jwtToken = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            Map<String, Object> responseData = createUserLoginResponse(userResponse, jwtToken);
            Map<String, Object> response = createSuccessResponse("注册成功并已自动登录！", responseData);
            
            return ResponseEntity.ok(response);
        } else {
            BusinessErrorCode errorCode = result.getErrorCode();
            String errorMessage = result.getMessage();
            
            if (BusinessErrorCode.isVerificationCodeError(errorCode)) {
                log.warn("User ({}) registration failed due to invalid/expired verification code: {}", userInfo, errorMessage);
            } else if (BusinessErrorCode.isDuplicateRegistrationError(errorCode)) {
                log.info("User ({}) registration failed due to duplicate registration: {}", userInfo, errorMessage);
            } else {
                log.info("User ({}) registration failed: {}", userInfo, errorMessage);
            }
            
            Map<String, Object> response = createErrorResponse(errorMessage);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 发送注册验证码
     */
    @PostMapping("/getregcode")
    public ResponseEntity<Map<String, Object>> sendRegisterCode(@Valid @RequestBody RegisterRequest registerRequest) {
        String ip = LogUtil.getCurrentClientIp();
        String userInfo = LogUtil.getUserInfoString(ip, null);
        String email = registerRequest.getEmail();
        
        ServiceResult<Void> result = loginUserService.sendEmailCode(email);
        
        if (result.isSuccess()) {
            log.info("User ({}) verification code sent successfully to email: {}", userInfo, email);
            
            Map<String, Object> response = createSuccessResponse("验证码发送成功！", null);
            
            return ResponseEntity.ok(response);
        } else {
            String errorMessage = result.getMessage();
            log.warn("User ({}) failed to send verification code to {}: {}", userInfo, email, errorMessage);
            
            Map<String, Object> response = createErrorResponse(errorMessage);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 创建用户登录响应数据
     */
    private Map<String, Object> createUserLoginResponse(UserResponse userResponse, String jwtToken) {
        Map<String, Object> responseData = createBaseResponseMap();
        responseData.put("user", userResponse);
        responseData.put("token", jwtToken);
        responseData.put("tokenType", "Bearer");
        responseData.put("expiresIn", jwtExpiration / 1000);
        responseData.put("isLoggedIn", true);
        return responseData;
    }

    /**
     * 创建成功响应
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = createBaseResponseMap();
        response.put("code", 200);
        response.put("msg", message);
        response.put("data", data);
        return response;
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = createBaseResponseMap();
        response.put("code", 400);
        response.put("msg", message);
        response.put("data", null);
        return response;
    }

    /**
     * 创建基础响应Map
     */
    private Map<String, Object> createBaseResponseMap() {
        return new LinkedHashMap<>();
    }
}