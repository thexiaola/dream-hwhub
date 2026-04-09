package top.thexiaola.dreamhwhub.module.work_management.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师邀请用户实体类
 */
@Data
@TableName("class_invitation")
public class ClassInvitation {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_id")
    private Integer classId;  // 班级 ID

    @TableField("inviter_id")
    private Integer inviterId;  // 邀请人 ID（教师）

    @TableField("invitee_user_id")
    private Integer inviteeUserId;  // 被邀请人 ID

    @TableField("status")
    private Integer status;  // 0-待处理，1-已同意，2-已拒绝，3-已过期

    @TableField("expire_time")
    private LocalDateTime expireTime;  // 过期时间

    @TableField("response_time")
    private LocalDateTime responseTime;  // 响应时间

    @TableField("response_comment")
    private String responseComment;  // 用户回复说明

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 邀请时间
}
