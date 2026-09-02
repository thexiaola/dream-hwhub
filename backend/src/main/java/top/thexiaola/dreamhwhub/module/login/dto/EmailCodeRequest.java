package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送注册验证码请求 DTO
 */
@Data
public class EmailCodeRequest {

    // 学号/工号
    @NotBlank(message = "学号不能为空")
    @Size(max = 24, message = "学号长度不能超过 24 位")
    @Pattern(regexp = "^[0-9]+$", message = "学号/工号只能包含数字")
    private String userNo;

    // 用户名（参考 Minecraft 命名规则：3-16 位字母/数字/下划线，不能以下划线开头或结尾）
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 16, message = "用户名长度需为 3-16 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Pattern(regexp = "^[A-Za-z0-9].*[A-Za-z0-9]$", message = "用户名不能以下划线开头或结尾")
    private String username;

    // 邮箱
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过 64 位")
    private String email;
}
