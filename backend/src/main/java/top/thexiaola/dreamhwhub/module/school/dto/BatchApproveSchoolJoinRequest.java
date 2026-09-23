package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量审核加入学校申请请求（学校管理员操作）
 */
@Data
public class BatchApproveSchoolJoinRequest {

    /**
     * 申请 ID 列表
     */
    @NotEmpty(message = "请至少选择一条申请")
    @Size(max = 200, message = "单次最多审核 200 条申请")
    private List<Integer> applicationIds;

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
