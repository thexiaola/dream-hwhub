package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级创建申请实体类
 */
@Data
@TableName("class_create_application")
public class ClassCreateApplication {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("applicant_id")
    private Integer applicantId;  // 申请人ID

    @TableField("class_name")
    private String className;  // 申请的班级名称

    @TableField("description")
    private String description;  // 申请的班级描述

    @TableField("status")
    private Integer status;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("reviewer_id")
    private Integer reviewerId;  // 审核人ID（管理员）

    @TableField("review_time")
    private LocalDateTime reviewTime;  // 审核时间

    @TableField("review_comment")
    private String reviewComment;  // 审核意见

    @TableField("created_class_id")
    private Integer createdClassId;  // 审核通过后创建的班级ID

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 申请时间
}
