package top.thexiaola.dreamhwhub.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可添加用户 VO（校内用户搜索 / 推荐结果）
 * <p>
 * 用于「添加好友」时搜索同校用户，附带与当前用户的好友关系状态。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddableUserInfo {

    /**
     * 用户 ID
     */
    private Integer userId;

    /**
     * 所属学校 ID
     */
    private Integer schoolId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 在该校的姓名
     */
    private String realName;

    /**
     * 在该校的学工号
     */
    private String staffNo;

    /**
     * 在该校的角色名称（学校管理员/教师/学生）
     */
    private String role;

    /**
     * 与当前用户的关系：none-非好友，pending_out-我已申请，pending_in-对方已申请，friend-已是好友
     */
    private String relation;
}
