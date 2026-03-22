package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.EmailCodeRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.service.RegisterUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.SessionManager;



/**
 * 用户注册控制器
 */
@RestController
@RequestMapping("/api/users")
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);
    
    private final RegisterUserService registerUserService;
    
    @Value("${app.verification-code.cooldown-seconds}")
    private int cooldown;

    public RegisterController(RegisterUserService registerUserService) {
        this.registerUserService = registerUserService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(HttpServletRequest request, @Valid @RequestBody RegisterRequest registerRequest) {
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
            
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } else {
            String errorMessage = result.getMessage();
            return ResponseEntity.badRequest().body(ApiResponse.error(400, errorMessage));
        }
    }

    /**
     * 发送注册验证码
     */
    @PostMapping("/getregcode")
    public ResponseEntity<ApiResponse<Void>> sendRegisterCode(@Valid @RequestBody EmailCodeRequest emailCodeRequest) {
        ServiceResult<Void> result = registerUserService.sendEmailCode(emailCodeRequest.getEmail(), emailCodeRequest.getUserNo(), emailCodeRequest.getUsername());

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(null));
        } else {
            String errorMessage = result.getMessage();
            // 从额外数据中获取剩余秒数
            int remainingSeconds = 0;
            if (result.getExtraData() instanceof Number) {
                remainingSeconds = ((Number) result.getExtraData()).intValue();
            }
            return ResponseEntity.badRequest().body(ApiResponse.error(400, errorMessage));
        }
    }
}