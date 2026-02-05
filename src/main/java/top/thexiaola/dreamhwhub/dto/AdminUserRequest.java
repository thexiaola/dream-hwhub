package top.thexiaola.dreamhwhub.dto;

public class AdminUserRequest {
    private String userNo;      // 学号
    private String username;    // 用户名
    private String email;       // 邮箱
    private String password;    // 密码
    private Short permission;   // 权限等级

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
        return permission;
    }

    public void setPermission(Short permission) {
        this.permission = permission;
    }
}