package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置加入学校是否需要审核请求（平台管理员或学校管理员操作）
 */
@Data
public class SetSchoolJoinApprovalRequest {

    /**
     * 是否免审核：true-填入学工号与姓名后直接加入，false-需学校管理员审核
     */
    @NotNull(message = "请设置加入学校是否需要审核")
    private Boolean allowJoinWithoutApproval;
}
