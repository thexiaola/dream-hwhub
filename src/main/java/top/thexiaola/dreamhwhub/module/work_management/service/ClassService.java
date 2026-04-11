package top.thexiaola.dreamhwhub.module.work_management.service;

import top.thexiaola.dreamhwhub.module.work_management.domain.ClassCreateApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInvitation;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInviteApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassJoinApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.vo.ClassDetailResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.ClassMemberResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.InvitationResponse;

import java.util.List;

/**
 * 班级管理服务接口
 */
public interface ClassService {

    /**
     * 添加用户为班级老师（管理员或老师专用）
     */
    ClassMember addTeacherToClass(Integer classId, String userAccount);

    /**
     * 设置学生为助理老师（老师专用）
     */
    void setStudentAsAssistantTeacher(Integer classId, Integer studentUserId);

    /**
     * 检查用户是否是普通老师（非创建者）
     */
    boolean isOrdinaryTeacher(Integer classId, Integer userId);

    /**
     * 将学生踢出班级（老师/助理老师专用）
     */
    void removeStudentFromClass(Integer classId, Integer studentUserId);

    /**
     * 取消助理老师权限（降级为学生，仅创建者可用）
     */
    void demoteAssistantTeacher(Integer classId, Integer teacherUserId);

    /**
     * 学生邀请用户加入班级（需要审核）
     */
    ClassInviteApplication studentInviteUser(Integer classId, String userAccount);

    /**
     * 审核邀请申请（老师/管理员专用）
     */
    void approveInviteApplication(Integer applicationId, Boolean approved, String comment);

    /**
     * 获取待审核的邀请申请列表（班级老师专用）
     */
    List<ClassInviteApplication> getPendingInviteApplications(Integer classId);

    /**
     * 退出班级
     */
    void leaveClass(Integer classId);

    /**
     * 删除班级（仅创建者）
     */
    void deleteClass(Integer classId);

    /**
     * 查询用户所在的班级列表
     */
    List<ClassInfo> getUserClasses(Integer userId);

    /**
     * 检查用户在指定班级是否是老师
     */
    boolean isTeacher(Integer classId, Integer userId);

    /**
     * 检查用户在指定班级是否是学生
     */
    boolean isStudent(Integer classId, Integer userId);

    /**
     * 检查用户是否在指定班级中
     */
    boolean isClassMember(Integer classId, Integer userId);

    /**
     * 获取班级信息
     */
    ClassInfo getClassById(Integer classId);

    /**
     * 获取班级详情（包含统计信息）
     */
    ClassDetailResponse getClassDetail(Integer classId);

    /**
     * 获取用户加入的所有班级
     */
    List<ClassDetailResponse> getMyClasses(Integer userId);

    /**
     * 获取班级成员列表
     */
    List<ClassMemberResponse> getClassMembers(Integer classId);

    /**
     * 更新成员角色
     */
    void updateMemberRole(Integer classId, Integer userId, Boolean isTeacher);

    /**
     * 提交创建班级申请
     */
    ClassCreateApplication submitCreateClassRequest(String className, String description);

    /**
     * 获取创建班级申请列表（管理员专用）
     * @param status 状态筛选（0-待审核，1-已通过，2-已拒绝），可选
     * @return 按创建时间倒序排列的申请列表
     */
    List<ClassCreateApplication> getCreateApplications(Integer status);

    /**
     * 审核创建班级申请
     */
    void approveCreateApplication(Integer applicationId, Boolean approved, String comment);

    /**
     * 提交加入班级申请
     */
    ClassJoinApplication submitJoinClassRequest(Integer classId);

    /**
     * 获取加入班级申请列表（老师和管理员专用）
     * @param classId 班级 ID 筛选，可选
     * @param status 状态筛选（0-待审核，1-已通过，2-已拒绝），可选
     * @return 按创建时间倒序排列的申请列表
     */
    List<ClassJoinApplication> getJoinApplications(Integer classId, Integer status);

    /**
     * 审核加入班级申请
     */
    void approveJoinApplication(Integer applicationId, Boolean approved, String comment);

    /**
     * 教师邀请用户加入班级（需用户同意）
     */
    ClassInvitation inviteUserToClassWithApproval(Integer classId, String userAccount);

    /**
     * 获取用户收到的邀请列表
     */
    List<InvitationResponse> getMyInvitations(Integer userId, Integer status);

    /**
     * 用户响应邀请（同意/拒绝）
     */
    void respondInvitation(Integer invitationId, Boolean accepted, String comment);
}
