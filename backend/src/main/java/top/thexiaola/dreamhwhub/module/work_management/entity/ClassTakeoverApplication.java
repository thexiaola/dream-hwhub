package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级接管申请实体类
 * <p>
 * 当班级创建者的教师身份被解除（在该班级所属学校已不再是老师）时，
 * 该班级进入「失活」状态：原创建者无法再管理，且不再接纳新学生；
 * 本校其他老师可发起接管申请。学校可配置为自动同意（默认），或由学校管理员审核。
 * 接管通过后将班级所有权转移给接管人。
 */
@Data
@TableName("class_takeover_application")
public class ClassTakeoverApplication {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_id")
    private Integer classId;  // 班级 ID

    @TableField("applicant_id")
    private Integer applicantId;  // 申请接管的用户 ID

    @TableField("status")
    private Integer status;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("reviewer_id")
    private Integer reviewerId;  // 审核人 ID（学校管理员）

    @TableField("review_time")
    private LocalDateTime reviewTime;  // 审核时间

    @TableField("review_comment")
    private String reviewComment;  // 审核意见

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 申请时间
}
