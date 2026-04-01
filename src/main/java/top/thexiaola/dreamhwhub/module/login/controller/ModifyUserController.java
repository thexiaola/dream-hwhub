package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.*;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.UserUtils;


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
            return ResponseEntity.ok(ApiResponse.success(userResponse, "信息修改成功"));
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
            return ResponseEntity.ok(ApiResponse.success(userResponse, "邮箱修改成功"));
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
            return ResponseEntity.ok(ApiResponse.success(null, "验证码已发送"));
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
    public ResponseEntity<ApiResponse<Void>> sendModifyEmailCodeAfter(@Valid @RequestBody SendModifyCodeRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            modifyUserService.sendModifyCodeToNewEmail(request.getNewEmail());
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            log.info("User ({}) sent modify verification code to new email: {} successfully", userInfo, request.getNewEmail());
            return ResponseEntity.ok(ApiResponse.success(null, "验证码已发送"));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to send modify verification code to new email: {}", userInfo, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 修改用户密码
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> modifyUserPassword(@Valid @RequestBody ModifyPasswordRequest modifyPasswordRequest) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            modifyUserService.modifyUserPassword(modifyPasswordRequest);
            log.info("User ({}) password modified successfully", userInfo);
            return ResponseEntity.ok(ApiResponse.success(null, "密码修改成功"));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to modify password: {}", userInfo, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}