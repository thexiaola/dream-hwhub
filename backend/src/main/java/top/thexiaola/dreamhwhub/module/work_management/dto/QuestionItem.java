package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 作业题目项（随创建/更新作业提交，或单独维护题库）
 * <p>
 * 题型见 {@code QuestionType}：single/multiple/judge/fill/subjective/extra。
 */
@Data
public class QuestionItem {

    /**
     * 题目 ID（更新已有题目时携带，新增时为空）
     */
    private Integer id;

    /**
     * 题型
     */
    @NotBlank(message = "题型不能为空")
    private String questionType;

    /**
     * 题干
     */
    @NotBlank(message = "题干不能为空")
    @Size(max = 4096, message = "题干长度不能超过 4096 位")
    @Pattern(regexp = "^[^\\t\\f\\v]+$", message = "题干不能包含制表符等特殊字符")
    private String content;

    /**
     * 选项（单选/多选必填）：[{ "key": "A", "text": "..." }]
     */
    private List<QuestionOption> options;

    /**
     * 参考答案（客观题必填）：
     * single/judge-字符串，multiple-字符串数组，fill-可接受答案数组
     */
    private Object correctAnswer;

    /**
     * 本题满分
     */
    private BigDecimal score;

    /**
     * 答案解析（可选）
     */
    private String analysis;

    /**
     * 单个选项
     */
    @Data
    public static class QuestionOption {
        /** 选项键，如 A/B/C/D */
        private String key;
        /** 选项文本 */
        private String text;
    }
}
