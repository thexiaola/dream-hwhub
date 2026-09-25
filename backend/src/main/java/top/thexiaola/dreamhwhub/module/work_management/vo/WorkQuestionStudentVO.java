package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 作业题目响应 VO（学生侧：不含参考答案与解析）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkQuestionStudentVO {

    private Integer id;
    private Integer orderNo;
    /** 题型 */
    private String questionType;
    /** 题型中文名 */
    private String questionTypeName;
    /** 题干 */
    private String content;
    /** 选项 */
    private List<OptionVO> options;
    /** 本题满分 */
    private BigDecimal score;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionVO {
        private String key;
        private String text;
    }
}
