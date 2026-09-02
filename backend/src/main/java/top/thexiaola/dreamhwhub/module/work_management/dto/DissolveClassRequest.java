package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解散班级请求（账号密码二次校验）
 * 说明：改用请求体而非 URL 参数传递，避免密码明文出现在 URL/访问日志中
 */
@Data
public class DissolveClassRequest {

    /**
     * 当前用户密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 确认文案
     */
    @NotBlank(message = "确认文案不能为空")
    private String confirmText;
}
