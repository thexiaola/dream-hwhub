package top.thexiaola.dreamhwhub.module.work_management.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级成员实体类
 */
@Data
@TableName("class_member")
public class ClassMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_id")
    private Integer classId;

    @TableField("user_id")
    private Integer userId;

    @TableField("role")
    private Boolean isTeacher;  // true=老师，false=学生

    @TableField("approval_status")
    private Integer approvalStatus;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("reviewer_id")
    private Integer reviewerId;  // 审核人 ID

    @TableField("review_time")
    private LocalDateTime reviewTime;  // 审核时间

    @TableField("review_comment")
    private String reviewComment;  // 审核意见

    @TableField("join_time")
    private LocalDateTime joinTime;

    @TableField("invite_by")
    private Integer inviteBy;
}
