package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作业提交响应VO
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
     * 提交人 ID
     */
    private Integer submitterId;

    /**
     * 提交内容/文本描述
     */
    private String submissionContent;

    /**
     * 提交分数
     */
    private BigDecimal score;

    /**
     * 批改人评语
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
     * 批改人 ID
     */
    private Integer graderId;

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

    /**
     * 附件列表
     */
    private java.util.List<AttachmentInfo> attachments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private Integer id;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String fileType;
        private LocalDateTime uploadTime;
    }
}
