package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 班级成员响应VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassMemberResponse {

    /**
     * 成员 ID
     */
    private Integer id;

    /**
     * 用户 ID
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 班级内姓名（学生入班时填写，老师为空）
     */
    private String studentName;

    /**
     * 班级内学号（学生入班时填写，老师为空）
     */
    private String studentNo;

    /**
     * 角色名称（创建者/老师/课代表/学生）
     */
    private String role;

    /**
     * 角色代码：1-拥有班级管理员权限，0-普通成员
     */
    private Integer roleCode;

    /**
     * 加入时间
     */
    private java.time.LocalDateTime joinTime;

    /**
     * 班级教师总数
     */
    private Long teacherCount;
}
