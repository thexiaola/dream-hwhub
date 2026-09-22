package top.thexiaola.dreamhwhub.module.school.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校加入申请实体类
 * 用户填入学工号与姓名后提交，由学校管理员审核
 */
@Data
@TableName("school_join_application")
public class SchoolJoinApplication {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("school_id")
    private Integer schoolId;  // 申请加入的学校ID

    @TableField("applicant_id")
    private Integer applicantId;  // 申请人ID

    @TableField("applicant_name")
    private String applicantName;  // 申请人姓名

    @TableField("applicant_no")
    private String applicantNo;  // 申请人学工号

    @TableField("status")
    private Integer status;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("reviewer_id")
    private Integer reviewerId;  // 审核人ID（学校管理员）

    @TableField("review_time")
    private LocalDateTime reviewTime;  // 审核时间

    @TableField("review_comment")
    private String reviewComment;  // 审核意见

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 申请时间
}
