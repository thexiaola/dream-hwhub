package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核班级请求
 */
@Data
public class ApproveClassRequest {
    @NotNull(message = "班级 ID 不能为空")
    private Integer classId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;  // true=通过，false=拒绝

    private String remark;  // 审核备注
}
