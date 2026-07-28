package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 转让班级所有权请求
 */
@Data
public class TransferOwnershipRequest {

    /**
     * 新所有者用户 ID
     */
    @NotNull(message = "新所有者 ID 不能为空")
    private Integer newOwnerId;
}
