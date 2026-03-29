package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交作业请求
 */
@Data
public class SubmitWorkRequest {

    /**
     * 作业 ID
     */
    @NotNull(message = "作业 ID 不能为空")
    private Integer workId;

    /**
     * 提交内容/文本描述
     */
    private String submissionContent;

    /**
     * 附件列表（文件路径）
     */
    private java.util.List<String> attachmentPaths;
}
