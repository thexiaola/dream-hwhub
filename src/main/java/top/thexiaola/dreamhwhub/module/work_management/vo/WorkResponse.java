package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业响应VO
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
     * 发布人用户名
     */
    private String publisherName;

    /**
     * 所属班级 ID
     */
    private Integer classId;

    /**
     * 班级名称
     */
    private String className;

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
    private List<AttachmentInfo> attachments;

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
