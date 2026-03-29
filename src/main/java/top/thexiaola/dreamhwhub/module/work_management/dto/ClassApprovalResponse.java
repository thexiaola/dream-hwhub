package top.thexiaola.dreamhwhub.module.work_management.dto;

import lombok.Data;

/**
 * 班级审核响应
 */
@Data
public class ClassApprovalResponse {
    private Integer id;
    private String className;
    private String description;
    private Integer creatorId;
    private String creatorName;
    private String creatorNo;
    private Integer approvalStatus;  // 0-待审核，1-已通过，2-已拒绝
    private String adminRemark;
    private String createTime;
}
