package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.work_management.entity.*;
import top.thexiaola.dreamhwhub.module.work_management.mapper.*;
import top.thexiaola.dreamhwhub.module.work_management.vo.*;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 班级加入服务
 * 负责加入申请与审核、邀请码、教师与学生邀请等入班流程
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassJoinService {

    private final ClassInfoMapper classInfoMapper;
    private final ClassAccessResolver classAccessResolver;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;
    private final ClassJoinApplicationMapper classJoinApplicationMapper;
    private final ClassUserInvitationMapper classUserInvitationMapper;
    private final ClassTeacherApprovalMapper classTeacherApprovalMapper;
    private final ClassInvitationMapper classInvitationMapper;
    private final SchoolService schoolService;
    private final UserLookupSupport userLookup;

    @Transactional(rollbackFor = Exception.class)
    public void studentInviteUser(Integer classId, String userAccount) {
        User currentUser = userLookup.requireCurrentUser();

        // 验证班级是否存在
        ClassInfo classEntity = classInfoMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }
        // 班级冻结（创建者教师身份被解除）期间不再接纳新成员
        classAccessResolver.requireClassActive(classEntity);

        // 检查当前用户是否是班级内的成员（学生或助理/老师均可邀请）
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_UPDATE);
        boolean isClassMember = classAccessResolver.isTeacher(classId, currentUser.getId())
                || classAccessResolver.isStudent(classId, currentUser.getId());

        if (!isAdmin && !isClassMember) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级内的成员才能提交邀请申请", null);
        }

        // 班级关闭学生邀请时，仅老师（创建者/助理）和管理员可发起邀请
        boolean isTeacherRole = isAdmin || classAccessResolver.isTeacher(classId, currentUser.getId());
        if (!isTeacherRole && !Boolean.TRUE.equals(classEntity.getAllowStudentInvite())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "该班级不允许学生邀请同学加入", null);
        }

        // 根据账号查询目标用户
        User targetUser = userLookup.findByAccountOrThrow(userAccount);

        // 检查目标用户是否已经是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", targetUser.getId());
        if (classMemberMapper.selectCount(memberQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该用户已经在班级中", null);
        }

        // 检查是否有待确认的邀请（status=0）
        QueryWrapper<ClassUserInvitation> pendingInvitationQuery = new QueryWrapper<>();
        pendingInvitationQuery.eq("class_id", classId)
                .eq("inviter_id", currentUser.getId())
                .eq("invitee_id", targetUser.getId())
                .eq("status", 0); // 待确认
        List<ClassUserInvitation> pendingInvitations = classUserInvitationMapper.selectList(pendingInvitationQuery);

        if (!pendingInvitations.isEmpty()) {
            // 删除所有待确认的用户邀请记录
            for (ClassUserInvitation invitation : pendingInvitations) {
                // 先删除关联的教师审核记录（如果存在）
                QueryWrapper<ClassTeacherApproval> approvalQuery = new QueryWrapper<>();
                approvalQuery.eq("invitation_id", invitation.getId());
                classTeacherApprovalMapper.delete(approvalQuery);

                // 再删除用户邀请记录
                classUserInvitationMapper.deleteById(invitation.getId());

            }
        }

        // 创建新的用户邀请记录（等待被邀请人确认）
        ClassUserInvitation invitation = new ClassUserInvitation();
        invitation.setClassId(classId);
        invitation.setInviterId(currentUser.getId());
        invitation.setInviteeId(targetUser.getId());
        invitation.setStatus(0); // 待用户确认
        invitation.setCreateTime(LocalDateTime.now());

        classUserInvitationMapper.insert(invitation);

    }

    @Transactional(rollbackFor = Exception.class)
    public void respondUserInvitation(Integer invitationId, Boolean accepted) {
        User currentUser = userLookup.requireCurrentUser();

        ClassUserInvitation invitation = classUserInvitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "邀请不存在", null);
        }

        // 只能响应发给自己的邀请
        if (!invitation.getInviteeId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能响应发给自己的邀请", null);
        }

        // 检查邀请状态
        if (!Integer.valueOf(0).equals(invitation.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该邀请已处理", null);
        }

        // 同意时要求已具备学校成员身份，姓名与学工号取自该身份
        if (Boolean.TRUE.equals(accepted)) {
            classAccessResolver.requireSchoolMember(classInfoMapper.selectById(invitation.getClassId()), currentUser.getId());
        }

        // 更新邀请状态
        invitation.setStatus(accepted ? 1 : 2); // 1-已同意，2-已拒绝
        invitation.setResponseTime(LocalDateTime.now());
        classUserInvitationMapper.updateById(invitation);

        // 如果同意，创建教师审核记录
        if (accepted) {
            ClassTeacherApproval approval = new ClassTeacherApproval();
            approval.setClassId(invitation.getClassId());
            approval.setInvitationId(invitation.getId());
            approval.setInviteeId(invitation.getInviteeId());
            approval.setStatus(0); // 待教师审核
            approval.setCreateTime(LocalDateTime.now());

            classTeacherApprovalMapper.insert(approval);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveTeacherApproval(Integer approvalId, Boolean approved, String comment) {
        User currentUser = userLookup.requireCurrentUser();

        ClassTeacherApproval approval = classTeacherApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "审核记录不存在", null);
        }

        if (!Integer.valueOf(0).equals(approval.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该申请已处理", null);
        }

        // 检查审核人是否是老师助理或拥有审批权限
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_APPROVE_JOIN);
        boolean isClassTeacher = classAccessResolver.isTeacher(approval.getClassId(), currentUser.getId());

        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或助理可以审核邀请申请", null);
        }

        // 更新审核状态
        approval.setStatus(approved ? 1 : 2); // 1-已通过，2-已拒绝
        approval.setReviewerId(currentUser.getId());
        approval.setReviewTime(LocalDateTime.now());
        approval.setReviewComment(comment);
        classTeacherApprovalMapper.updateById(approval);

        // 如果审核通过，添加为班级成员（学生）
        if (approved) {
            // 检查是否已经是成员（双重检查）
            QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
            memberQuery.eq("class_id", approval.getClassId())
                    .eq("user_id", approval.getInviteeId());
            if (classMemberMapper.selectCount(memberQuery) == 0) {
                // 姓名与学工号取自被邀请人的学校成员身份；学校老师与学校管理员自动获得班级管理员权限
                SchoolMember schoolMember = classAccessResolver.requireSchoolMember(classInfoMapper.selectById(approval.getClassId()),
                        approval.getInviteeId());

                // 邀请记录中携带邀请人 ID
                ClassUserInvitation invitation = classUserInvitationMapper.selectById(approval.getInvitationId());

                ClassMember member = new ClassMember();
                member.setClassId(approval.getClassId());
                member.setUserId(approval.getInviteeId());
                member.setRole(classAccessResolver.resolveClassRole(schoolMember));
                member.setJoinTime(LocalDateTime.now());

                if (invitation != null) {
                    member.setInviteBy(invitation.getInviterId());
                }

                classMemberMapper.insert(member);

            }
        }

    }

    public List<TeacherApprovalResponse> getPendingTeacherApprovals(Integer classId) {
        User currentUser = userLookup.requireCurrentUser();

        // 检查是否是班级内的老师或助理
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_APPROVE_JOIN);
        boolean isClassTeacher = classAccessResolver.isTeacher(classId, currentUser.getId());

        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或助理可以查看待审核邀请", null);
        }

        QueryWrapper<ClassTeacherApproval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId)
                .eq("status", 0) // 待审核
                .orderByDesc("create_time");
        List<ClassTeacherApproval> approvals = classTeacherApprovalMapper.selectList(queryWrapper);

        // 转换为 VO 列表
        return approvals.stream()
                .map(this::convertToTeacherApprovalResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public JoinClassApplicationResponse submitJoinClassRequest(Integer classId) {
        User currentUser = userLookup.requireCurrentUser();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }
        // 班级冻结（创建者教师身份被解除）期间不再接纳新成员
        classAccessResolver.requireClassActive(classInfo);

        // 姓名与学工号取自学校成员身份，入班前必须先加入班级所属学校
        classAccessResolver.requireSchoolMember(classInfo, currentUser.getId());

        // 检查是否已经是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", currentUser.getId());
        if (classMemberMapper.selectCount(memberQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "你已经在该班级中", null);
        }

        // 检查是否已经有待审核的申请
        QueryWrapper<ClassJoinApplication> appQuery = new QueryWrapper<>();
        appQuery.eq("class_id", classId)
                .eq("applicant_id", currentUser.getId())
                .eq("status", 0);
        if (classJoinApplicationMapper.selectCount(appQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "你已有待审核的申请", null);
        }

        // 创建申请记录
        ClassJoinApplication application = new ClassJoinApplication();
        application.setClassId(classId);
        application.setApplicantId(currentUser.getId());
        application.setStatus(0); // 待审核
        classJoinApplicationMapper.insert(application);

        return convertToJoinClassApplicationResponse(application);
    }

    public Page<JoinClassApplicationResponse> getJoinApplications(Integer classId, Integer status, Integer pageNum,
            Integer pageSize) {
        User currentUser = userLookup.requireCurrentUser();

        // 检查权限：拥有审批加入申请权限或班级老师
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_APPROVE_JOIN);

        if (!isAdmin && classId != null) {
            // 如果不是管理员，必须是该班级的老师
            if (!classAccessResolver.isTeacher(classId, currentUser.getId())) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员或班级老师可以查看加入申请", null);
            }
        } else if (!isAdmin) {
            // 非管理员且classId为空：查询自己担任老师的所有班级的申请
            QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
            teacherQuery.eq("user_id", currentUser.getId())
                    .eq("role", 1)
                    .select("class_id");
            List<ClassMember> teacherMembers = classMemberMapper.selectList(teacherQuery);

            if (teacherMembers.isEmpty()) {
                // 没有担任老师的班级，返回空分页结果
                Page<JoinClassApplicationResponse> emptyPage = new Page<>(pageNum, pageSize, 0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }

            // 构建classId列表
            List<Integer> classIds = teacherMembers.stream()
                    .map(ClassMember::getClassId)
                    .toList();

            // 查询这些班级的申请
            QueryWrapper<ClassJoinApplication> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("class_id", classIds);

            if (status != null) {
                queryWrapper.eq("status", status);
            }

            // 按创建时间倒序排列
            queryWrapper.orderByDesc("create_time");

            // 使用MyBatisPlus分页
            Page<ClassJoinApplication> appPage = new Page<>(pageNum, pageSize);
            Page<ClassJoinApplication> resultPage = classJoinApplicationMapper.selectPage(appPage, queryWrapper);

            // 转换为VO列表
            List<JoinClassApplicationResponse> voList = resultPage.getRecords().stream()
                    .map(this::convertToJoinClassApplicationResponse)
                    .toList();

            // 构建新的分页结果
            Page<JoinClassApplicationResponse> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
            voPage.setRecords(voList);
            return voPage;
        }

        QueryWrapper<ClassJoinApplication> queryWrapper = new QueryWrapper<>();

        if (classId != null) {
            queryWrapper.eq("class_id", classId);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc("create_time");

        // 使用MyBatisPlus分页
        Page<ClassJoinApplication> appPage = new Page<>(pageNum, pageSize);
        Page<ClassJoinApplication> resultPage = classJoinApplicationMapper.selectPage(appPage, queryWrapper);

        // 转换为VO列表
        List<JoinClassApplicationResponse> voList = resultPage.getRecords().stream()
                .map(this::convertToJoinClassApplicationResponse)
                .toList();

        // 构建新的分页结果
        Page<JoinClassApplicationResponse> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 将 ClassJoinApplication 实体转换为 JoinClassApplicationResponse VO
     */
    private JoinClassApplicationResponse convertToJoinClassApplicationResponse(ClassJoinApplication application) {
        ClassInfo classInfo = classInfoMapper.selectById(application.getClassId());
        String className = classInfo != null ? classInfo.getClassName() : "未知";

        // 申请人姓名与学工号取自其学校成员身份
        String applicantName = null;
        String applicantNo = null;
        if (classInfo != null && classInfo.getSchoolId() != null) {
            SchoolMember schoolMember = schoolService.getMember(classInfo.getSchoolId(), application.getApplicantId());
            if (schoolMember != null) {
                applicantName = schoolMember.getRealName();
                applicantNo = schoolMember.getStaffNo();
            }
        }

        return new JoinClassApplicationResponse(
                application.getId(),
                application.getClassId(),
                application.getApplicantId(),
                application.getStatus(),
                application.getCreateTime(),
                className,
                applicantName,
                applicantNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveJoinApplication(Integer applicationId, Boolean approved, String comment) {
        User currentUser = userLookup.requireCurrentUser();

        ClassJoinApplication application = classJoinApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "申请不存在", null);
        }

        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该申请已处理", null);
        }

        // 检查权限：拥有审批加入申请权限或班级老师
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_APPROVE_JOIN);
        boolean isClassTeacher = classAccessResolver.isTeacher(application.getClassId(), currentUser.getId());

        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员或班级老师可以审核加入申请", null);
        }

        // 更新申请状态
        application.setStatus(approved ? 1 : 2);
        application.setReviewerId(currentUser.getId());
        application.setReviewTime(LocalDateTime.now());
        application.setReviewComment(comment);
        classJoinApplicationMapper.updateById(application);

        // 如果审核通过，添加成员（学生），姓名与学工号取自其学校成员身份
        if (approved) {
            // 学校老师与学校管理员进入班级后自动获得班级管理员权限
            SchoolMember schoolMember = classAccessResolver.requireSchoolMember(classInfoMapper.selectById(application.getClassId()),
                    application.getApplicantId());

            ClassMember member = new ClassMember();
            member.setClassId(application.getClassId());
            member.setUserId(application.getApplicantId());
            member.setRole(classAccessResolver.resolveClassRole(schoolMember));
            member.setJoinTime(LocalDateTime.now());
            classMemberMapper.insert(member);

        }

    }

    @Transactional(rollbackFor = Exception.class)
    public InvitationResponse inviteUserToClassWithApproval(Integer classId, String userAccount) {
        User currentUser = userLookup.requireCurrentUser();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }
        // 班级冻结期间不再接纳新成员（老师邀请同样受限）
        classAccessResolver.requireClassActive(classInfo);

        // 检查当前用户是否有权限邀请（必须是老师或有添加班级老师权限）
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_TEACHER_ADD);
        boolean isClassTeacher = classAccessResolver.isTeacher(classId, currentUser.getId());

        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师或管理员可以邀请用户加入班级", null);
        }

        // 根据账号查询目标用户
        User targetUser = userLookup.findByAccountOrThrow(userAccount);

        // 检查目标用户是否已经是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", targetUser.getId());
        if (classMemberMapper.selectCount(memberQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该用户已经在班级中", null);
        }

        // 删除已有的待处理邀请记录（如果存在）
        QueryWrapper<ClassInvitation> oldInvitationQuery = new QueryWrapper<>();
        oldInvitationQuery.eq("class_id", classId)
                .eq("invitee_user_id", targetUser.getId())
                .eq("status", 0); // 待处理
        classInvitationMapper.delete(oldInvitationQuery);

        // 创建新的邀请记录
        ClassInvitation invitation = new ClassInvitation();
        invitation.setClassId(classId);
        invitation.setInviterId(currentUser.getId());
        invitation.setInviteeUserId(targetUser.getId());
        invitation.setStatus(0); // 待处理

        classInvitationMapper.insert(invitation);

        // 转换为VO返回
        User inviter = userMapper.selectById(currentUser.getId());
        String inviterName = inviter != null ? inviter.getUsername() : "未知";

        User invitee = userMapper.selectById(targetUser.getId());
        String inviteeName = invitee != null ? invitee.getUsername() : "未知";

        // ClassInfo classInfo already defined above
        String className = classInfo != null ? classInfo.getClassName() : "未知";

        return new InvitationResponse(
                invitation.getId(),
                invitation.getClassId(),
                className,
                invitation.getInviterId(),
                inviterName,
                invitation.getInviteeUserId(),
                invitation.getStatus(),
                invitation.getResponseTime(),
                invitation.getCreateTime());
    }

    public List<InvitationResponse> getMyInvitations(Integer userId, Integer status) {
        User currentUser = userLookup.requireCurrentUser();

        // 只能查看自己的邀请
        if (!userId.equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能查看自己的邀请", null);
        }

        QueryWrapper<ClassInvitation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("invitee_user_id", userId);

        if (status != null) {
            queryWrapper.eq("status", status);
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc("create_time");

        List<ClassInvitation> invitations = classInvitationMapper.selectList(queryWrapper);

        // 转换为响应对象
        return invitations.stream().map(invitation -> {
            InvitationResponse response = new InvitationResponse();
            response.setId(invitation.getId());
            response.setClassId(invitation.getClassId());
            response.setInviterId(invitation.getInviterId());
            response.setInviteeUserId(invitation.getInviteeUserId());
            response.setStatus(invitation.getStatus());
            response.setResponseTime(invitation.getResponseTime());
            response.setCreateTime(invitation.getCreateTime());

            // 获取班级名称
            ClassInfo classInfo = classInfoMapper.selectById(invitation.getClassId());
            if (classInfo != null) {
                response.setClassName(classInfo.getClassName());
            }

            // 获取邀请人姓名
            User inviter = userMapper.selectById(invitation.getInviterId());
            if (inviter != null) {
                response.setInviterName(inviter.getUsername());
            }

            return response;
        }).collect(Collectors.toList());
    }

    public List<InvitationResponse> getMyUserInvitations(Integer userId) {
        User currentUser = userLookup.requireCurrentUser();

        // 只能查看发给自己的学生邀请
        if (!userId.equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能查看自己的邀请", null);
        }

        // 待当前用户确认的学生邀请
        QueryWrapper<ClassUserInvitation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("invitee_id", userId)
                .eq("status", 0)
                .orderByDesc("create_time");
        List<ClassUserInvitation> invitations = classUserInvitationMapper.selectList(queryWrapper);

        return invitations.stream().map(invitation -> {
            InvitationResponse response = new InvitationResponse();
            response.setId(invitation.getId());
            response.setClassId(invitation.getClassId());
            response.setInviterId(invitation.getInviterId());
            response.setInviteeUserId(invitation.getInviteeId());
            response.setStatus(invitation.getStatus());
            response.setCreateTime(invitation.getCreateTime());

            // 获取班级名称
            ClassInfo classInfo = classInfoMapper.selectById(invitation.getClassId());
            if (classInfo != null) {
                response.setClassName(classInfo.getClassName());
            }

            // 获取邀请人姓名
            User inviter = userMapper.selectById(invitation.getInviterId());
            if (inviter != null) {
                response.setInviterName(inviter.getUsername());
            }

            return response;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void respondInvitation(Integer invitationId, Boolean accepted) {
        User currentUser = userLookup.requireCurrentUser();

        ClassInvitation invitation = classInvitationMapper.selectById(invitationId);
        if (invitation == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "邀请不存在", null);
        }

        // 只能响应发给自己的邀请
        if (!invitation.getInviteeUserId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能响应发给自己的邀请", null);
        }

        // 检查邀请状态
        if (!Integer.valueOf(0).equals(invitation.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该邀请已处理", null);
        }

        // 更新邀请状态
        invitation.setStatus(accepted ? 1 : 2); // 1-已同意，2-已拒绝
        invitation.setResponseTime(LocalDateTime.now());
        classInvitationMapper.updateById(invitation);

        // 如果同意，添加为班级成员（学生）
        if (accepted) {
            // 学校老师与学校管理员进入班级后自动获得班级管理员权限
            SchoolMember schoolMember = classAccessResolver.requireSchoolMember(classInfoMapper.selectById(invitation.getClassId()),
                    currentUser.getId());

            // 再次检查是否已经是成员
            QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
            memberQuery.eq("class_id", invitation.getClassId()).eq("user_id", currentUser.getId());
            if (classMemberMapper.selectCount(memberQuery) == 0) {
                ClassMember member = new ClassMember();
                member.setClassId(invitation.getClassId());
                member.setUserId(currentUser.getId());
                member.setRole(classAccessResolver.resolveClassRole(schoolMember));
                member.setJoinTime(LocalDateTime.now());
                member.setInviteBy(invitation.getInviterId());
                classMemberMapper.insert(member);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public String getInviteCode(Integer classId) {
        User currentUser = userLookup.requireCurrentUser();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }
        // 班级冻结期间邀请码失效，不再对外提供
        classAccessResolver.requireClassActive(classInfo);

        boolean isAdminUser = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_UPDATE);
        boolean isTeacherUser = classAccessResolver.isTeacher(classId, currentUser.getId());
        if (!isAdminUser && !isTeacherUser) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师可以查看邀请码", null);
        }

        // 已存在邀请码则直接返回，不刷新
        if (StrUtil.isNotBlank(classInfo.getInviteCode())) {
            return classInfo.getInviteCode();
        }

        // 不存在则生成并保存
        String newCode = generateRandomCode(25);
        classInfo.setInviteCode(newCode);
        classInfoMapper.updateById(classInfo);

        return newCode;
    }

    @Transactional(rollbackFor = Exception.class)
    public String resetInviteCode(Integer classId) {
        User currentUser = userLookup.requireCurrentUser();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }
        // 班级冻结期间不允许重置邀请码
        classAccessResolver.requireClassActive(classInfo);

        boolean isAdminUser = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_UPDATE);
        boolean isTeacherUser = classAccessResolver.isTeacher(classId, currentUser.getId());
        if (!isAdminUser && !isTeacherUser) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师可以重置邀请码", null);
        }

        // 生成新码覆盖旧码，旧码立即失效
        String newCode = generateRandomCode(25);
        classInfo.setInviteCode(newCode);
        classInfoMapper.updateById(classInfo);

        return newCode;
    }

    @Transactional(rollbackFor = Exception.class)
    public JoinClassApplicationResponse joinClassByInviteCode(String inviteCode, Integer schoolId) {
        User currentUser = userLookup.requireCurrentUser();

        if (StrUtil.isBlank(inviteCode)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "邀请码不能为空", null);
        }

        // 根据邀请码查找班级
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.eq("invite_code", StrUtil.trim(inviteCode));
        ClassInfo classInfo = classInfoMapper.selectOne(classQuery);

        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "邀请码失效", null);
        }

        // 班级冻结期间邀请码失效，不再可作为入班凭证
        classAccessResolver.requireClassActive(classInfo);

        // 只能加入当前所在学校的班级，避免跨校入班
        if (!Objects.equals(classInfo.getSchoolId(), schoolId)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "该邀请码不属于当前学校，请先切换到对应学校", null);
        }

        // 姓名与学工号取自学校成员身份，入班前必须先加入班级所属学校
        SchoolMember schoolMember = classAccessResolver.requireSchoolMember(classInfo, currentUser.getId());

        // 检查是否已经是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classInfo.getId()).eq("user_id", currentUser.getId());
        if (classMemberMapper.selectCount(memberQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_MEMBER, "您已经是该班级成员", null);
        }

        // 通过邀请码直接加入班级，无需审核
        ClassMember member = new ClassMember();
        member.setClassId(classInfo.getId());
        member.setUserId(currentUser.getId());
        member.setRole(classAccessResolver.resolveClassRole(schoolMember));
        member.setJoinTime(LocalDateTime.now());
        classMemberMapper.insert(member);

        // 为了保持接口一致性，返回一个“已通过”的申请记录（虚拟）
        ClassJoinApplication application = new ClassJoinApplication();
        application.setClassId(classInfo.getId());
        application.setApplicantId(currentUser.getId());
        application.setStatus(1); // 直接设置为已通过
        application.setReviewerId(currentUser.getId()); // 自动审核
        application.setReviewTime(LocalDateTime.now());
        application.setCreateTime(LocalDateTime.now());

        // 转换为VO返回
        return new JoinClassApplicationResponse(
                application.getId(),
                application.getClassId(),
                application.getApplicantId(),
                application.getStatus(),
                application.getCreateTime(),
                classInfo.getClassName(),
                schoolMember.getRealName(),
                schoolMember.getStaffNo());
    }

    /**
     * 生成指定长度的随机码（大小写字母+数字）
     */
    private String generateRandomCode(int length) {
        return RandomUtil.randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", length);
    }

    /**
     * 将 ClassTeacherApproval 实体转换为 TeacherApprovalResponse VO
     */
    private TeacherApprovalResponse convertToTeacherApprovalResponse(ClassTeacherApproval approval) {
        TeacherApprovalResponse response = new TeacherApprovalResponse();
        response.setId(approval.getId());
        response.setClassId(approval.getClassId());
        response.setInvitationId(approval.getInvitationId());
        response.setInviteeId(approval.getInviteeId());
        response.setStatus(approval.getStatus());
        response.setReviewerId(approval.getReviewerId());
        response.setReviewTime(approval.getReviewTime());
        response.setReviewComment(approval.getReviewComment());
        response.setCreateTime(approval.getCreateTime());

        // 查询班级名称
        if (approval.getClassId() != null) {
            ClassInfo classInfo = classInfoMapper.selectById(approval.getClassId());
            if (classInfo != null) {
                response.setClassName(classInfo.getClassName());
            }
        }

        // 查询被邀请人用户名
        if (approval.getInviteeId() != null) {
            User invitee = userMapper.selectById(approval.getInviteeId());
            if (invitee != null) {
                response.setInviteeUsername(invitee.getUsername());
            }
        }

        // 查询审核人用户名
        if (approval.getReviewerId() != null) {
            User reviewer = userMapper.selectById(approval.getReviewerId());
            if (reviewer != null) {
                response.setReviewerUsername(reviewer.getUsername());
            }
        }

        return response;
    }
}
