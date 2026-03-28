package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送找回密码验证码请求 DTO
 */
public class RetrievePasswordCodeRequest {

    // 账号（学号/用户名/邮箱）
    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号长度不能超过 100 位")
    private String account;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
