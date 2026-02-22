package top.thexiaola.dreamhwhub.module.login.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.*;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
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

    public LoginUserController(LoginUserService loginUserService) {
        this.loginUserService = loginUserService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        String ip = LogUtil.getCurrentClientIp();
        String userInfo = LogUtil.getUserInfoString(ip, null);
        
        try {
            User user = loginUserService.register(registerRequest);
            UserResponse userResponse = UserResponse.fromEntity(user);
            
            // 更新userInfo包含注册成功的用户信息
            userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) registration successful", userInfo);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 200);
            response.put("msg", "注册成功！");
            response.put("data", userResponse);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.info("User ({}) registration failed: {}", userInfo, e.getMessage());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 400);
            response.put("msg", e.getMessage());
            response.put("data", null);
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("User ({}) registration failed with system error: {}", userInfo, e.getMessage(), e);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 500);
            response.put("msg", "注册失败！");
            response.put("data", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String ip = LogUtil.getCurrentClientIp();
        String userInfo = LogUtil.getUserInfoString(ip, null);
        
        try {
            User user = loginUserService.login(loginRequest);
            UserResponse userResponse = UserResponse.fromEntity(user);
            
            // 更新userInfo包含登录成功的用户信息
            userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) login successful", userInfo);
            
            // 这里可以生成JWT token，简化处理直接返回用户信息
            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("user", userResponse);
            // responseData.put("token", generateToken(user)); // 可以添加token生成
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 200);
            response.put("msg", "登录成功！");
            response.put("data", responseData);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.info("User ({}) login failed: {}", userInfo, e.getMessage());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 401);
            response.put("msg", "账号或密码错误！");
            response.put("data", null);
            
            return ResponseEntity.status(401).body(response);
        } catch (Exception e) {
            log.error("User ({}) login failed with system error: {}", userInfo, e.getMessage(), e);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 500);
            response.put("msg", "登录失败！");
            response.put("data", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 发送注册验证码
     */
    @PostMapping("/getregcode")
    public ResponseEntity<Map<String, Object>> sendRegisterCode(@Valid @RequestBody EmailCodeRequest emailCodeRequest) {
        String ip = LogUtil.getCurrentClientIp();
        String userInfo = LogUtil.getUserInfoString(ip, null);
        String email = emailCodeRequest.getEmail();
        
        try {
            loginUserService.sendEmailCode(email);
            
            log.debug("User ({}) verification code sent to email: {}", userInfo, email);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 200);
            response.put("msg", "验证码发送成功！");
            response.put("data", null);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.info("User ({}) failed to send verification code to {}: {}", userInfo, email, e.getMessage());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 400);
            response.put("msg", e.getMessage());
            response.put("data", null);
            
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("User ({}) failed to send verification code to {} with system error: {}", userInfo, email, e.getMessage(), e);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("code", 500);
            response.put("msg", "验证码发送失败！");
            response.put("data", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查学号是否可用
     */
    @GetMapping("/check/userno/{userNo}")
    public ResponseEntity<Map<String, Object>> checkUserNo(@PathVariable String userNo) {
        boolean exists = loginUserService.isUserNoExists(userNo);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "检查成功");
        response.put("data", Map.of("exists", exists));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查用户名是否可用
     */
    @GetMapping("/check/username/{username}")
    public ResponseEntity<Map<String, Object>> checkUsername(@PathVariable String username) {
        boolean exists = loginUserService.isUsernameExists(username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "检查成功");
        response.put("data", Map.of("exists", exists));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 检查邮箱是否可用
     */
    @GetMapping("/check/email/{email}")
    public ResponseEntity<Map<String, Object>> checkEmail(@PathVariable String email) {
        boolean exists = loginUserService.isEmailExists(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "检查成功");
        response.put("data", Map.of("exists", exists));
        
        return ResponseEntity.ok(response);
    }
}