package top.thexiaola.dreamhwhub.dto;

public class LoginRequest {
    private String userNo;      // 学号
    private String email;       // 邮箱
    private String password;    // 密码

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
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
}