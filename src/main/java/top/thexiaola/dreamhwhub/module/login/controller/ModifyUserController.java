package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.UserUtils;


@RestController
@RequestMapping("/api/users")
public class ModifyUserController {
    private static final Logger log = LoggerFactory.getLogger(ModifyUserController.class);
    private final ModifyUserService modifyUserService;
    private final EmailService emailService;
    
    public ModifyUserController(ModifyUserService modifyUserService, EmailService emailService) {
        this.modifyUserService = modifyUserService;
        this.emailService = emailService;
    }

    @PostMapping("/modify/info")
    public ResponseEntity<ApiResponse<UserResponse>> modifyUserInfo(@Valid @RequestBody ModifyUserInfoRequest modifyUserInfoRequest) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.modifyUserInfo(modifyUserInfoRequest);
            UserResponse userResponse = UserResponse.fromEntity(user);
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) modify user info successful", userInfo);
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PostMapping("/modify/email")
    public String modifyUserEmail() {
        // TODO: 需要允许用户修改自己的 email
        return null;
    }

    /**
     * 发送换绑验证码
     */
    @GetMapping("/modify/getemailcode")
    public ResponseEntity<ApiResponse<Void>> sendModifyEmailCode() {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser == null) {
                log.warn("User (ip: {}) attempted to send modify code but not logged in", ip);
                return ResponseEntity.badRequest().body(ApiResponse.error(401, "用户未登录"));
            }

            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            emailService.sendModifyEmailCode(currentUser.getEmail(), currentUser.getUserNo(), currentUser.getUsername());
            log.info("User ({}) sent modify verification code successfully", userInfo);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to send modify verification code: {}", userInfo, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PostMapping("/modify/password")
    public String modifyUserPassword() {
        // TODO: 需要允许用户修改自己的密码
        return null;
    }
}