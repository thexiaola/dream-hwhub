package top.thexiaola.dreamhwhub.module.login.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.JwtUtil;
import top.thexiaola.dreamhwhub.util.LogUtil;

import jakarta.validation.Valid;
import java.util.HashMap;
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
    private final JwtUtil jwtUtil;
    
    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    public LoginUserController(LoginUserService loginUserService, JwtUtil jwtUtil) {
        this.loginUserService = loginUserService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String ip = LogUtil.getCurrentClientIp();
        String userInfo = LogUtil.getUserInfoString(ip, null);
        
        ServiceResult<User> result = loginUserService.login(loginRequest);
        
        if (result.isSuccess()) {
            User user = result.getData();
            UserResponse userResponse = UserResponse.fromEntity(user);

            userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) login successful, generating JWT token", userInfo);
            
            // 生成JWT token
            String jwtToken = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("user", userResponse);
            responseData.put("token", jwtToken);
            responseData.put("tokenType", "Bearer");
            responseData.put("expiresIn", jwtExpiration / 1000); // 毫秒 -> 秒
            responseData.put("isLoggedIn", true);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 200);
            response.put("msg", "登录成功！");
            response.put("data", responseData);
            
            return ResponseEntity.ok(response);
        } else {
            String errorMessage = result.getMessage();
            log.info("User ({}) login failed: {}", userInfo, errorMessage);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 401);
            response.put("msg", errorMessage);
            response.put("data", null);
            
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * 检查学号是否可用
     * 传入学号
     * 传出 是否存在
     */
    @GetMapping("/check/userno")
    public ResponseEntity<Map<String, Object>> checkUserNo(
            @RequestParam() String userNo) {
        boolean exists = loginUserService.isUserNoExists(userNo);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "检查成功");
        response.put("data", Map.of("exists", exists));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查用户名是否可用
     * 传入用户名
     * 传出 是否存在
     */
    @GetMapping("/check/username")
    public ResponseEntity<Map<String, Object>> checkUsername(
            @RequestParam() String username) {
        boolean exists = loginUserService.isUsernameExists(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "检查成功");
        response.put("data", Map.of("exists", exists));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查邮箱是否可用
     * 传入邮箱地址
     * 传出 是否存在
     */
    @GetMapping("/check/email")
    public ResponseEntity<Map<String, Object>> checkEmail(
            @RequestParam() String email) {
        boolean exists = loginUserService.isEmailExists(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "检查成功");
        response.put("data", Map.of("exists", exists));
        
        return ResponseEntity.ok(response);
    }
}