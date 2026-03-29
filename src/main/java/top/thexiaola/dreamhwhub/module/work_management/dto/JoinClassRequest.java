package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 加入班级请求
 */
@Data
public class JoinClassRequest {

    /**
     * 班级 ID（数字格式）
     */
    @NotBlank(message = "班级 ID 不能为空")
    @Pattern(regexp = "^\\d+$", message = "班级 ID 必须是数字")
    private String classCode;

    /**
     * 是否是老师（true=老师，false=学生）
     */
    @NotNull(message = "角色不能为空")
    private Boolean isTeacher;
}
