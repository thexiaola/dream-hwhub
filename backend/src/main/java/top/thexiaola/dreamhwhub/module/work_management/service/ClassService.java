package top.thexiaola.dreamhwhub.module.work_management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.vo.*;

import java.util.List;

/**
 * 班级管理服务接口
 */
public interface ClassService {

    /**
     * 批量将学生设为课代表（班级管理员专用）
     */
    void batchSetAssistantTeachers(Integer classId, List<Integer> studentUserIds);

    /**
     * 检查用户是否是普通班级管理员（非创建者）
     */
    boolean isOrdinaryTeacher(Integer classId, Integer userId);

    /**
     * 批量将学生踢出班级（班级管理员/平台管理员专用）
     */
    void batchKickStudentsFromClass(Integer classId, List<Integer> studentUserIds);

    /**
     * 取消课代表的班级管理员权限（降级为普通成员，仅创建者或平台管理员可用）
     */
    void demoteAssistantTeacher(Integer classId, Integer teacherUserId);

    /**
     * 学生邀请用户加入班级（需要用户确认和教师审核）
     */
    void studentInviteUser(Integer classId, String userAccount);

    /**
     * 被邀请用户响应邀请（同意/拒绝）
     * 同意时以学校成员身份加入班级
     */
    void respondUserInvitation(Integer invitationId, Boolean accepted);

    /**
     * 班级管理员审核邀请申请
     */
    void approveTeacherApproval(Integer approvalId, Boolean approved, String comment);

    /**
     * 获取待审核的邀请列表（班级管理员专用）
     */
    List<TeacherApprovalResponse> getPendingTeacherApprovals(Integer classId);

    /**
     * 退出班级
     * @return 退出的班级名称
     */
    String leaveClass(Integer classId);

    /**
     * 解散班级（仅创建者或管理员，需账号密码二次校验+确认文案）
     */
    void dissolveClass(Integer classId, String account, String password, String confirmText);

    /**
     * 检查用户是否拥有该班级的管理员权限
     */
    boolean isTeacher(Integer classId, Integer userId);
    
    /**
     * 获取用户作为班级管理员的所有班级 ID 列表
     *
     * @param userId 用户 ID
     * @return 班级ID列表
     */
    List<Integer> getTeacherClassIds(Integer userId);

    /**
     * 获取用户作为班级成员（学生或老师）加入的所有班级 ID
     * @param userId 用户 ID
     * @return 班级 ID 列表
     */
    List<Integer> getMemberClassIds(Integer userId);

    /**
     * 检查用户在指定班级是否是学生
     */
    boolean isStudent(Integer classId, Integer userId);

    /**
     * 判断用户是否可以提交作业：
     * 普通成员（role=0）或班级管理员（role=1）可以提交，创建者不可提交
     */
    boolean canSubmitWork(Integer classId, Integer userId);

    /**
     * 检查用户是否在指定班级中
     */
    boolean isClassMember(Integer classId, Integer userId);

    /**
     * 获取用户在班级中的角色代码
     * @param classId 班级 ID
     * @param userId 用户 ID
     * @return 角色代码：1-拥有班级管理员权限，0-普通成员，null-非成员
     */
    Integer getUserRoleCodeInClass(Integer classId, Integer userId);

    /**
     * 获取用户在班级中的角色名称
     * @param classId 班级 ID
     * @param userId 用户 ID
     * @return 角色名称（创建者/老师/课代表/学生），如果不是成员则返回 null
     */
    String getUserRoleNameInClass(Integer classId, Integer userId);

    /**
     * 批量获取班级信息
     *
     * @param classIds 班级ID列表
     * @return 班级信息列表
     */
    List<ClassInfo> getClassByIds(List<Integer> classIds);

    /**
     * 获取班级详情（包含统计信息）
     */
    ClassDetailResponse getClassDetail(Integer classId);

    /**
     * 获取用户加入的所有班级（分页）
     */
    Page<ClassDetailResponse> getMyClasses(Integer userId, Integer pageNum, Integer pageSize);

    /**
     * 管理员获取全部班级（用于管理面板的班级管理入口）
     */
    Page<ClassDetailResponse> getAdminManageClasses(Integer userId, Integer pageNum, Integer pageSize, String keyword);

    /**
     * 获取班级成员列表（分页）
     */
    Page<ClassMemberResponse> getClassMembers(Integer classId, Integer pageNum, Integer pageSize);

    /**
     * 获取班级所有成员列表（不分页）
     */
    List<ClassMemberResponse> getAllClassMembers(Integer classId);

    /**
     * 创建班级（创建者自动成为班级管理员）
     *
     * @param schoolId    班级所属学校 ID
     * @param className   班级名称
     * @param description 班级描述
     * @return 创建完成的班级实体
     */
    ClassInfo createClass(Integer schoolId, String className, String description);

    /**
     * 提交加入班级申请（姓名与学工号取自学校成员身份）
     * @return 加入申请响应对象
     */
    JoinClassApplicationResponse submitJoinClassRequest(Integer classId);

    /**
     * 获取加入班级申请列表（班级管理员或平台管理员专用，分页）
     * @param classId 班级 ID 筛选，可选
     * @param status 状态筛选（0-待审核，1-已通过，2-已拒绝），可选
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小
     * @return 按创建时间倒序排列的申请分页结果
     */
    Page<JoinClassApplicationResponse> getJoinApplications(Integer classId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 审核加入班级申请
     */
    void approveJoinApplication(Integer applicationId, Boolean approved, String comment);

    /**
     * 更新班级信息（班级管理员）
     *
     * @param classId 班级 ID
     * @param className 班级名称
     * @param description 班级描述
     * @return 更新后的班级信息
     */
    ClassDetailResponse updateClassInfo(Integer classId, String className, String description);

    /**
     * 更新学生邀请设置（班级管理员）
     *
     * @param classId 班级 ID
     * @param allowStudentInvite 是否允许学生邀请同学加入
     */
    void setStudentInviteAllowed(Integer classId, Boolean allowStudentInvite);

    /**
     * 获取待当前用户确认的学生邀请列表
     *
     * @param userId 当前用户 ID
     * @return 待确认的学生邀请列表
     */
    List<InvitationResponse> getMyUserInvitations(Integer userId);

    /**
     * 教师邀请用户加入班级（需用户同意）
     */
    InvitationResponse inviteUserToClassWithApproval(Integer classId, String userAccount);

    /**
     * 获取用户收到的邀请列表
     */
    List<InvitationResponse> getMyInvitations(Integer userId, Integer status);

    /**
     * 用户响应邀请（同意/拒绝）
     * 同意时以学校成员身份加入班级
     */
    void respondInvitation(Integer invitationId, Boolean accepted);

    /**
     * 获取班级当前邀请码（不存在则生成，存在则返回已有的，不刷新）
     */
    String getInviteCode(Integer classId);

    /**
     * 重置班级邀请码（旧码失效，生成新码覆盖）
     */
    String resetInviteCode(Integer classId);

    /**
     * 通过邀请码加入班级
     */
    JoinClassApplicationResponse joinClassByInviteCode(String inviteCode);

    /**
     * 转让班级所有权
     */
    void transferClassOwnership(Integer classId, Integer newOwnerId);
}
