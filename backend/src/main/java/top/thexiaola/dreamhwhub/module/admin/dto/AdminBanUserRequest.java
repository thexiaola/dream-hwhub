package top.thexiaola.dreamhwhub.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员封禁/解封用户请求
 */
@Data
public class AdminBanUserRequest {

    // true-封禁，false-解封
    @NotNull(message = "封禁状态不能为空")
    private Boolean banned;

    // 封禁原因（封禁时建议填写）
    @Size(max = 256, message = "封禁原因长度不能超过 256 位")
    private String reason;
}
