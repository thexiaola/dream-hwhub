package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置班级接管是否需要审核请求（学校管理员或平台管理员操作）
 */
@Data
public class SetClassTakeoverApprovalRequest {

    /**
     * 班级接管是否自动同意：true-其他老师申请接管失活班级时自动通过，false-需学校管理员审核
     */
    @NotNull(message = "请设置班级接管是否自动同意")
    private Boolean autoApproveClassTakeover;
}
