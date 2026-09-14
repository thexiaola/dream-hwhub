package top.thexiaola.dreamhwhub.module.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员新增用户请求
 */
@Data
public class AdminCreateUserRequest {

    // 学号/工号（仅允许数字）
    @NotBlank(message = "学号/工号不能为空")
    @Size(max = 24, message = "学号/工号长度不能超过 24 位")
    @Pattern(regexp = "^[0-9]+$", message = "学号/工号只能包含数字")
    private String userNo;

    // 用户名（参考 Minecraft 命名规则：3-16 位字母/数字/下划线）
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 16, message = "用户名长度需为 3-16 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Pattern(regexp = "^[A-Za-z0-9].*[A-Za-z0-9]$", message = "用户名不能以下划线开头或结尾")
    private String username;

    // 姓名
    @Size(max = 32, message = "姓名长度不能超过 32 位")
    @Pattern(regexp = "^[\\p{L}\\s·-]*$", message = "姓名只能包含字母、汉字及允许的符号（空格、中点、连字符）")
    private String idName;

    // 邮箱
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过 64 位")
    private String email;

    // 手机号
    @Size(max = 20, message = "手机号长度不能超过 20 位")
    @Pattern(regexp = "^[+]?[0-9()\\-\\s]*$", message = "手机号格式不正确，请检查后重新输入")
    private String phone;

    // 初始密码
    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 48, message = "密码长度必须在 4-48 位之间")
    private String password;
}
