package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注销账号请求
 * <p>
 * 注销是不可逆操作，须由账号所有者本人凭登录密码确认。
 */
@Data
public class DeleteAccountRequest {

    /**
     * 登录密码（用于验证账号所有者身份）
     */
    @NotBlank(message = "请输入登录密码以验证身份")
    private String password;
}
