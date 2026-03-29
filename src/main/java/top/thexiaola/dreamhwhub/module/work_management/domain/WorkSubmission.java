package top.thexiaola.dreamhwhub.module.work_management.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交实体类
 */
@Data
@TableName("work_submission")
public class WorkSubmission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 提交 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 作业 ID
     */
    @TableField("work_id")
    private Integer workId;

    /**
     * 提交学生学号
     */
    @TableField("student_no")
    private String studentNo;

    /**
     * 提交学生姓名
     */
    @TableField("student_name")
    private String studentName;

    /**
     * 提交内容/文件路径
     */
    @TableField("submission_content")
    private String submissionContent;

    /**
     * 提交分数
     */
    @TableField("score")
    private BigDecimal score;

    /**
     * 教师评语
     */
    @TableField("comment")
    private String comment;

    /**
     * 提交时间
     */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /**
     * 批改时间
     */
    @TableField("grade_time")
    private LocalDateTime gradeTime;

    /**
     * 批改教师工号
     */
    @TableField("grade_teacher_no")
    private String gradeTeacherNo;

    /**
     * 提交状态：0-未提交，1-已提交，2-已批改
     */
    @TableField("status")
    private Integer status = 1;

    /**
     * 是否逾期：0-否，1-是
     */
    @TableField("is_overdue")
    private Boolean isOverdue = false;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
