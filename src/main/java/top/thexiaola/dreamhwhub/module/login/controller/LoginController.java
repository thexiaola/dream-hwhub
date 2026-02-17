package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.shared.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;

@RestController
@RequestMapping("/api/users")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    
    private final LoginUserService loginUserService;

    public LoginController(LoginUserService loginUserService) {
        this.loginUserService = loginUserService;
    }

    @PostMapping("/login")
    public ApiResponse<User> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        logger.info("收到登录请求，学号: {}, 邮箱: {}", loginRequest.getUserNo(), loginRequest.getEmail());
        
        try {
            // 将LoginRequest转换为User对象
            User user = new User();
            user.setUserNo(loginRequest.getUserNo());
            user.setEmail(loginRequest.getEmail());
            user.setPassword(loginRequest.getPassword());
            
            User loggedInUser = loginUserService.login(user);
            LogUtil.info(logger, "登录成功", loggedInUser, request);
            return ApiResponse.success(loggedInUser, "登录成功");
        } catch (Exception e) {
            LogUtil.errorAnonymous(logger, "登录失败: " + e.getMessage(), request);
            return ApiResponse.error(401, e.getMessage());
        }
    }

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        LogUtil.infoAnonymous(logger, String.format("收到注册请求，学号: %s, 邮箱: %s", registerRequest.getUserNo(), registerRequest.getEmail()), request);
        
        try {
            // 将RegisterRequest转换为User对象
            User user = new User();
            user.setUserNo(registerRequest.getUserNo());
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPassword(registerRequest.getPassword());
            
            User registeredUser = loginUserService.register(user, registerRequest.getVerificationCode(), registerRequest.getInvitationCode());
            LogUtil.info(logger, "注册成功", registeredUser, request);
            return ApiResponse.success(registeredUser, "注册成功");
        } catch (Exception e) {
            LogUtil.errorAnonymous(logger, String.format("注册失败: %s", e.getMessage()), request);
            return ApiResponse.error(400, e.getMessage());
        }
    }
    
    @PostMapping("/getregcode")
    public ApiResponse<String> sendVerificationCode(@RequestBody RegisterRequest registerRequest, HttpServletRequest request) {
        LogUtil.infoAnonymous(logger, String.format("收到发送验证码请求，学号: %s, 邮箱: %s", registerRequest.getUserNo(), registerRequest.getEmail()), request);
        
        try {
            String result = loginUserService.sendVerificationCode(registerRequest.getUserNo(), registerRequest.getEmail());
            LogUtil.infoAnonymous(logger, String.format("验证码发送请求处理完成，邮箱: %s", registerRequest.getEmail()), request);
            return ApiResponse.success(null, result);
        } catch (Exception e) {
            LogUtil.errorAnonymous(logger, String.format("发送验证码失败: %s", e.getMessage()), request);
            return ApiResponse.error(400, e.getMessage());
        }
    }
}