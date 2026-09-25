package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单题作答响应 VO（用于提交详情/教师评阅/学生查看成绩）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkAnswerVO {

    private Integer id;
    private Integer questionId;
    /** 题号 */
    private Integer orderNo;
    private String questionType;
    private String questionTypeName;
    /** 题干 */
    private String content;
    /** 学生作答（原始形态） */
    private Object answer;
    /** 本题满分 */
    private BigDecimal fullScore;
    /** 本题得分 */
    private BigDecimal score;
    /** 客观题是否正确 */
    private Boolean isCorrect;
    /** 评分方式：auto/manual */
    private String gradingType;
    /** 本题评语 */
    private String comment;
    /** 参考答案（仅教师评阅/已批改后学生可见） */
    private Object correctAnswer;
    /** 答案解析 */
    private String analysis;
    /** 是否可自动评判 */
    private Boolean autoGradable;
    private LocalDateTime gradeTime;
}
