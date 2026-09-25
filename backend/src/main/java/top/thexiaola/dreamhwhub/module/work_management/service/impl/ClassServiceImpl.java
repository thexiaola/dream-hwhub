package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.work_management.entity.*;
import top.thexiaola.dreamhwhub.module.work_management.mapper.*;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.vo.*;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 班级管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;
    private final ClassUserInvitationMapper classUserInvitationMapper;
    private final ClassTeacherApprovalMapper classTeacherApprovalMapper;
    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper;
    private final WorkMapper workMapper;
    private final WorkAttachmentMapper workAttachmentMapper;
    private final SchoolService schoolService;
    private final UserLookupSupport userLookup;
    private final ClassAccessResolver classAccessResolver;
    private final ClassQueryService classQueryService;
    private final ClassMembershipService classMembershipService;
    private final ClassJoinService classJoinService;
    private final ClassTakeoverService classTakeoverService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveClass(Integer classId, String confirmText) {
        User currentUser = userLookup.requireCurrentUser();

        ClassInfo classEntity = classInfoMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 创建者或拥有解散班级权限者可以解散班级
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_DISSOLVE);
        boolean isOwner = classEntity.getOwnerId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有创建者或管理员可以解散班级", null);
        }

        // 身份二次验证（登录密码或邮箱验证码）已由 SensitiveVerificationInterceptor 统一完成，
        // 此处仅校验确认文案。

        // 确认文案校验：我已确认要删除{className}课堂
        String className = classEntity.getClassName();
        String expected = "我已确认要删除" + (className == null ? "" : className) + "课堂";
        if (StrUtil.isBlank(confirmText) || !confirmText.equals(expected)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "确认文案不匹配，请完整输入：" + expected, null);
        }

        // 1. 硬删除该班级下的所有作业提交记录和附件
        hardDeleteAllSubmissionsInClass(classId);

        // 2. 硬删除该班级下的所有作业附件记录
        QueryWrapper<WorkInfo> workQuery = new QueryWrapper<>();
        workQuery.eq("class_id", classId);
        List<WorkInfo> works = workMapper.selectList(workQuery);

        if (!works.isEmpty()) {
            List<Integer> workIds = works.stream().map(WorkInfo::getId).toList();

            // 删除作业附件
            QueryWrapper<WorkAttachment> attQuery = new QueryWrapper<>();
            attQuery.in("work_id", workIds);
            int attachmentCount = workAttachmentMapper.delete(attQuery);

            // 删除作业信息
            int workCount = workMapper.delete(workQuery);
        }

        // 3. 删除班级成员记录
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId);
        int memberCount = classMemberMapper.delete(memberQuery);

        // 4. 删除班级用户邀请记录
        QueryWrapper<ClassUserInvitation> userInvitationQuery = new QueryWrapper<>();
        userInvitationQuery.eq("class_id", classId);
        int userInvitationCount = classUserInvitationMapper.delete(userInvitationQuery);

        // 5. 删除教师审核邀请记录
        QueryWrapper<ClassTeacherApproval> teacherApprovalQuery = new QueryWrapper<>();
        teacherApprovalQuery.eq("class_id", classId);
        int teacherApprovalCount = classTeacherApprovalMapper.delete(teacherApprovalQuery);

        // 6. 最后删除班级信息
        classInfoMapper.deleteById(classId);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassDetailResponse updateClassInfo(Integer classId, String className, String description) {
        // 1. 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 2. 查询班级信息
        ClassInfo classEntity = classInfoMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 3. 验证权限（只有老师或班级助理可以修改）
        boolean isTeacher = classAccessResolver.isTeacher(classId, currentUser.getId());
        if (!isTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或助理可以修改班级信息", null);
        }

        // 4. 更新班级信息
        classEntity.setClassName(className);
        classEntity.setDescription(description);
        classEntity.setUpdateTime(LocalDateTime.now());

        int updated = classInfoMapper.updateById(classEntity);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, "更新班级信息失败", null);
        }

        // 5. 转换为VO返回
        User owner = userMapper.selectById(classEntity.getOwnerId());
        String ownerName = owner != null ? owner.getUsername() : "未知";

        // 查询成员统计
        QueryWrapper<ClassMember> countQuery = new QueryWrapper<>();
        countQuery.eq("class_id", classEntity.getId());
        long memberCount = classMemberMapper.selectCount(countQuery);

        QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("class_id", classEntity.getId()).eq("role", 1);
        long teacherCount = classMemberMapper.selectCount(teacherQuery);

        QueryWrapper<ClassMember> studentQuery = new QueryWrapper<>();
        studentQuery.eq("class_id", classEntity.getId()).eq("role", 0);
        long studentCount = classMemberMapper.selectCount(studentQuery);

        // 查询当前用户在该班级的角色
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classEntity.getId()).eq("user_id", currentUser.getId());
        ClassMember member = classMemberMapper.selectOne(memberQuery);
        String userRole = classAccessResolver.getUserRole(classEntity, member);

        String schoolName = classAccessResolver.resolveSchoolName(classEntity.getSchoolId());

        return new ClassDetailResponse(
                classEntity.getId(),
                classEntity.getClassName(),
                classEntity.getSchoolId(),
                schoolName,
                classEntity.getOwnerId(),
                ownerName,
                userRole,
                classAccessResolver.getUserRoleCode(classEntity, member),
                memberCount,
                teacherCount,
                studentCount,
                classEntity.getDescription(),
                classEntity.getAllowStudentInvite(),
                classEntity.getCreateTime());
    }

    @Override
    public void setStudentInviteAllowed(Integer classId, Boolean allowStudentInvite) {
        User currentUser = userLookup.requireCurrentUser();

        ClassInfo classEntity = classInfoMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 只有老师（创建者/助理）可以修改学生邀请设置
        if (!classAccessResolver.isTeacher(classId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或助理可以修改邀请设置", null);
        }

        classEntity.setAllowStudentInvite(allowStudentInvite);
        classEntity.setUpdateTime(LocalDateTime.now());
        int updated = classInfoMapper.updateById(classEntity);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, "更新邀请设置失败", null);
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassInfo createClass(Integer schoolId, String className, String description) {
        User currentUser = userLookup.requireCurrentUser();

        // 班级必须归属学校。默认只有该学校的老师（含学校管理员）才能创建；
        // 拥有 class:create 权限者（平台管理员 OP 恒有）可在任意学校下创建。
        schoolService.requireSchoolExists(schoolId);
        boolean canCreateAnywhere = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_CREATE);
        if (!canCreateAnywhere && !schoolService.isSchoolTeacher(schoolId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.NOT_SCHOOL_TEACHER, "只有学校老师才能创建班级", null);
        }

        ClassInfo classInfo = new ClassInfo();
        classInfo.setSchoolId(schoolId);
        classInfo.setClassName(className);
        classInfo.setDescription(description);
        classInfo.setOwnerId(currentUser.getId());
        classInfoMapper.insert(classInfo);

        // 创建者自动成为班级老师
        ClassMember member = new ClassMember();
        member.setClassId(classInfo.getId());
        member.setUserId(currentUser.getId());
        member.setRole(1);
        member.setJoinTime(LocalDateTime.now());
        classMemberMapper.insert(member);

        return classInfo;
    }


    /**
     * 硬删除班级下的所有作业提交记录和附件
     * 
     * @param classId 班级 ID
     */
    private void hardDeleteAllSubmissionsInClass(Integer classId) {
        // 1. 查询该班级下所有作业的 ID
        QueryWrapper<WorkInfo> workQuery = new QueryWrapper<>();
        workQuery.eq("class_id", classId);
        List<WorkInfo> works = workMapper.selectList(workQuery);

        if (works.isEmpty()) {
            return;
        }

        List<Integer> workIds = works.stream()
                .map(WorkInfo::getId)
                .toList();

        // 2. 查询所有提交记录
        QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.in("work_id", workIds);
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(submissionQuery);

        if (submissions.isEmpty()) {
            return;
        }

        List<Integer> submissionIds = submissions.stream()
                .map(WorkSubmission::getId)
                .toList();

        // 3. 硬删除所有提交附件记录
        QueryWrapper<WorkSubmissionAttachment> attQuery = new QueryWrapper<>();
        attQuery.in("submission_id", submissionIds);
        int attachmentCount = workSubmissionAttachmentMapper.delete(attQuery);

        // 4. 硬删除所有提交记录
        int submissionCount = workSubmissionMapper.delete(submissionQuery);
    }


    // ===== 以下为拆出的协作组件的委托实现 =====

    @Override
    public boolean isOrdinaryTeacher(Integer classId, Integer userId) {
        return classAccessResolver.isOrdinaryTeacher(classId, userId);
    }

    @Override
    public boolean isTeacher(Integer classId, Integer userId) {
        return classAccessResolver.isTeacher(classId, userId);
    }

    @Override
    public boolean isStudent(Integer classId, Integer userId) {
        return classAccessResolver.isStudent(classId, userId);
    }

    @Override
    public boolean canSubmitWork(Integer classId, Integer userId) {
        return classAccessResolver.canSubmitWork(classId, userId);
    }

    @Override
    public boolean isClassMember(Integer classId, Integer userId) {
        return classAccessResolver.isClassMember(classId, userId);
    }

    @Override
    public Integer getUserRoleCodeInClass(Integer classId, Integer userId) {
        return classAccessResolver.getUserRoleCodeInClass(classId, userId);
    }

    @Override
    public String getUserRoleNameInClass(Integer classId, Integer userId) {
        return classAccessResolver.getUserRoleNameInClass(classId, userId);
    }

    @Override
    public List<Integer> getTeacherClassIds(Integer userId) {
        return classQueryService.getTeacherClassIds(userId);
    }

    @Override
    public List<Integer> getMemberClassIds(Integer userId) {
        return classQueryService.getMemberClassIds(userId);
    }

    @Override
    public List<ClassInfo> getClassByIds(List<Integer> classIds) {
        return classQueryService.getClassByIds(classIds);
    }

    @Override
    public ClassDetailResponse getClassDetail(Integer classId) {
        return classQueryService.getClassDetail(classId);
    }

    @Override
    public Page<ClassDetailResponse> getMyClasses(Integer userId, Integer pageNum, Integer pageSize,
            Integer schoolId, Integer roleCode, Boolean excludeOwner) {
        return classQueryService.getMyClasses(userId, pageNum, pageSize, schoolId, roleCode, excludeOwner);
    }

    @Override
    public Page<ClassDetailResponse> getAdminManageClasses(Integer userId, Integer pageNum, Integer pageSize, String keyword,
            Integer schoolId) {
        return classQueryService.getAdminManageClasses(userId, pageNum, pageSize, keyword, schoolId);
    }

    @Override
    public Page<ClassMemberResponse> getClassMembers(Integer classId, Integer pageNum, Integer pageSize) {
        return classQueryService.getClassMembers(classId, pageNum, pageSize);
    }

    @Override
    public List<ClassMemberResponse> getAllClassMembers(Integer classId) {
        return classQueryService.getAllClassMembers(classId);
    }

    @Override
    public void batchSetAssistantTeachers(Integer classId, List<Integer> studentUserIds) {
        classMembershipService.batchSetAssistantTeachers(classId, studentUserIds);
    }

    @Override
    public void batchKickStudentsFromClass(Integer classId, List<Integer> studentUserIds) {
        classMembershipService.batchKickStudentsFromClass(classId, studentUserIds);
    }

    @Override
    public void demoteAssistantTeacher(Integer classId, Integer teacherUserId) {
        classMembershipService.demoteAssistantTeacher(classId, teacherUserId);
    }

    @Override
    public String leaveClass(Integer classId) {
        return classMembershipService.leaveClass(classId);
    }

    @Override
    public void transferClassOwnership(Integer classId, Integer newOwnerId) {
        classMembershipService.transferClassOwnership(classId, newOwnerId);
    }

    @Override
    public void studentInviteUser(Integer classId, String userAccount) {
        classJoinService.studentInviteUser(classId, userAccount);
    }

    @Override
    public void respondUserInvitation(Integer invitationId, Boolean accepted) {
        classJoinService.respondUserInvitation(invitationId, accepted);
    }

    @Override
    public void approveTeacherApproval(Integer approvalId, Boolean approved, String comment) {
        classJoinService.approveTeacherApproval(approvalId, approved, comment);
    }

    @Override
    public List<TeacherApprovalResponse> getPendingTeacherApprovals(Integer classId) {
        return classJoinService.getPendingTeacherApprovals(classId);
    }

    @Override
    public JoinClassApplicationResponse submitJoinClassRequest(Integer classId) {
        return classJoinService.submitJoinClassRequest(classId);
    }

    @Override
    public Page<JoinClassApplicationResponse> getJoinApplications(Integer classId, Integer status, Integer pageNum, Integer pageSize) {
        return classJoinService.getJoinApplications(classId, status, pageNum, pageSize);
    }

    @Override
    public void approveJoinApplication(Integer applicationId, Boolean approved, String comment) {
        classJoinService.approveJoinApplication(applicationId, approved, comment);
    }

    @Override
    public InvitationResponse inviteUserToClassWithApproval(Integer classId, String userAccount) {
        return classJoinService.inviteUserToClassWithApproval(classId, userAccount);
    }

    @Override
    public List<InvitationResponse> getMyInvitations(Integer userId, Integer status) {
        return classJoinService.getMyInvitations(userId, status);
    }

    @Override
    public List<InvitationResponse> getMyUserInvitations(Integer userId) {
        return classJoinService.getMyUserInvitations(userId);
    }

    @Override
    public void respondInvitation(Integer invitationId, Boolean accepted) {
        classJoinService.respondInvitation(invitationId, accepted);
    }

    @Override
    public String getInviteCode(Integer classId) {
        return classJoinService.getInviteCode(classId);
    }

    @Override
    public String resetInviteCode(Integer classId) {
        return classJoinService.resetInviteCode(classId);
    }

    @Override
    public JoinClassApplicationResponse joinClassByInviteCode(String inviteCode, Integer schoolId) {
        return classJoinService.joinClassByInviteCode(inviteCode, schoolId);
    }

    @Override
    public ClassTakeoverResponse applyClassTakeover(Integer classId) {
        return classTakeoverService.applyTakeover(classId);
    }

    @Override
    public ClassTakeoverResponse getMyClassTakeover(Integer classId) {
        return classTakeoverService.getMyTakeover(classId);
    }

    @Override
    public List<ClassTakeoverResponse> listSchoolClassTakeovers(Integer schoolId, Integer status) {
        return classTakeoverService.listSchoolTakeovers(schoolId, status);
    }

    @Override
    public List<ClassTakeoverResponse> listAvailableClassTakeovers() {
        return classTakeoverService.listAvailableTakeovers();
    }

    @Override
    public void reviewClassTakeover(Integer applicationId, Boolean approved, String comment) {
        classTakeoverService.reviewTakeover(applicationId, approved, comment);
    }

    @Override
    public boolean isClassFrozen(Integer classId) {
        return classAccessResolver.isClassFrozen(classInfoMapper.selectById(classId));
    }
}
