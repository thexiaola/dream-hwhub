package top.thexiaola.dreamhwhub.module.login.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应 DTO
 */
@Data
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime registerTime;

    // 最后登录时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
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

}