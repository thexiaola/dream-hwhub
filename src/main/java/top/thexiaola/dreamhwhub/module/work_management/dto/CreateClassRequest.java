package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建班级请求
 */
@Data
public class CreateClassRequest {

    /**
     * 班级名称（允许字母、汉字和常用特殊字符，不允许换行符、制表符等不常见字符）
     */
    @NotBlank(message = "班级名称不能为空")
    @Size(max = 64, message = "班级名称长度不能超过 64 位")
    @Pattern(regexp = "^[^\\r\\n\\t\\f\\v]+$", message = "班级名称不能包含特殊字符（换行符、制表符等）")
    private String className;

    /**
     * 班级描述
     */
    @Size(max = 512, message = "班级描述长度不能超过 512 位")
    @Pattern(regexp = "^[^\\t\\f\\v]*$", message = "班级描述不能包含特殊字符（制表符等）")
    private String description;
}
