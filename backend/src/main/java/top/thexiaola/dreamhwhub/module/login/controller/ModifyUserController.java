package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.config.GlobalExceptionHandler;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.*;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.mapper.UserMapper;
import top.thexiaola.dreamhwhub.support.session.UserUtils;


@Slf4j
@RestController
@RequestMapping("/api/users/modify")
@RequiredArgsConstructor
public class ModifyUserController {
    private final ModifyUserService modifyUserService;
    private final UserMapper userResponseMapper;
    private final EmailService emailService;

    @PutMapping("/info")
    public ResponseEntity<ApiResponse<UserResponse>> modifyUserInfo(@Valid @RequestBody ModifyUserInfoRequest modifyUserInfoRequest) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.modifyUserInfo(modifyUserInfoRequest);
            UserResponse userResponse = userResponseMapper.toUserResponse(user);
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) modify user info successful", userInfo);
            return ResponseEntity.ok(ApiResponse.success(userResponse, "信息修改成功"));
        } catch (BusinessException e) {
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 上传并更新当前用户头像
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> modifyUserAvatar(
            @RequestParam("file") MultipartFile file) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.modifyUserAvatar(file);
            UserResponse userResponse = userResponseMapper.toUserResponse(user);
            log.info("User ({}) updated avatar successfully", LogUtil.getUserInfoString(ip, user));
            return ResponseEntity.ok(ApiResponse.success(userResponse, "头像已更新"));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to update avatar: {}", userInfo, e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 清除当前用户头像
     */
    @DeleteMapping("/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> removeUserAvatar() {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.removeUserAvatar();
            UserResponse userResponse = userResponseMapper.toUserResponse(user);
            log.info("User ({}) removed avatar successfully", LogUtil.getUserInfoString(ip, user));
            return ResponseEntity.ok(ApiResponse.success(userResponse, "头像已清除"));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to remove avatar: {}", userInfo, e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    @PutMapping("/email")
    public ResponseEntity<ApiResponse<UserResponse>> modifyUserEmail(@Valid @RequestBody ModifyEmailRequest modifyEmailRequest) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.modifyUserEmail(modifyEmailRequest);
            UserResponse userResponse = userResponseMapper.toUserResponse(user);
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) modify email successful", userInfo);
            return ResponseEntity.ok(ApiResponse.success(userResponse, "邮箱修改成功"));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to modify email: {}", userInfo, e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 为换绑前的邮箱发送换绑验证码
     */
    @PostMapping("/getmodifycode/before")
    public ResponseEntity<ApiResponse<Object>> sendModifyEmailCodeBefore() {
        String ip = LogUtil.getCurrentClientIp();
        try {
            modifyUserService.sendModifyCodeToOldEmail();
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            log.info("User ({}) sent modify verification code successfully", userInfo);
            return ResponseEntity.ok(ApiResponse.success(emailService.getCooldownSeconds(), "验证码已发送"));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to send modify verification code: {}", userInfo, e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 为换绑的目标邮箱发送换绑验证码
     */
    @PostMapping("/getmodifycode/after")
    public ResponseEntity<ApiResponse<Object>> sendModifyEmailCodeAfter(@Valid @RequestBody SendModifyCodeRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            modifyUserService.sendModifyCodeToNewEmail(request.getNewEmail());
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            log.info("User ({}) sent modify verification code to new email: {} successfully", userInfo, request.getNewEmail());
            return ResponseEntity.ok(ApiResponse.success(emailService.getCooldownSeconds(), "验证码已发送"));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to send modify verification code to new email: {}", userInfo, e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
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
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 查询当前用户的危险操作安全验证设置
     */
    @GetMapping("/security-verification")
    public ResponseEntity<ApiResponse<SecurityVerificationSettings>> getSecurityVerification() {
        try {
            return ResponseEntity.ok(ApiResponse.success(modifyUserService.getSecurityVerificationSettings()));
        } catch (BusinessException e) {
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 更新当前用户的危险操作安全验证设置
     * <p>
     * 变更本身是危险操作：**每个被改动的开关都必须用该方式自身的凭据验证身份**——
     * 改动了密码验证 → 须携带登录密码；改动了邮箱验证码验证 → 须携带邮箱验证码；
     * 两者都改动则两者都须提供。凭据随请求体提交，由服务层校验。
     */
    @PutMapping("/security-verification")
    public ResponseEntity<ApiResponse<SecurityVerificationSettings>> updateSecurityVerification(
            @Valid @RequestBody UpdateSecurityVerificationRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            SecurityVerificationSettings result = modifyUserService.updateSecurityVerificationSettings(request);
            log.info("User ({}) updated security verification settings", userInfo);
            return ResponseEntity.ok(ApiResponse.success(result, "安全验证设置已更新"));
        } catch (BusinessException e) {
            String userInfo = LogUtil.getUserInfoString(ip, UserUtils.getCurrentUser());
            log.warn("User ({}) failed to update security verification settings: {}", userInfo, e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }
}