package top.thexiaola.dreamhwhub.module.work_management.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级加入申请实体类
 */
@Data
@TableName("class_join_application")
public class ClassJoinApplication {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_id")
    private Integer classId;  // 申请加入的班级ID

    @TableField("applicant_id")
    private Integer applicantId;  // 申请人ID

    @TableField("status")
    private Integer status;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("reviewer_id")
    private Integer reviewerId;  // 审核人ID（老师或管理员）

    @TableField("review_time")
    private LocalDateTime reviewTime;  // 审核时间

    @TableField("review_comment")
    private String reviewComment;  // 审核意见

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 申请时间
}
