package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 考试违规记录 VO（教师审计用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamViolationVO {

    private Integer id;
    private Integer sessionId;
    private Integer workId;
    private Integer studentId;
    /** 学生用户名 */
    private String studentName;
    /** 学生在班级内的姓名 */
    private String studentRealName;
    /** 学生在班级内的学号 */
    private String studentNo;
    /** 违规类型 */
    private String type;
    /** 违规类型中文名 */
    private String typeName;
    /** 补充说明 */
    private String detail;
    /** 发生时间 */
    private LocalDateTime occurTime;
}
