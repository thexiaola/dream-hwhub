package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核申请请求（通用）
 */
@Data
public class ApproveJoinClassRequest {
    @NotNull(message = "申请 ID 不能为空")
    @Pattern(regexp = "^[0-9]+$", message = "申请 ID 必须是数字")
    private String applicationId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;  // true=通过，false=拒绝

    @Size(max = 256, message = "审核意见长度不能超过 256 位")
    @Pattern(regexp = "^[^\\t\\f\\v]*$", message = "审核意见不能包含特殊字符（制表符等）")
    private String comment;  // 审核意见
}
