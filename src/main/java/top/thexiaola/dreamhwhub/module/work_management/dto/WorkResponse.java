package top.thexiaola.dreamhwhub.module.work_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作业响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkResponse {

    /**
     * 作业 ID
     */
    private Integer id;

    /**
     * 作业标题
     */
    private String title;

    /**
     * 作业描述
     */
    private String description;

    /**
     * 发布教师工号
     */
    private String teacherNo;

    /**
     * 发布教师姓名
     */
    private String teacherName;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 作业总分
     */
    private Integer totalScore;

    /**
     * 作业状态：0-未发布，1-已发布，2-已结束
     */
    private Integer status;

    /**
     * 是否已逾期
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
