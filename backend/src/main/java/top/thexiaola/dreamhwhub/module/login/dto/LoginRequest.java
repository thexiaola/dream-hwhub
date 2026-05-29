package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户登录请求DTO
 */
@Data
public class LoginRequest {

    // 账号（可以是学号、用户名或邮箱）
    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号长度不能超过100位")
    private String account;

    // 密码
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^[0-9a-zA-Z!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$", message = "密码只能包含字母、数字和特殊字符")
    @Size(min = 6, max = 50, message = "密码长度必须在 6-50 位之间")
    private String password;
}