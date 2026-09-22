package top.thexiaola.dreamhwhub.module.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员编辑用户请求
 * 字段为空表示不修改；password 非空时重置密码
 */
@Data
public class AdminUpdateUserRequest {

    // 用户名
    @Size(min = 3, max = 16, message = "用户名长度需为 3-16 位")
    @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    // 邮箱
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过 64 位")
    private String email;

    // 手机号
    @Size(max = 20, message = "手机号长度不能超过 20 位")
    @Pattern(regexp = "^[+]?[0-9()\\-\\s]*$", message = "手机号格式不正确，请检查后重新输入")
    private String phone;

    // 新密码（可选，非空时重置密码）
    @Size(min = 4, max = 48, message = "密码长度必须在 4-48 位之间")
    private String password;
}
