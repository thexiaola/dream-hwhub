package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 作业题目响应 VO（教师侧：含参考答案与解析）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkQuestionVO {

    private Integer id;
    private Integer workId;
    /** 题号，从 1 开始 */
    private Integer orderNo;
    /** 题型 */
    private String questionType;
    /** 题型中文名 */
    private String questionTypeName;
    /** 题干 */
    private String content;
    /** 选项 */
    private List<OptionVO> options;
    /** 参考答案（原始形态：字符串或数组） */
    private Object correctAnswer;
    /** 本题满分 */
    private BigDecimal score;
    /** 答案解析 */
    private String analysis;
    /** 是否可自动评判 */
    private Boolean autoGradable;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionVO {
        private String key;
        private String text;
    }
}
