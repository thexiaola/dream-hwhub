package top.thexiaola.dreamhwhub.module.login.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表：user
 */
@Data
@TableName("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 用户编号
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 学号/工号
    @TableField("user_no")
    private String userNo;

    // 用户名
    @TableField("username")
    private String username;

    // 身份证姓名
    @TableField("id_name")
    private String idName;

    // 邮箱
    @TableField("email")
    private String email;

    // 手机号
    @TableField("phone")
    private String phone;

    //密码
    @TableField("password")
    private String password;

    // 权限级别
    @TableField("permission")
    private Short permission = 1;

    // 是否被封禁：0-正常，1-封禁
    @TableField("is_banned")
    private Boolean isBanned = false;

    // 封禁原因
    @TableField("ban_reason")
    private String banReason;

    // 注册时间
    @TableField("register_time")
    private LocalDateTime registerTime;

    // 最后登录时间
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
}