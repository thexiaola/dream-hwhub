package top.thexiaola.dreamhwhub.module.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 管理员视角的用户信息 VO
 */
@Data
public class AdminUserVO {

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

    // 是否被封禁
    private Boolean isBanned;

    // 封禁原因
    private String banReason;

    // 所属权限组名称
    private List<String> groupNames;

    // 生效的权限节点
    private Set<String> permissions;

    // 注册时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime registerTime;

    // 最后登录时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;
}
