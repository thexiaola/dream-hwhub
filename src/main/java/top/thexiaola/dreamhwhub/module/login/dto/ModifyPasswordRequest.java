package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class ModifyPasswordRequest {

    @NotBlank(message = "原密码不为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^[0-9a-zA-Z!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$", message = "新密码只能包含字母、数字和特殊字符")
    @Size(min = 6, max = 48, message = "新密码长度必须在 6-48 位之间")
    private String newPassword;
}
