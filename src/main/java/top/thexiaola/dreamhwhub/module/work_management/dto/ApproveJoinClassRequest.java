package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核加入班级请求
 */
@Data
public class ApproveJoinClassRequest {
    @NotNull(message = "成员 ID 不能为空")
    private Integer memberId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approved;  // true=通过，false=拒绝

    private String comment;  // 审核意见
}
