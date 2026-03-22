package top.thexiaola.dreamhwhub.module.login.dto;

import jakarta.validation.constraints.Size;

/**
 * 修改用户信息请求DTO
 */
public class ModifyUserInfoRequest {

    // 学号/工号
    @Size(max = 24, message = "学号/工号长度不能超过24位")
    private String userNo;

    // 用户名
    @Size(max = 64, message = "用户名长度不能超过64位")
    private String username;

    // 身份证姓名
    @Size(max = 32, message = "身份证姓名长度不能超过32位")
    private String idName;

    // 手机号
    @Size(max = 20, message = "手机号长度不能超过20位")
    private String phone;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
