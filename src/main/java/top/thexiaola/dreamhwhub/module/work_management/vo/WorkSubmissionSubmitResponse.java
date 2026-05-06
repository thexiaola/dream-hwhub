package top.thexiaola.dreamhwhub.module.work_management.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 作业提交响应VO（仅包含提交基本信息，不包含批改信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkSubmissionSubmitResponse {

    /**
     * 提交 ID
     */
    private Integer id;

    /**
     * 作业 ID
     */
    private Integer workId;

    /**
     * 所属班级 ID
     */
    private Integer classId;

    /**
     * 提交人 ID
     */
    private Integer submitterId;

    /**
     * 提交内容/文本描述
     */
    private String submissionContent;

    /**
     * 提交状态：1-已提交，2-已批改
     */
    private Integer status;

    /**
     * 是否逾期提交：true-逾期，false-按时
     */
    private Boolean isLate;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
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
