package top.thexiaola.dreamhwhub.module.work_management.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级邀请申请实体类
 */
@Data
@TableName("class_invite_application")
public class ClassInviteApplication {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_id")
    private Integer classId;  // 班级 ID

    @TableField("inviter_id")
    private Integer inviterId;  // 邀请人 ID（学生）

    @TableField("invitee_account")
    private String inviteeAccount;  // 被邀请人账号

    @TableField("is_teacher")
    private Boolean isTeacher;  // true=老师，false=学生

    @TableField("status")
    private Integer status;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("reviewer_id")
    private Integer reviewerId;  // 审核人 ID

    @TableField("review_time")
    private LocalDateTime reviewTime;  // 审核时间

    @TableField("review_comment")
    private String reviewComment;  // 审核意见

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 邀请时间
}
