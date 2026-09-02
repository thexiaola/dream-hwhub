package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户信息请求DTO
 */
@Data
public class ModifyUserInfoRequest {

    // 学号/工号（仅允许数字，可重复、必填，可在编辑个人信息时修改）
    @NotBlank(message = "学号不能为空")
    @Size(max = 24, message = "学号/工号长度不能超过 24 位")
    @Pattern(regexp = "^[0-9]+$", message = "学号/工号只能包含数字")
    private String userNo;
    
    // 用户名（参考 Minecraft 命名规则：3-16 位字母/数字/下划线，不能以下划线开头或结尾，大小写不敏感唯一）
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 16, message = "用户名长度需为 3-16 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @Pattern(regexp = "^[A-Za-z0-9].*[A-Za-z0-9]$", message = "用户名不能以下划线开头或结尾")
    private String username;
    
    // 身份证姓名（允许英文、汉字、日语等文字，不允许特殊字符）
    @Size(max = 32, message = "身份证姓名长度不能超过 32 位")
    @Pattern(regexp = "^[\\p{L}\\s·-]+$", message = "身份证姓名只能包含字母、汉字及允许的符号（空格、中点、连字符）")
    private String idName;
    
    // 手机号（兼容国外手机号，允许数字、+、-、空格、括号）
    @Size(max = 20, message = "手机号长度不能超过 20 位")
    @Pattern(regexp = "^[+]?[0-9()\\-\\s]+$", message = "手机号格式不正确，请检查后重新输入")
    private String phone;
}
