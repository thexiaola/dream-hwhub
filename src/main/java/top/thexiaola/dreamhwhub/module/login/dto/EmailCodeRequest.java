package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发送注册验证码请求 DTO
 */
public class EmailCodeRequest {

    // 学号/工号
    @NotBlank(message = "学号不能为空")
    @Size(max = 24, message = "学号长度不能超过 24 位")
    private String userNo;

    // 用户名
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64 位")
    private String username;

    // 邮箱
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过 64 位")
    private String email;

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
