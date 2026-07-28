package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 邀请用户加入班级请求
 */
@Data
public class InviteUserRequest {

    /**
     * 用户账号（学号/工号）
     */
    @NotBlank(message = "用户账号不能为空")
    @Size(max = 64, message = "用户账号长度不能超过 64 位")
    @Pattern(regexp = "^[^\\r\\n\\t\\f\\v]+$", message = "用户账号不能包含特殊字符（换行符、制表符等）")
    private String userAccount;
}
