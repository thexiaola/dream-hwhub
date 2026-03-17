package top.thexiaola.dreamhwhub.module.login.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 用户信息响应 DTO
 */
@JsonPropertyOrder({"id", "userNo", "username", "email", "permission"})
public class UserResponse {

    // 用户编号
    private Integer id;

    // 学号/工号
    private String userNo;

    // 用户名
    private String username;

    // 邮箱
    private String email;

    // 权限级别
    private Short permission;

    /**
     * 从User实体创建响应对象
     * @param user 用户实体
     * @return 用户响应对象
     */
    public static UserResponse fromEntity(top.thexiaola.dreamhwhub.module.login.domain.User user) {
        if (user == null) {
            return null;
        }
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUserNo(user.getUserNo());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPermission(user.getPermission());
        return response;
    }

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

    public Short getPermission() {
        return permission;
    }

    public void setPermission(Short permission) {
        this.permission = permission;
    }
}