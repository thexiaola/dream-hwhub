package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考试会话实体
 * <p>
 * 记录学生一次考试的开考时刻、限时、交卷状态与违规累计，
 * 同时保存本次下发的字体映射种子与题目乱序顺序，用于限时判定与反作弊审计。
 */
@Data
@TableName("exam_session")
public class ExamSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 考试（work_info）ID */
    @TableField("work_id")
    private Integer workId;

    /** 学生 ID */
    @TableField("student_id")
    private Integer studentId;

    /** 开考时刻（限时起点） */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 交卷时刻（为空表示进行中） */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /** 状态：1-进行中，2-已交卷，3-超时自动交卷 */
    @TableField("status")
    private Integer status = 1;

    /** 累计违规次数 */
    @TableField("violation_count")
    private Integer violationCount = 0;

    /** 本次下发的字体映射种子（反作弊-字体映射） */
    @TableField("font_seed")
    private Integer fontSeed;

    /** 题目乱序后的题目ID顺序 JSON 数组（为空表示按题号） */
    @TableField("question_order")
    private String questionOrder;

    /** 答题草稿 JSON（自动保存，用于刷新/断线后恢复作答） */
    @TableField("draft_answers")
    private String draftAnswers;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
