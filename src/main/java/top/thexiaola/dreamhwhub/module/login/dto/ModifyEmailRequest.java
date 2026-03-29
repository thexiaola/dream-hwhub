package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改用户邮箱请求 DTO
 */
public class ModifyEmailRequest {

    // 原邮箱验证码（beforeCode）
    @NotBlank(message = "原邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度为 6 位")
    private String beforeCode;

    // 新邮箱
    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过 64 位")
    private String newEmail;

    // 新邮箱验证码（afterCode）
    @NotBlank(message = "新邮箱验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码长度为 6 位")
    private String afterCode;

    public String getBeforeCode() {
        return beforeCode;
    }

    public void setBeforeCode(String beforeCode) {
        this.beforeCode = beforeCode;
    }

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }

    public String getAfterCode() {
        return afterCode;
    }

    public void setAfterCode(String afterCode) {
        this.afterCode = afterCode;
    }
}
