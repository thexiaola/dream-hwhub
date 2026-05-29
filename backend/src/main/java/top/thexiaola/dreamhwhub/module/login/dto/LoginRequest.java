package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.NotBlank;
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

    // 密码（登录时不进行格式验证，统一返回账号或密码错误）
    @NotBlank(message = "密码不能为空")
    private String password;
}