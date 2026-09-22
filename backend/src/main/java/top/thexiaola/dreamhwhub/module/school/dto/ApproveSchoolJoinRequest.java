package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核加入学校申请请求（学校管理员操作）
 */
@Data
public class ApproveSchoolJoinRequest {

    /**
     * 申请 ID
     */
    @NotNull(message = "申请 ID 不能为空")
    private Integer applicationId;

    /**
     * 审核结果：true-通过，false-拒绝
     */
    @NotNull(message = "请选择审核结果")
    private Boolean approved;

    /**
     * 审核意见
     */
    @Size(max = 500, message = "审核意见长度不能超过 500 位")
    private String comment;
}
