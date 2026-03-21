package top.thexiaola.dreamhwhub.module.login.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * 用户信息响应 DTO
 */
@JsonPropertyOrder({"id", "userNo", "username", "idName", "email", "phone", "permission", "isBanned", "registerTime", "lastLoginTime"})
public class UserResponse {

    // 用户编号
    private Integer id;

    // 学号/工号
    private String userNo;

    // 用户名
    private String username;

    // 身份证姓名
    private String idName;

    // 邮箱
    private String email;

    // 手机号
    private String phone;

    // 权限级别
    private Short permission;

    // 是否被封禁：0-正常，1-封禁
    private Boolean isBanned;

    // 注册时间
    private LocalDateTime registerTime;

    // 最后登录时间
    private LocalDateTime lastLoginTime;

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
        response.setIdName(user.getIdName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setPermission(user.getPermission());
        response.setIsBanned(user.getIsBanned());
        response.setRegisterTime(user.getRegisterTime());
        response.setLastLoginTime(user.getLastLoginTime());
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

    public String getIdName() {
        return idName;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Short getPermission() {
        return permission;
    }

    public void setPermission(Short permission) {
        this.permission = permission;
    }

    public Boolean getIsBanned() {
        return isBanned;
    }

    public void setIsBanned(Boolean isBanned) {
        this.isBanned = isBanned;
    }

    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(LocalDateTime registerTime) {
        this.registerTime = registerTime;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}