package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 批改作业请求
 */
@Data
public class GradeWorkRequest {

    /**
     * 提交 ID
     */
    @NotNull(message = "提交 ID 不能为空")
    private Integer submissionId;

    /**
     * 分数
     */
    @NotNull(message = "分数不能为空")
    @Min(value = 0, message = "分数不能小于 0")
    @Max(value = 100, message = "分数不能大于 100")
    private BigDecimal score;

    /**
     * 批改人评语
     */
    @NotBlank(message = "评语不能为空")
    private String comment;
}
