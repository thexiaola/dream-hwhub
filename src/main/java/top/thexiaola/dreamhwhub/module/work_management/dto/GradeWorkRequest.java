package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.*;
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
    private BigDecimal score;

    /**
     * 批改人评语（允许字母、汉字和常用特殊字符，不允许换行符、制表符等不常见字符）
     */
    @NotBlank(message = "评语不能为空")
    @Size(max = 512, message = "评语长度不能超过 512 位")
    @Pattern(regexp = "^[^\\t\\f\\v]+$", message = "评语不能包含特殊字符（制表符等）")
    private String comment;

    /**
     * 是否打回（true-打回让学生修改，false-正常批改）
     */
    private Boolean isReturned = false;
}
