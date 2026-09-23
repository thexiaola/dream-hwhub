package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 班级详情响应VO
 */
@Data
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

    /**
     * 班级是否已「冻结」：创建者的教师身份被解除。
     * 冻结期间原创建者与班级老师均无法管理该班，邀请码失效、不再接纳新学生
     */
    private Boolean frozen;

    /**
     * 创建者是否仍具备教师身份（即班级是否处于可正常管理状态）
     */
    private Boolean ownerActive;

    /**
     * 当前用户是否可以申请接管该班级（班级已冻结、本人是该校老师、且非本班已接管的老师）
     */
    private Boolean canTakeover;

    /**
     * 当前用户是否已提交待审核的接管申请
     */
    private Boolean takeoverPending;

    /**
     * 该班所属学校的班级接管是否自动同意（班级冻结时用于提示）
     */
    private Boolean takeoverAutoApprove;

    /**
     * 基础字段构造器（冻结相关字段默认由 setter 补充，避免破坏既有调用）
     */
    public ClassDetailResponse(Integer id, String className, Integer schoolId, String schoolName,
            Integer ownerId, String ownerName, String userRole, Integer userRoleCode,
            Long memberCount, Long teacherCount, Long studentCount, String description,
            Boolean allowStudentInvite, LocalDateTime createTime) {
        this.id = id;
        this.className = className;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.userRole = userRole;
        this.userRoleCode = userRoleCode;
        this.memberCount = memberCount;
        this.teacherCount = teacherCount;
        this.studentCount = studentCount;
        this.description = description;
        this.allowStudentInvite = allowStudentInvite;
        this.createTime = createTime;
    }
}
