package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyEmailRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.UserUtils;

import java.util.Map;


@RestController
@RequestMapping("/api/users/modify")
public class ModifyUserController {
    private static final Logger log = LoggerFactory.getLogger(ModifyUserController.class);
    private final ModifyUserService modifyUserService;

    public ModifyUserController(ModifyUserService modifyUserService) {
        this.modifyUserService = modifyUserService;
    }

    @PutMapping("/info")
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

    @PutMapping("/email")
    public ResponseEntity<ApiResponse<UserResponse>> modifyUserEmail(@Valid @RequestBody ModifyEmailRequest modifyEmailRequest) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.modifyUserEmail(modifyEmailRequest);
            UserResponse userResponse = UserResponse.fromEntity(user);
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) modify email successful", userInfo);
            return ResponseEntity.ok(ApiResponse.success(userResponse));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to modify email: {}", userInfo, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 为换绑前的邮箱发送换绑验证码
     */
    @PostMapping("/getmodifycode/before")
    public ResponseEntity<ApiResponse<Void>> sendModifyEmailCodeBefore() {
        String ip = LogUtil.getCurrentClientIp();
        try {
            modifyUserService.sendModifyCodeToOldEmail();
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            log.info("User ({}) sent modify verification code successfully", userInfo);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to send modify verification code: {}", userInfo, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 为换绑的目标邮箱发送换绑验证码
     */
    @PostMapping("/getmodifycode/after")
    public ResponseEntity<ApiResponse<Void>> sendModifyEmailCodeAfter(@RequestBody Map<String, String> requestBody) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            String newEmail = requestBody.get("newEmail");
            modifyUserService.sendModifyCodeToNewEmail(newEmail);
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            log.info("User ({}) sent modify verification code to new email: {} successfully", userInfo, newEmail);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to send modify verification code to new email: {}", userInfo, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/password")
    public String modifyUserPassword() {
        // TODO: 需要允许用户修改自己的密码
        return null;
    }
}