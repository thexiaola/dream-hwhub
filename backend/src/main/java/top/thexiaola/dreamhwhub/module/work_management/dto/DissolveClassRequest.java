package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解散班级请求
 * <p>
 * 身份二次验证（登录密码或邮箱验证码）由 {@code RequireSensitiveVerification} 注解与
 * 拦截器统一处理，从请求头接收，故本请求体仅携带确认文案。
 */
@Data
public class DissolveClassRequest {

    /**
     * 确认文案
     */
    @NotBlank(message = "确认文案不能为空")
    private String confirmText;
}
