package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级详情响应VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassDetailResponse {

    /**
     * 班级 ID
     */
    private Integer id;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 班级所属学校 ID
     */
    private Integer schoolId;

    /**
     * 班级所属学校名称
     */
    private String schoolName;

    /**
     * 班级所有者 ID
     */
    private Integer ownerId;

    /**
     * 班级所有者用户名
     */
    private String ownerName;

    /**
     * 用户在该班级的角色名称（创建者/老师/课代表/学生）
     */
    private String userRole;

    /**
     * 用户在该班级的角色代码：1-拥有班级管理员权限，0-普通成员
     */
    private Integer userRoleCode;

    /**
     * 成员总数
     */
    private Long memberCount;

    /**
     * 教师数量
     */
    private Long teacherCount;

    /**
     * 学生数量
     */
    private Long studentCount;

    /**
     * 班级描述
     */
    private String description;

    /** 是否允许学生邀请同学加入 */
    private Boolean allowStudentInvite;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
