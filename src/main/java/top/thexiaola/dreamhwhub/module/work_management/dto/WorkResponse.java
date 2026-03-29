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
     * 发布人 ID
     */
    private Integer publisherId;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 作业总分
     */
    private Integer totalScore;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 作业状态：0-未发布，1-已发布，2-已结束（动态计算）
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
