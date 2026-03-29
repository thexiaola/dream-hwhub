package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入班级请求
 */
@Data
public class JoinClassRequest {

    /**
     * 班级邀请码
     */
    @NotBlank(message = "班级邀请码不能为空")
    private String classCode;

    /**
     * 是否是老师（true=老师，false=学生）
     */
    @NotNull(message = "角色不能为空")
    private Boolean isTeacher;
}
