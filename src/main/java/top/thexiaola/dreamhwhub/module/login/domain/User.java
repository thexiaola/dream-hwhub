package top.thexiaola.dreamhwhub.module.login.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 用户实体类
 * 对应数据库表：user
 */
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

    // 邮箱
    @TableField("email")
    private String email;

    //密码
    @TableField("password")
    private String password;

    // 权限级别
    @TableField("permission")
    private Short permission = 1;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Short getPermission() {
        return permission == null ? 1 : permission;
    }

    public void setPermission(Short permission) {
        this.permission = permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id);
    }
}