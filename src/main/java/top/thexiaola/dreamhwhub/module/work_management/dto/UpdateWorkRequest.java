package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新作业请求
 */
@Data
public class UpdateWorkRequest {

    /**
     * 作业 ID（数字格式）
     */
    @NotNull(message = "作业 ID 不能为空")
    @Pattern(regexp = "^[0-9]+$", message = "作业 ID 必须是数字")
    private String id;

    /**
     * 作业标题（允许字母、汉字和常用特殊字符，不允许换行符、制表符等不常见字符）
     */
    @NotBlank(message = "作业标题不能为空")
    @Size(max = 128, message = "作业标题长度不能超过 128 位")
    @Pattern(regexp = "^[^\\r\\n\\t\\f\\v]+$", message = "作业标题不能包含特殊字符（换行符、制表符等）")
    private String title;

    /**
     * 作业描述（允许字母、汉字和常用特殊字符，不允许换行符、制表符等不常见字符）
     */
    @NotBlank(message = "作业描述不能为空")
    @Size(max = 1024, message = "作业描述长度不能超过 1024 位")
    @Pattern(regexp = "^[^\\t\\f\\v]+$", message = "作业描述不能包含特殊字符（制表符等）")
    private String description;

    /**
     * 截止时间
     */
    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;

    /**
     * 作业总分
     */
    @NotNull(message = "作业总分不能为空")
    private Integer totalScore;

    /**
     * 附件列表（文件路径）
     */
    private java.util.List<String> attachmentPaths;
}
