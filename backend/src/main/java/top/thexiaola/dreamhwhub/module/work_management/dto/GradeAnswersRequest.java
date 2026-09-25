package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 老师逐题评分请求（也用于主观题/附加题手动评分，或对客观题手动改判）
 */
@Data
public class GradeAnswersRequest {

    /**
     * 提交 ID
     */
    @NotNull(message = "提交 ID 不能为空")
    private Integer submissionId;

    /**
     * 每题的评分结果
     */
    @NotNull(message = "评分内容不能为空")
    private List<GradeItem> items;

    /**
     * 单题评分
     */
    @Data
    public static class GradeItem {
        /** 题目 ID */
        @NotNull(message = "题目 ID 不能为空")
        private Integer questionId;
        /** 本题得分（不得超过该题满分） */
        @NotNull(message = "分数不能为空")
        private BigDecimal score;
        /** 本题评语（可选） */
        private String comment;
    }
}
