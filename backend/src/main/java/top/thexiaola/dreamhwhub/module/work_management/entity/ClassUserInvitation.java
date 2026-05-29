package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户邀请申请实体类（学生发起邀请，等待被邀请用户确认）
 */
@Data
@TableName("class_user_invitation")
public class ClassUserInvitation {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_id")
    private Integer classId;  // 班级 ID

    @TableField("inviter_id")
    private Integer inviterId;  // 邀请人 ID（学生）

    @TableField("invitee_id")
    private Integer inviteeId;  // 被邀请人 ID

    @TableField("status")
    private Integer status;  // 0-待确认，1-已同意，2-已拒绝

    @TableField("response_time")
    private LocalDateTime responseTime;  // 用户响应时间

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 邀请时间
}
