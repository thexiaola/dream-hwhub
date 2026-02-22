package top.thexiaola.dreamhwhub.module.login.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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



    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        String ip = LogUtil.getCurrentClientIp();
        String userInfo = LogUtil.getUserInfoString(ip, null);
        
        ServiceResult<User> result = loginUserService.login(loginRequest);
        
        if (result.isSuccess()) {
            User user = result.getData();
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