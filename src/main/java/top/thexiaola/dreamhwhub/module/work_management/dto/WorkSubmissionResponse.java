package top.thexiaola.dreamhwhub.module.work_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkSubmissionResponse {

    /**
     * 提交 ID
     */
    private Integer id;

    /**
     * 作业 ID
     */
    private Integer workId;

    /**
     * 作业标题
     */
    private String workTitle;

    /**
     * 提交学生学号
     */
    private String studentNo;

    /**
     * 提交学生姓名
     */
    private String studentName;

    /**
     * 提交内容/文件路径
     */
    private String submissionContent;

    /**
     * 提交分数
     */
    private BigDecimal score;

    /**
     * 教师评语
     */
    private String comment;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;

    /**
     * 批改时间
     */
    private LocalDateTime gradeTime;

    /**
     * 批改教师工号
     */
    private String gradeTeacherNo;

    /**
     * 提交状态：0-未提交，1-已提交，2-已批改
     */
    private Integer status;

    /**
     * 是否逾期：0-否，1-是
     */
    private Boolean isOverdue;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
