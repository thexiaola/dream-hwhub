package top.thexiaola.dreamhwhub.module.school.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学校成员响应 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchoolMemberResponse {

    /**
     * 成员记录 ID
     */
    private Integer id;

    /**
     * 学校 ID
     */
    private Integer schoolId;

    /**
     * 用户 ID
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 姓名（学校内身份）
     */
    private String realName;

    /**
     * 学工号（学校内身份）
     */
    private String staffNo;

    /**
     * 角色代码：2-学校管理员，1-老师，0-学生
     */
    private Integer roleCode;

    /**
     * 角色名称（学校管理员/老师/学生）
     */
    private String role;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
}
