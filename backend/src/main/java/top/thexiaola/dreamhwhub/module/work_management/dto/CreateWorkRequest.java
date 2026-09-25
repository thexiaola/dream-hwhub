package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.support.validation.XssValidator;

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
    @Size(max = 1024, message = "作业描述长度不能超过 1024 位")
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
    @Min(value = 1, message = "作业总分必须大于0")
    @Max(value = 1000, message = "作业总分不能超过1000")
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
     * 附件文件列表（直接上传的文件）
     */
    private List<MultipartFile> attachments;

    /**
     * 结构化题目列表（可选）：填写后学生将逐题作答，客观题自动评判。
     * 为空时表示纯文本作业（与原行为一致）。
     */
    private List<QuestionItem> questions;

    /**
     * 类型（可选）：homework-作业（默认），exam-考试
     */
    private String workType;

    /**
     * 考试配置（workType=exam 时生效）：限时与反作弊设置
     */
    private ExamConfigDto examConfig;

    /**
     * 自定义校验：XSS防护和时间逻辑校验
     */
    public void validate() {
        // XSS防护
        XssValidator.validateNoXss(title, "作业标题");
        XssValidator.validateNoXss(description, "作业描述");
    }
}
