package top.thexiaola.dreamhwhub.module.login.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户信息查询响应 DTO（不含 token，token 仅在登录时返回）
 */
@Data
@JsonPropertyOrder({"id", "username", "email", "phone", "avatar", "isOp", "permissions", "isBanned", "registerTime", "lastLoginTime"})
public class UserInfoResponse {

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

    // 是否被封禁：0-正常，1-封禁
    private Boolean isBanned;

    // 注册时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime registerTime;

    // 最后登录时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;
}
