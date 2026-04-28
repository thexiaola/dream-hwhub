package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建作业请求
 */
@Data
public class CreateWorkRequest {

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
    @Pattern(regexp = "^[^\\t\\f\\v]+$", message = "作业描述不能包含特殊字符（制表符等）")
    private String description;

    /**
     * 截止时间
     */
    @NotNull(message = "截止时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deadline;

    /**
     * 作业总分
     */
    @NotNull(message = "作业总分不能为空")
    private Integer totalScore = 100;

    /**
     * 是否允许逾期提交（默认true）
     */
    private Boolean allowLateSubmit = true;

    /**
     * 所属班级 ID
     */
    @NotNull(message = "所属班级 ID 不能为空")
    private Integer classId;

    /**
     * 发布时间（必填，立即发布则传当前时间）
     */
    @NotNull(message = "发布时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime publishTime;

    /**
     * 附件文件列表（直接上传的文件）
     */
    private List<MultipartFile> attachments;
}
