package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师审核邀请实体类（用户同意后，等待教师或助理审核）
 */
@Data
@TableName("class_teacher_approval")
public class ClassTeacherApproval {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_id")
    private Integer classId;  // 班级 ID

    @TableField("invitation_id")
    private Integer invitationId;  // 关联的用户邀请 ID

    @TableField("invitee_id")
    private Integer inviteeId;  // 被邀请人 ID

    @TableField("status")
    private Integer status;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("reviewer_id")
    private Integer reviewerId;  // 审核人 ID（老师/助理）

    @TableField("review_time")
    private LocalDateTime reviewTime;  // 审核时间

    @TableField("review_comment")
    private String reviewComment;  // 审核意见

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 创建时间
}
