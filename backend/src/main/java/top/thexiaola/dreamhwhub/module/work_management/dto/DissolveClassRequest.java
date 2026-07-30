package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 解散班级请求（危险操作，需二次校验账号密码+确认文案）
 */
@Data
public class DissolveClassRequest {

    /**
     * 用户账号（登录账号，学号/工号或邮箱）
     */
    @NotBlank(message = "账号不能为空")
    @Size(max = 128, message = "账号长度不能超过 128 位")
    private String account;

    /**
     * 用户密码（明文，后端做 BCrypt 比对）
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码长度不能超过 128 位")
    private String password;

    /**
     * 用户手写的确认文案：我已确认要删除{课堂名}课堂
     */
    @NotBlank(message = "请输入确认文案")
    @Size(max = 256, message = "确认文案过长")
    private String confirmText;
}
