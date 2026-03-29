package top.thexiaola.dreamhwhub.module.work_management.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级申请实体类
 */
@Data
@TableName("class_application")
public class ClassApplication {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("type")
    private Integer type;  // 1-创建班级申请，2-加入班级申请

    @TableField("class_id")
    private Integer classId;

    @TableField("applicant_id")
    private Integer applicantId;

    @TableField("target_role")
    private Boolean targetRole;  // true=老师，false=学生

    @TableField("class_name")
    private String className;

    @TableField("description")
    private String description;

    @TableField("status")
    private Integer status;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("reviewer_id")
    private Integer reviewerId;

    @TableField("review_time")
    private LocalDateTime reviewTime;

    @TableField("review_comment")
    private String reviewComment;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
