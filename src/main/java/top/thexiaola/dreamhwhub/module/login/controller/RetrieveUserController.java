package top.thexiaola.dreamhwhub.module.login.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.RetrievePasswordCodeRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RetrievePasswordModifyRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;

@RestController
@RequestMapping("/api/users/retrieve")
public class RetrieveUserController {
    private static final Logger log = LoggerFactory.getLogger(RetrieveUserController.class);
    private final ModifyUserService modifyUserService;

    public RetrieveUserController(ModifyUserService modifyUserService) {
        this.modifyUserService = modifyUserService;
    }

    /**
     * 发送找回密码验证码
     */
    @PostMapping("/sendcode")
    public ResponseEntity<ApiResponse<Void>> sendRetrievePasswordCode(@Valid @RequestBody RetrievePasswordCodeRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.sendRetrievePasswordCode(request.getAccount());
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) send retrieve password verification code successful", userInfo);
            return ResponseEntity.ok(ApiResponse.success(null, "验证码已发送"));
        } catch (BusinessException e) {
            String userInfo = String.format("ip: %s, account: %s", ip, request.getAccount());
            log.warn("User ({}) failed to send retrieve password verification code: {}", userInfo, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 找回密码（通过验证码修改密码）
     */
    @PutMapping("/resetpassword")
    public ResponseEntity<ApiResponse<Void>> retrievePassword(@Valid @RequestBody RetrievePasswordModifyRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = modifyUserService.retrievePassword(request);
            String userInfo = LogUtil.getUserInfoString(ip, user);
            log.info("User ({}) password retrieved successfully", userInfo);
            return ResponseEntity.ok(ApiResponse.success(null, "密码重置成功"));
        } catch (BusinessException e) {
            String userInfo = String.format("ip: %s, account: %s", ip, request.getAccount());
            log.warn("User ({}) failed to retrieve password: {}", userInfo, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}