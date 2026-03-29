package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
public class RegisterRequest {

    // 学号/工号（仅允许数字）
    @NotBlank(message = "学号/工号不能为空")
    @Size(max = 24, message = "学号/工号长度不能超过24位")
    @Pattern(regexp = "^[0-9]+$", message = "学号/工号只能包含数字")
    private String userNo;

    // 用户名（允许字母、汉字和常用特殊字符，不允许换行符、制表符等不常见字符）
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64 位")
    @Pattern(regexp = "^[^\\r\\n\\t\\f\\v]+$", message = "用户名不能包含特殊字符（换行符、制表符等）")
    private String username;

    // 邮箱
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过 64 位")
    private String email;

    // 邮箱验证码
    @NotBlank(message = "邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度为 6 位")
    private String emailCode;

    // 密码
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^[0-9a-zA-Z!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$", message = "密码只能包含字母、数字和特殊字符")
    @Size(min = 6, max = 48, message = "密码长度必须在 6-48 位之间")
    private String password;
}