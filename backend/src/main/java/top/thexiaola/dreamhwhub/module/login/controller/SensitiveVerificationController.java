package top.thexiaola.dreamhwhub.module.login.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.config.GlobalExceptionHandler;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.security.SensitiveOperationVerifier;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

/**
 * 敏感操作身份二次验证控制器
 * <p>
 * 解散班级、退出学校、踢出成员，以及平台管理员的高危/权限类操作前，前端向当前用户
 * 的绑定邮箱发送身份验证码；随后在操作请求头中携带该验证码（或直接使用登录密码）即可。
 */
@Slf4j
@RestController
@RequestMapping("/api/users/sensitive-verification")
@RequiredArgsConstructor
public class SensitiveVerificationController {

    private final SensitiveOperationVerifier sensitiveOperationVerifier;

    /**
     * 向当前用户的绑定邮箱发送敏感操作身份验证码
     *
     * @return 验证码发送冷却时间（秒），供前端展示倒计时
     */
    @PostMapping("/code")
    public ResponseEntity<ApiResponse<Integer>> sendCode() {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            int cooldown = sensitiveOperationVerifier.sendCode(currentUser);
            log.info("User ({}) requested sensitive-operation verification code",
                    LogUtil.getUserInfoString(ip, currentUser));
            return ResponseEntity.ok(ApiResponse.success(cooldown, "验证码已发送至绑定邮箱"));
        } catch (BusinessException e) {
            log.warn("Failed to send sensitive-operation verification code: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }
}
