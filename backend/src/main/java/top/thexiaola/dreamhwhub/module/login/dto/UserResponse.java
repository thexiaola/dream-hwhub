package top.thexiaola.dreamhwhub.module.login.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户信息响应 DTO
 */
@Data
@JsonPropertyOrder({"id", "username", "email", "phone", "avatar", "isOp", "permissions", "verifyByPassword", "verifyByEmailCode", "disabledVerificationOperations", "isBanned", "registerTime", "lastLoginTime", "token"})
public class UserResponse {

    // 用户编号
    private Integer id;

    // 用户名
    private String username;

    // 邮箱
    private String email;

    // 手机号
    private String phone;

    // 头像文件相对路径，空表示未设置
    private String avatar;

    // 是否平台管理员（OP）
    private Boolean isOp;

    // 拥有的权限节点（OP 为全部节点）
    private Set<String> permissions;

    // 危险操作是否启用「密码验证」
    private Boolean verifyByPassword;

    // 危险操作是否启用「邮箱验证码验证」
    private Boolean verifyByEmailCode;

    // 已被该用户关闭二次验证的敏感操作标识（如 class.dissolve）；未列出者默认需要验证
    private Set<String> disabledVerificationOperations;

    // 是否被封禁：0-正常，1-封禁
    private Boolean isBanned;

    // 注册时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime registerTime;

    // 最后登录时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;

    // JWT Token(登录时返回)
    private String token;
}