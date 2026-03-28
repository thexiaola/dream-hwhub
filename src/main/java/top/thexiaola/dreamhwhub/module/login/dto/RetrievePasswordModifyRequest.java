package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 找回密码修改密码请求 DTO
 */
public class RetrievePasswordModifyRequest {

    // 账号（学号/用户名/邮箱）
    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号长度不能超过 100 位")
    private String account;

    // 验证码
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度为 6 位")
    private String code;

    // 新密码
    @NotBlank(message = "新密码不符合预期")
    @Pattern(regexp = "^[0-9a-zA-Z!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$", message = "新密码不符合预期")
    @Size(min = 6, max = 50, message = "新密码不符合预期")
    private String newPassword;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
