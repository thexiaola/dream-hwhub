package top.thexiaola.dreamhwhub.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员设置/取消平台管理员（OP）请求
 */
@Data
public class AdminSetOpRequest {

    // true-授予平台管理员，false-取消
    @NotNull(message = "OP 状态不能为空")
    private Boolean isOp;
}
