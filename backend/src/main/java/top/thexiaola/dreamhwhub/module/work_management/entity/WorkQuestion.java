package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 作业题目实体类
 * <p>
 * 题干与答案分表存储：{@code correctAnswer} 仅用于老师侧查询与自动评判，
 * 学生取题时不下发。
 */
@Data
@TableName("work_question")
public class WorkQuestion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("work_id")
    private Integer workId;

    /** 题号（从 1 开始，用于展示顺序） */
    @TableField("order_no")
    private Integer orderNo;

    /** 题型：single/multiple/judge/fill/subjective/extra */
    @TableField("question_type")
    private String questionType;

    /** 题干 */
    @TableField("content")
    private String content;

    /** 选项 JSON 数组（单选/多选）：[{"key":"A","text":"..."}] */
    @TableField("options")
    private String options;

    /** 参考答案 JSON：选择题存选项键、判断存布尔、填空存可接受答案数组 */
    @TableField("correct_answer")
    private String correctAnswer;

    /** 本题满分 */
    @TableField("score")
    private BigDecimal score;

    /** 答案解析（可选） */
    @TableField("analysis")
    private String analysis;

    /** 是否可自动评判（客观题为 true） */
    @TableField("auto_gradable")
    private Boolean autoGradable = false;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
