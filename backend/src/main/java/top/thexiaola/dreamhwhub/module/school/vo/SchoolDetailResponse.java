package top.thexiaola.dreamhwhub.module.school.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学校详情 VO
 * 除学校基本信息与统计外，携带当前登录用户在该校的身份与申请状态
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchoolDetailResponse {

    /**
     * 学校 ID
     */
    private Integer id;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 学校描述
     */
    private String description;

    /**
     * 加入学校是否免审核
     */
    private Boolean allowJoinWithoutApproval;

    /**
     * 成员总数
     */
    private Long memberCount;

    /**
     * 学校管理员数量
     */
    private Long adminCount;

    /**
     * 老师数量
     */
    private Long teacherCount;

    /**
     * 学生数量
     */
    private Long studentCount;

    /**
     * 班级数量
     */
    private Long classCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 当前用户是否已是该校成员
     */
    private Boolean member;

    /**
     * 当前用户在该校的角色代码：2-学校管理员，1-老师，0-学生，null-非成员
     */
    private Integer myRoleCode;

    /**
     * 当前用户在该校的角色名称
     */
    private String myRole;

    /**
     * 当前用户在该校的学工号（非成员为 null）
     */
    private String myStaffNo;

    /**
     * 当前用户在该校的姓名（非成员为 null）
     */
    private String myRealName;

    /**
     * 当前用户待审核的加入申请状态：0-待审核，1-已通过，2-已拒绝，null-无申请
     */
    private Integer myApplicationStatus;
}
