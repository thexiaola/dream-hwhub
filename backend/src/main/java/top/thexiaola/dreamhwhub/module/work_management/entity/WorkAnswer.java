package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业作答实体类
 * <p>
 * 记录学生在某次提交中每道题的作答与得分：
 * 客观题默认自动评判记分，老师可手动改判覆盖；主观题/附加题由老师手动评分。
 */
@Data
@TableName("work_answer")
public class WorkAnswer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("submission_id")
    private Integer submissionId;

    @TableField("question_id")
    private Integer questionId;

    /** 学生作答 JSON */
    @TableField("answer")
    private String answer;

    /** 本题得分（自动或手动） */
    @TableField("score")
    private BigDecimal score;

    /** 客观题是否正确：true-对，false-错，null-未判定/主观题 */
    @TableField("is_correct")
    private Boolean isCorrect;

    /** 评分方式：auto-自动，manual-手动 */
    @TableField("grading_type")
    private String gradingType;

    /** 老师对本题的评语（可选） */
    @TableField("comment")
    private String comment;

    /** 本题评分人 ID（手动评分时） */
    @TableField("grader_id")
    private Integer graderId;

    @TableField("grade_time")
    private LocalDateTime gradeTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
