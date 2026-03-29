package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建作业请求
 */
@Data
public class CreateWorkRequest {

    /**
     * 作业标题
     */
    @NotBlank(message = "作业标题不能为空")
    private String title;

    /**
     * 作业描述
     */
    @NotBlank(message = "作业描述不能为空")
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
    private Integer totalScore = 100;

    /**
     * 所属班级 ID
     */
    @NotNull(message = "所属班级 ID 不能为空")
    private Integer classId;

    /**
     * 附件列表（文件路径）
     */
    private java.util.List<String> attachmentPaths;
}
