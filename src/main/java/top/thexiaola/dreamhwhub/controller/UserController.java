package top.thexiaola.dreamhwhub.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.ApiResponse;
import top.thexiaola.dreamhwhub.domain.User;
import top.thexiaola.dreamhwhub.dto.LoginRequest;
import top.thexiaola.dreamhwhub.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.service.EmailService;
import top.thexiaola.dreamhwhub.service.IUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final IUserService userService;
    
    private final EmailService emailService;

    public UserController(IUserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
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
            
            User loggedInUser = userService.login(user);
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
            
            User registeredUser = userService.register(user, registerRequest.getVerificationCode(), registerRequest.getInvitationCode());
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
            userService.sendVerificationCode(registerRequest.getUserNo(), registerRequest.getEmail());
            LogUtil.infoAnonymous(logger, String.format("验证码发送请求处理完成，邮箱: %s", registerRequest.getEmail()), request);
            return ApiResponse.success(null, "验证码已发送");
        } catch (Exception e) {
            LogUtil.errorAnonymous(logger, String.format("发送验证码失败: %s", e.getMessage()), request);
            return ApiResponse.error(400, e.getMessage());
        }
    }
}