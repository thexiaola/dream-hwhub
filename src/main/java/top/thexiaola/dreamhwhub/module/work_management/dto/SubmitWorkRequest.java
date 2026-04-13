package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 提交作业请求
 */
@Data
public class SubmitWorkRequest {

    /**
     * 作业 ID（数字格式）
     */
    @NotNull(message = "作业 ID 不能为空")
    @Pattern(regexp = "^[0-9]+$", message = "作业 ID 必须是数字")
    private String workId;

    /**
     * 提交内容/文本描述（允许字母、汉字和常用特殊字符，不允许换行符、制表符等不常见字符）
     */
    @Pattern(regexp = "^[^\\t\\f\\v]*$", message = "提交内容不能包含特殊字符（制表符等）")
    private String submissionContent;

    /**
     * 附件ID列表（通过 /api/upload/file 上传后返回的文件ID）
     */
    private java.util.List<Integer> attachmentIds;
}
