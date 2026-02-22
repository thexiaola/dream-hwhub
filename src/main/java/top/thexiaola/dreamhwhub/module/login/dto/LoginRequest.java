package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求DTO
 */
public class LoginRequest {

    // 账号（可以是学号、用户名或邮箱）
    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号长度不能超过100位")
    private String account;

    // 密码
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度必须在6-50位之间")
    private String password;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}