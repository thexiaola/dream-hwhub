package top.thexiaola.dreamhwhub.module.login.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import top.thexiaola.dreamhwhub.util.LogUtil;

import jakarta.validation.Valid;
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

    public RegisterController(LoginUserService loginUserService) {
        this.loginUserService = loginUserService;
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
            
            // 更新userInfo包含注册成功的用户信息
            userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) registration successful", userInfo);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 200);
            response.put("msg", "注册成功！");
            response.put("data", userResponse);
            
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
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 400);
            response.put("msg", errorMessage);
            response.put("data", null);
            
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
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 200);
            response.put("msg", "验证码发送成功！");
            response.put("data", null);
            
            return ResponseEntity.ok(response);
        } else {
            String errorMessage = result.getMessage();
            log.warn("User ({}) failed to send verification code to {}: {}", userInfo, email, errorMessage);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 400);
            response.put("msg", errorMessage);
            response.put("data", null);
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}