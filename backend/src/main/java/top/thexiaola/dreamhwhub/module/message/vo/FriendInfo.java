package top.thexiaola.dreamhwhub.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 好友信息 VO（对应「我的好友」列表项）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendInfo {

    /**
     * 好友关系 ID
     */
    private Integer relationId;

    /**
     * 所属学校 ID
     */
    private Integer schoolId;

    /**
     * 好友用户 ID
     */
    private Integer userId;

    /**
     * 好友用户名
     */
    private String username;

    /**
     * 好友头像相对路径
     */
    private String avatar;

    /**
     * 好友在该校的姓名
     */
    private String realName;

    /**
     * 好友在该校的学工号
     */
    private String staffNo;

    /**
     * 好友在该校的角色名称（学校管理员/教师/学生）
     */
    private String role;

    /**
     * 成为好友的时间
     */
    private java.time.LocalDateTime friendTime;
}
