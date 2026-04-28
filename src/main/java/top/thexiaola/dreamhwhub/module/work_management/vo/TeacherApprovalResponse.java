package top.thexiaola.dreamhwhub.module.work_management.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教师审核响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherApprovalResponse {

    /**
     * 审核 ID
     */
    private Integer id;

    /**
     * 班级 ID
     */
    private Integer classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 关联的用户邀请 ID
     */
    private Integer invitationId;

    /**
     * 被邀请人 ID
     */
    private Integer inviteeId;

    /**
     * 被邀请人用户名
     */
    private String inviteeUsername;

    /**
     * 教师审核状态：0-待审核，1-已通过，2-已拒绝
     */
    private Integer status;

    /**
     * 审核人 ID（老师/助理）
     */
    private Integer reviewerId;

    /**
     * 审核人用户名
     */
    private String reviewerUsername;

    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;

    /**
     * 审核意见
     */
    private String reviewComment;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
