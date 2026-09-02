package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import cn.hutool.core.util.RandomUtil;
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
import top.thexiaola.dreamhwhub.module.work_management.entity.*;
import top.thexiaola.dreamhwhub.module.work_management.mapper.*;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.vo.*;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ClassCreateApplicationMapper classCreateApplicationMapper;
    private final ClassJoinApplicationMapper classJoinApplicationMapper;
    private final ClassUserInvitationMapper classUserInvitationMapper;
    private final ClassTeacherApprovalMapper classTeacherApprovalMapper;
    private final ClassInvitationMapper classInvitationMapper;  // 保留用于教师邀请功能
    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper;
    private final WorkMapper workMapper;
    private final WorkAttachmentMapper workAttachmentMapper;
    private final top.thexiaola.dreamhwhub.support.password.PasswordUtil passwordUtil;

    /**
     * 获取当前登录用户，如果未登录则抛出异常
     * @return 当前用户对象
     */
    private User getCurrentUserOrThrow() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }
        return currentUser;
    }

    /**
     * 根据用户名或邮箱查询用户，如果不存在则抛出异常
     * @param userAccount 用户名或邮箱（学号不作为账号，允许重复）
     * @return 用户对象
     */
    private User getUserByAccountOrThrow(String userAccount) {
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.and(q -> q.apply("BINARY username = BINARY {0}", userAccount).or().eq("email", userAccount));
        User targetUser = userMapper.selectOne(userQuery);
        
        if (targetUser == null) {
            // 目标账号不存在属于业务失败（而非当前登录用户认证失效），返回 400 避免误触发前端登出
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "邀请的用户不存在，请确认用户名或邮箱是否正确", null);
        }
        return targetUser;
    }

    /**
     * 判断用户是否为管理员
     * @param user 用户对象
     * @return true-是管理员，false-不是管理员
     */
    private boolean isAdmin(User user) {
        return user != null && user.getPermission() != null && user.getPermission() >= 100;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassMember addTeacherToClass(Integer classId, String userAccount) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否有权限添加老师（需要是老师或管理员）
        boolean isAdmin = isAdmin(currentUser);
        boolean isTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师或管理员可以添加其他老师到班级", null);
        }

        // 根据账号查询目标用户
        User targetUser = getUserByAccountOrThrow(userAccount);

        // 检查目标用户是否已经是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", targetUser.getId());
        ClassMember existingMember = classMemberMapper.selectOne(memberQuery);
        
        if (existingMember != null) {
            // 如果已经是成员，更新为老师
            existingMember.setRole(1);
            classMemberMapper.updateById(existingMember);
            return existingMember;
        }

        ClassMember member = new ClassMember();
        member.setClassId(classId);
        member.setUserId(targetUser.getId());
        member.setRole(1);  // 设置为老师
        member.setJoinTime(LocalDateTime.now());
        member.setInviteBy(currentUser.getId());

        classMemberMapper.insert(member);

        return member;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetAssistantTeachers(Integer classId, List<Integer> studentUserIds) {
        User currentUser = getCurrentUserOrThrow();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        boolean isAdmin = isAdmin(currentUser);
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());

        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师可以设置班级助理", null);
        }

        boolean isOrdinaryTeacher = isOrdinaryTeacher(classId, currentUser.getId());
        if (isOrdinaryTeacher && !isAdmin) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "班级助理不能设置其他学生为班级助理", null);
        }

        int successCount = 0;
        for (Integer studentUserId : studentUserIds) {
            if (studentUserId.equals(currentUser.getId())) {
                continue;
            }
            QueryWrapper<ClassMember> studentQuery = new QueryWrapper<>();
            studentQuery.eq("class_id", classId).eq("user_id", studentUserId).eq("role", 0);
            ClassMember studentMember = classMemberMapper.selectOne(studentQuery);
            if (studentMember == null) {
                continue;
            }
            studentMember.setRole(1);
            classMemberMapper.updateById(studentMember);
            successCount++;
        }

        if (successCount == 0) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "没有符合条件的学员可以被设置为助理", null);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchKickStudentsFromClass(Integer classId, List<Integer> studentUserIds) {
        User currentUser = getCurrentUserOrThrow();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        boolean isAdmin = isAdmin(currentUser);
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());

        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或管理员可以踢出学生", null);
        }

        int kickedCount = 0;
        boolean isOrdinaryTeacher = isOrdinaryTeacher(classId, currentUser.getId());
        for (Integer studentUserId : studentUserIds) {
            if (studentUserId.equals(currentUser.getId())) {
                continue;
            }

            // 不能踢出班级创建者
            if (classInfo.getOwnerId().equals(studentUserId)) {
                continue;
            }

            QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
            memberQuery.eq("class_id", classId).eq("user_id", studentUserId);
            ClassMember member = classMemberMapper.selectOne(memberQuery);
            if (member == null) {
                continue;
            }

            // 普通助理不能踢出其他助理
            if (isOrdinaryTeacher && !isAdmin && member.getRole() == 1) {
                continue;
            }

            cleanupStudentSubmissions(classId, studentUserId);
            classMemberMapper.deleteById(member.getId());
            kickedCount++;
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void demoteAssistantTeacher(Integer classId, Integer teacherUserId) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是创建者或管理员
        boolean isAdmin = isAdmin(currentUser);
        boolean isCreator = classInfo.getOwnerId().equals(currentUser.getId());
        
        if (!isAdmin && !isCreator) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级创建者或管理员可以取消班级助理权限", null);
        }

        // 不能操作自己
        if (teacherUserId.equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能操作自己", null);
        }

        // 检查目标用户是否是老师（包括班级助理）
        QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("class_id", classId).eq("user_id", teacherUserId).eq("role", 1);
        ClassMember teacherMember = classMemberMapper.selectOne(teacherQuery);
        
        if (teacherMember == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "该用户不是班级老师或不在该班级中", null);
        }

        // 不能取消创建者的权限（虽然创建者不会是班级助理，但为了安全还是检查一下）
        if (classInfo.getOwnerId().equals(teacherUserId)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能取消创建者的权限", null);
        }

        // 降级为学生
        teacherMember.setRole(0);
        classMemberMapper.updateById(teacherMember);

    }

    @Override
    public boolean isOrdinaryTeacher(Integer classId, Integer userId) {
        // 检查是否是班级成员且是老师
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("role", 1);
        ClassMember member = classMemberMapper.selectOne(queryWrapper);
        
        if (member == null) {
            return false;
        }

        // 检查是否是创建者
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        return classInfo == null || !classInfo.getOwnerId().equals(userId);  // 创建者不是普通老师
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void studentInviteUser(Integer classId, String userAccount) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是班级内的成员（学生或助理/老师均可邀请）
        boolean isAdmin = isAdmin(currentUser);
        boolean isClassMember = isTeacher(classId, currentUser.getId())
                || isStudent(classId, currentUser.getId());
        
        if (!isAdmin && !isClassMember) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级内的成员才能提交邀请申请", null);
        }

        // 班级关闭学生邀请时，仅老师（创建者/助理）和管理员可发起邀请
        boolean isTeacherRole = isAdmin || isTeacher(classId, currentUser.getId());
        if (!isTeacherRole && !Boolean.TRUE.equals(classInfo.getAllowStudentInvite())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "该班级不允许学生邀请同学加入", null);
        }

        // 根据账号查询目标用户
        User targetUser = getUserByAccountOrThrow(userAccount);

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
                .eq("status", 0);  // 待确认
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
        invitation.setStatus(0);  // 待用户确认
        invitation.setCreateTime(LocalDateTime.now());

        classUserInvitationMapper.insert(invitation);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void respondUserInvitation(Integer invitationId, Boolean accepted) {
        User currentUser = getCurrentUserOrThrow();

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

        // 更新邀请状态
        invitation.setStatus(accepted ? 1 : 2);  // 1-已同意，2-已拒绝
        invitation.setResponseTime(LocalDateTime.now());
        classUserInvitationMapper.updateById(invitation);

        // 如果同意，创建教师审核记录
        if (accepted) {
            ClassTeacherApproval approval = new ClassTeacherApproval();
            approval.setClassId(invitation.getClassId());
            approval.setInvitationId(invitation.getId());
            approval.setInviteeId(invitation.getInviteeId());
            approval.setStatus(0);  // 待教师审核
            approval.setCreateTime(LocalDateTime.now());
            
            classTeacherApprovalMapper.insert(approval);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTeacherApproval(Integer approvalId, Boolean approved, String comment) {
        User currentUser = getCurrentUserOrThrow();

        ClassTeacherApproval approval = classTeacherApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "审核记录不存在", null);
        }

        if (!Integer.valueOf(0).equals(approval.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该申请已处理", null);
        }

        // 检查审核人是否是老师或助理
        boolean isAdmin = isAdmin(currentUser);
        boolean isClassTeacher = isTeacher(approval.getClassId(), currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或助理可以审核邀请申请", null);
        }

        // 更新审核状态
        approval.setStatus(approved ? 1 : 2);  // 1-已通过，2-已拒绝
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
                ClassMember member = new ClassMember();
                member.setClassId(approval.getClassId());
                member.setUserId(approval.getInviteeId());
                member.setRole(0);  // 固定为学生
                member.setJoinTime(LocalDateTime.now());
                
                // 获取邀请记录中的邀请人 ID
                ClassUserInvitation invitation = classUserInvitationMapper.selectById(approval.getInvitationId());
                if (invitation != null) {
                    member.setInviteBy(invitation.getInviterId());
                }
                
                classMemberMapper.insert(member);
                
            }
        }

    }

    @Override
    public List<TeacherApprovalResponse> getPendingTeacherApprovals(Integer classId) {
        User currentUser = getCurrentUserOrThrow();

        // 检查是否是班级内的老师或助理
        boolean isAdmin = isAdmin(currentUser);
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或助理可以查看待审核邀请", null);
        }

        QueryWrapper<ClassTeacherApproval> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId)
                   .eq("status", 0)  // 待审核
                   .orderByDesc("create_time");
        List<ClassTeacherApproval> approvals = classTeacherApprovalMapper.selectList(queryWrapper);
        
        // 转换为 VO 列表
        return approvals.stream()
                .map(this::convertToTeacherApprovalResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String leaveClass(Integer classId) {
        User currentUser = getCurrentUserOrThrow();

        // 检查是否是成员
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", currentUser.getId());
        ClassMember member = classMemberMapper.selectOne(queryWrapper);
        
        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "你不是该班级的成员", null);
        }

        // 获取班级名称
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 如果是创建者，不能退出（需要先转让或解散班级）
        if (classInfo.getOwnerId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.CREATOR_CANNOT_LEAVE, "创建者不能退出班级", null);
        }

        // 级联软删除该学生在该班级的所有作业提交和附件
        cleanupStudentSubmissions(classId, currentUser.getId());

        // 硬删除学生成员记录（从班级中移除）
        classMemberMapper.deleteById(member.getId());

        return classInfo.getClassName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveClass(Integer classId, String account, String password, String confirmText) {
        User currentUser = getCurrentUserOrThrow();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 创建者或管理员可以解散班级
        boolean isAdmin = isAdmin(currentUser);
        boolean isOwner = classInfo.getOwnerId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有创建者或管理员可以解散班级", null);
        }

        // 账号密码二次校验（校验失败属于用户输入错误而非登录失效，返回 400 避免误触发前端登出）
        if (StrUtil.isBlank(account) || StrUtil.isBlank(password)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "账号和密码不能为空", null);
        }
        QueryWrapper<User> accQuery = new QueryWrapper<>();
        accQuery.and(q -> q.eq("email", account).or().apply("BINARY username = BINARY {0}", account));
        User checkUser = userMapper.selectOne(accQuery);
        if (checkUser == null || !checkUser.getId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "账号不属于当前登录用户", null);
        }
        if (!passwordUtil.matches(password, checkUser.getPassword())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "密码错误", null);
        }

        // 确认文案校验：我已确认要删除{className}课堂
        String className = classInfo.getClassName();
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
    public ClassInfo updateClassInfo(Integer classId, String className, String description) {
        // 1. 获取当前用户
        User currentUser = getCurrentUserOrThrow();

        // 2. 查询班级信息
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 3. 验证权限（只有老师或班级助理可以修改）
        boolean isTeacher = isTeacher(classId, currentUser.getId());
        if (!isTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或助理可以修改班级信息", null);
        }

        // 4. 更新班级信息
        classInfo.setClassName(className);
        classInfo.setDescription(description);
        classInfo.setUpdateTime(LocalDateTime.now());
        
        int updated = classInfoMapper.updateById(classInfo);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, "更新班级信息失败", null);
        }


        return classInfo;
    }

    @Override
    public void setStudentInviteAllowed(Integer classId, Boolean allowStudentInvite) {
        User currentUser = getCurrentUserOrThrow();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 只有老师（创建者/助理）可以修改学生邀请设置
        if (!isTeacher(classId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或助理可以修改邀请设置", null);
        }

        classInfo.setAllowStudentInvite(allowStudentInvite);
        classInfo.setUpdateTime(LocalDateTime.now());
        int updated = classInfoMapper.updateById(classInfo);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, "更新邀请设置失败", null);
        }
    }

    @Override
    public List<ClassInfo> getUserClasses(Integer userId) {
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId);
        List<ClassMember> members = classMemberMapper.selectList(memberQuery);

        return members.stream()
                .map(member -> classInfoMapper.selectById(member.getClassId()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean isTeacher(Integer classId, Integer userId) {
        // 管理员可以像老师一样管理所有班级
        User user = userMapper.selectById(userId);
        if (isAdmin(user)) {
            return true;
        }
        // 检查是否是班级创建者
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo != null && classInfo.getOwnerId().equals(userId)) {
            return true;
        }
        // 检查是否是班级老师
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("role", 1);
        return classMemberMapper.selectCount(queryWrapper) > 0;
    }
    
    @Override
    public List<Integer> getTeacherClassIds(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 管理员可管理所有班级，返回全部班级 ID
        User user = userMapper.selectById(userId);
        if (isAdmin(user)) {
            QueryWrapper<ClassInfo> allQuery = new QueryWrapper<>();
            allQuery.select("id");
            return classInfoMapper.selectList(allQuery).stream()
                    .map(ClassInfo::getId)
                    .collect(Collectors.toList());
        }

        // 查询自己是创建者的班级
        QueryWrapper<ClassInfo> ownerQuery = new QueryWrapper<>();
        ownerQuery.eq("owner_id", userId).select("id");
        List<ClassInfo> ownerClasses = classInfoMapper.selectList(ownerQuery);
        List<Integer> result = ownerClasses.stream()
                .map(ClassInfo::getId)
                .collect(Collectors.toList());
        
        // 查询自己是老师的班级
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId)
                   .eq("role", 1)
                   .select("class_id");
        List<ClassMember> teacherClasses = classMemberMapper.selectList(memberQuery);
        result.addAll(teacherClasses.stream()
                .map(ClassMember::getClassId)
                .collect(Collectors.toList()));
        
        return result;
    }

    @Override
    public List<Integer> getMemberClassIds(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 查询用户以任何角色加入的班级
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId).select("class_id");
        List<ClassMember> memberClasses = classMemberMapper.selectList(memberQuery);
        return memberClasses.stream()
                .map(ClassMember::getClassId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isStudent(Integer classId, Integer userId) {
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("role", 0);
        return classMemberMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean canSubmitWork(Integer classId, Integer userId) {
        // 班主任（创建者）不能提交作业
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo != null && classInfo.getOwnerId().equals(userId)) {
            return false;
        }
        // 学生（role=0）或助理/协作老师（role=1）可以提交
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId)
                .in("role", 0, 1);
        return classMemberMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean isClassMember(Integer classId, Integer userId) {
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId);
        return classMemberMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public String getUserRoleInClass(Integer classId, Integer userId) {
        // 检查是否是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", userId);
        ClassMember member = classMemberMapper.selectOne(memberQuery);
        
        if (member == null) {
            return null;
        }

        // 获取班级信息
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        return getUserRole(classInfo, member);
    }

    @Override
    public Integer getUserRoleCodeInClass(Integer classId, Integer userId) {
        // 检查是否是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", userId);
        ClassMember member = classMemberMapper.selectOne(memberQuery);
        
        if (member == null) {
            return null;
        }

        // 获取班级信息
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        return getUserRoleCode(classInfo, member);
    }

    @Override
    public String getUserRoleNameInClass(Integer classId, Integer userId) {
        // 检查是否是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", userId);
        ClassMember member = classMemberMapper.selectOne(memberQuery);
        
        if (member == null) {
            return null;
        }

        // 获取班级信息
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        return getUserRole(classInfo, member);
    }

    @Override
    public ClassInfo getClassById(Integer classId) {
        return classInfoMapper.selectById(classId);
    }
    
    @Override
    public List<ClassInfo> getClassByIds(List<Integer> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<ClassInfo> query = new QueryWrapper<>();
        query.in("id", classIds);
        return classInfoMapper.selectList(query);
    }

    @Override
    public ClassDetailResponse getClassDetail(Integer classId) {
        // 查询班级信息
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 权限校验：管理员可查看任意班级，普通用户仅可查看自己所在的班级
        User currentUser = getCurrentUserOrThrow();
        boolean isAdmin = isAdmin(currentUser);
        if (!isAdmin && !isClassMember(classId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级成员或管理员可以查看班级详情", null);
        }

        // 查询创建者信息
        User owner = userMapper.selectById(classInfo.getOwnerId());
        String ownerName = owner != null ? owner.getUsername() : "未知";

        // 查询成员统计
        QueryWrapper<ClassMember> countQuery = new QueryWrapper<>();
        countQuery.eq("class_id", classId);
        long memberCount = classMemberMapper.selectCount(countQuery);

        QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("class_id", classId).eq("role", 1);
        long teacherCount = classMemberMapper.selectCount(teacherQuery);

        QueryWrapper<ClassMember> studentQuery = new QueryWrapper<>();
        studentQuery.eq("class_id", classId).eq("role", 0);
        long studentCount = classMemberMapper.selectCount(studentQuery);

        // 查询当前用户在该班级的角色（前面权限校验已确保 currentUser 非空）
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", currentUser.getId());
        ClassMember member = classMemberMapper.selectOne(memberQuery);
        String userRole = getUserRole(classInfo, member);

        return new ClassDetailResponse(
                classInfo.getId(),
                classInfo.getClassName(),
                classInfo.getOwnerId(),
                ownerName,
                userRole,
                memberCount,
                teacherCount,
                studentCount,
                classInfo.getDescription(),
                classInfo.getAllowStudentInvite(),
                classInfo.getCreateTime()
        );
    }

    @Override
    public Page<ClassDetailResponse> getMyClasses(Integer userId, Integer pageNum, Integer pageSize) {
        // 按班级成员关系分页查询（管理员管理全部班级走 getAdminManageClasses）
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId);
        Page<ClassMember> memberPage = classMemberMapper.selectPage(
                new Page<>(pageNum, pageSize), memberQuery);
        Map<Integer, ClassMember> memberMap = memberPage.getRecords().stream()
                .collect(Collectors.toMap(ClassMember::getClassId, m -> m, (a, b) -> a));
        List<Integer> classIds = memberPage.getRecords().stream()
                .map(ClassMember::getClassId)
                .distinct()
                .collect(Collectors.toList());

        if (classIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        List<ClassDetailResponse> responses = buildClassDetailResponses(classIds, memberMap, false);
        Page<ClassDetailResponse> page = new Page<>(pageNum, pageSize, memberPage.getTotal());
        page.setRecords(responses);
        return page;
    }

    @Override
    public Page<ClassDetailResponse> getAdminManageClasses(Integer userId, Integer pageNum, Integer pageSize, String keyword) {
        // 仅管理员可管理全部班级
        User currentUser = userMapper.selectById(userId);
        if (!isAdmin(currentUser)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "仅管理员可管理全部班级", null);
        }

        // 分页查询全部班级（可按班级名称关键字过滤）
        QueryWrapper<ClassInfo> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.like("class_name", keyword.trim());
        }
        Page<ClassInfo> classPage = classInfoMapper.selectPage(
                new Page<>(pageNum, pageSize), queryWrapper);
        List<Integer> classIds = classPage.getRecords().stream()
                .map(ClassInfo::getId)
                .collect(Collectors.toList());

        if (classIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        List<ClassDetailResponse> responses = buildClassDetailResponses(classIds, Collections.emptyMap(), true);
        Page<ClassDetailResponse> page = new Page<>(pageNum, pageSize, classPage.getTotal());
        page.setRecords(responses);
        return page;
    }

    /**
     * 批量构建班级列表响应（复用班级信息/创建者/成员统计查询）
     *
     * @param forceTeacherRole 为 true 时所有班级的角色统一按"老师"返回（管理员视角）
     */
    private List<ClassDetailResponse> buildClassDetailResponses(
            List<Integer> classIds, Map<Integer, ClassMember> memberMap, boolean forceTeacherRole) {
        // 批量查询班级信息
        final Map<Integer, ClassInfo> classMap;
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.in("id", classIds);
        List<ClassInfo> classes = classInfoMapper.selectList(classQuery);
        classMap = classes.stream().collect(Collectors.toMap(ClassInfo::getId, c -> c));

        // 从已查询的班级信息中收集所有者ID
        List<Integer> ownerIds = classMap.values().stream()
                .map(ClassInfo::getOwnerId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询用户信息
        final Map<Integer, User> userMap;
        if (!ownerIds.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", ownerIds);
            List<User> users = userMapper.selectList(userQuery);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        } else {
            userMap = new HashMap<>();
        }

        // 转换为响应对象
        return classIds.stream()
                .map(classId -> {
                    ClassInfo classInfo = classMap.get(classId);
                    if (classInfo == null) {
                        return null;
                    }

                    // 从缓存中获取创建者信息
                    User owner = userMap.get(classInfo.getOwnerId());
                    String ownerName = owner != null ? owner.getUsername() : "未知";

                    // 查询成员统计（这些需要单独查询，因为涉及聚合）
                    QueryWrapper<ClassMember> countQuery = new QueryWrapper<>();
                    countQuery.eq("class_id", classInfo.getId());
                    long memberCount = classMemberMapper.selectCount(countQuery);

                    QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
                    teacherQuery.eq("class_id", classInfo.getId()).eq("role", 1);
                    long teacherCount = classMemberMapper.selectCount(teacherQuery);

                    QueryWrapper<ClassMember> studentQuery = new QueryWrapper<>();
                    studentQuery.eq("class_id", classInfo.getId()).eq("role", 0);
                    long studentCount = classMemberMapper.selectCount(studentQuery);

                    // 确定用户角色（管理员视角统一按老师处理）
                    String role = forceTeacherRole ? "老师" : getUserRole(classInfo, memberMap.get(classId));

                    return new ClassDetailResponse(
                            classInfo.getId(),
                            classInfo.getClassName(),
                            classInfo.getOwnerId(),
                            ownerName,
                            role,
                            memberCount,
                            teacherCount,
                            studentCount,
                            classInfo.getDescription(),
                            classInfo.getAllowStudentInvite(),
                            classInfo.getCreateTime()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 获取用户在班级中的角色
     * @param classInfo 班级信息
     * @param member 班级成员信息
     * @return 角色字符串（创建者/班级助理/学生）
     */
    private String getUserRole(ClassInfo classInfo, ClassMember member) {
        if (classInfo == null || member == null) {
            return null;
        }
        
        // 如果是班级创建者
        if (classInfo.getOwnerId().equals(member.getUserId())) {
            return "创建者";
        }
        
        Integer role = member.getRole();
        if (role == null) {
            return "学生";
        }

        return switch (role) {
            case 1 -> "老师";
            case 0 -> "学生";
            default -> "学生";
        };
    }

    /**
     * 获取用户在班级中的角色代码
     * @param classInfo 班级信息
     * @param member 班级成员信息
     * @return 角色代码：1-老师，0-学生，null-非成员
     */
    private Integer getUserRoleCode(ClassInfo classInfo, ClassMember member) {
        if (classInfo == null || member == null) {
            return null;
        }
        
        // 如果是班级创建者，返回特殊标记
        if (classInfo.getOwnerId().equals(member.getUserId())) {
            return 1;  // 创建者也算老师
        }
        
        return member.getRole();
    }

    @Override
    public Page<ClassMemberResponse> getClassMembers(Integer classId, Integer pageNum, Integer pageSize) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 权限校验：管理员不受限制，普通用户只能查询自己创建或加入的班级
        boolean isAdmin = isAdmin(currentUser);
        boolean isClassMember = isClassMember(classId, currentUser.getId());
        
        if (!isAdmin && !isClassMember) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "你无权查看该班级的成员列表", null);
        }

        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId);
        
        // 使用MyBatisPlus分页
        Page<ClassMember> memberPage = new Page<>(pageNum, pageSize);
        Page<ClassMember> pagedResult = classMemberMapper.selectPage(memberPage, queryWrapper);

        if (pagedResult.getRecords().isEmpty()) {
            Page<ClassMemberResponse> page = new Page<>(pageNum, pageSize, 0);
            page.setRecords(Collections.emptyList());
            return page;
        }

        // 批量查询优化 - 收集所有用户ID
        List<Integer> userIds = pagedResult.getRecords().stream()
                .map(ClassMember::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询用户信息
        final Map<Integer, User> userMap;
        if (!userIds.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", userIds);
            List<User> users = userMapper.selectList(userQuery);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        } else {
            userMap = new HashMap<>();
        }

        // 查询班级教师总数（role=1 表示教师）
        QueryWrapper<ClassMember> teacherCountQuery = new QueryWrapper<>();
        teacherCountQuery.eq("class_id", classId).eq("role", 1);
        long teacherCount = classMemberMapper.selectCount(teacherCountQuery);

        List<ClassMemberResponse> responses = pagedResult.getRecords().stream()
                .map(member -> {
                    // 从缓存中获取用户信息
                    User user = userMap.get(member.getUserId());
                    String userName = user != null ? user.getUsername() : "未知";
                    String userNo = user != null ? user.getUserNo() : "未知";

                    // 确定角色
                    String role = getUserRole(classInfo, member);

                    return new ClassMemberResponse(
                            member.getId(),
                            member.getUserId(),
                            userName,
                            userNo,
                            role,
                            member.getJoinTime(),
                            teacherCount
                    );
                })
                .toList();

        // 构建分页结果
        Page<ClassMemberResponse> page = new Page<>(pageNum, pageSize, pagedResult.getTotal());
        page.setRecords(responses);
        return page;
    }

    @Override
    public List<ClassMemberResponse> getAllClassMembers(Integer classId) {
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId);
        
        // 不分页，查询所有成员
        List<ClassMember> members = classMemberMapper.selectList(queryWrapper);

        return members.stream()
                .map(member -> {
                    User user = userMapper.selectById(member.getUserId());
                    String userName = user != null ? user.getUsername() : "未知";
                    String userNo = user != null ? user.getUserNo() : "未知";
                    
                    // 确定角色
                    ClassInfo classInfo = classInfoMapper.selectById(classId);
                    String role = getUserRole(classInfo, member);

                    return new ClassMemberResponse(
                            member.getId(),
                            member.getUserId(),
                            userName,
                            userNo,
                            role,
                            member.getJoinTime(),
                            null
                    );
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(Integer classId, Integer userId, Integer role) {
        User currentUser = getCurrentUserOrThrow();

        // 检查当前用户是否是老师
        if (!isTeacher(classId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师可以修改成员角色", null);
        }

        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId);
        ClassMember member = classMemberMapper.selectOne(queryWrapper);
        
        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "用户不在该班级中", null);
        }

        member.setRole(role);
        classMemberMapper.updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateClassApplicationResponse submitCreateClassRequest(String className, String description) {
        User currentUser = getCurrentUserOrThrow();

        // 创建申请记录
        ClassCreateApplication application = new ClassCreateApplication();
        application.setApplicantId(currentUser.getId());
        application.setClassName(className);
        application.setDescription(description);
        application.setStatus(0);  // 待审核

        classCreateApplicationMapper.insert(application);

        
        // 构建响应对象
        return new CreateClassApplicationResponse(
                application.getId(),
                application.getApplicantId(),
                application.getClassName(),
                application.getDescription(),
                application.getStatus(),
                application.getCreateTime()
        );
    }

    @Override
    public Page<ClassCreateApplication> getCreateApplications(Integer status, Integer pageNum, Integer pageSize) {
        User currentUser = getCurrentUserOrThrow();

        // 检查是否是管理员（permission >= 100）
        if (!isAdmin(currentUser)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员可以查看创建申请列表", null);
        }

        QueryWrapper<ClassCreateApplication> queryWrapper = new QueryWrapper<>();
        
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        // 按创建时间倒序排列
        queryWrapper.orderByDesc("create_time");
        
        // 使用MyBatisPlus分页
        Page<ClassCreateApplication> appPage = new Page<>(pageNum, pageSize);
        return classCreateApplicationMapper.selectPage(appPage, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveCreateApplication(Integer applicationId, Boolean approved, String comment) {
        User currentUser = getCurrentUserOrThrow();

        // 检查是否是管理员（permission >= 100）
        if (!isAdmin(currentUser)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员可以审核创建申请", null);
        }

        ClassCreateApplication application = classCreateApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "申请不存在", null);
        }

        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该申请已处理", null);
        }

        // 更新申请状态
        application.setStatus(approved ? 1 : 2);
        application.setReviewerId(currentUser.getId());
        application.setReviewTime(LocalDateTime.now());
        application.setReviewComment(comment);
        classCreateApplicationMapper.updateById(application);

        // 如果审核通过，创建班级
        if (approved) {
            ClassInfo classInfo = new ClassInfo();
            classInfo.setClassName(application.getClassName());
            classInfo.setDescription(application.getDescription());
            classInfo.setOwnerId(application.getApplicantId());
            classInfoMapper.insert(classInfo);

            // 关联申请和创建的班级
            application.setCreatedClassId(classInfo.getId());
            classCreateApplicationMapper.updateById(application);

            // 创建者自动成为老师
            ClassMember member = new ClassMember();
            member.setClassId(classInfo.getId());
            member.setUserId(application.getApplicantId());
            member.setRole(1);
            member.setJoinTime(LocalDateTime.now());
            classMemberMapper.insert(member);

        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JoinClassApplicationResponse submitJoinClassRequest(Integer classId) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

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
        application.setStatus(0);  // 待审核

        classJoinApplicationMapper.insert(application);

        
        // 构建响应对象
        JoinClassApplicationResponse response = new JoinClassApplicationResponse();
        response.setId(application.getId());
        response.setClassId(application.getClassId());
        response.setApplicantId(application.getApplicantId());
        response.setStatus(application.getStatus());
        response.setCreateTime(application.getCreateTime());
        response.setClassName(classInfo.getClassName());
        response.setApplicantName(currentUser.getUsername());
        
        return response;
    }

    @Override
    public Page<ClassJoinApplication> getJoinApplications(Integer classId, Integer status, Integer pageNum, Integer pageSize) {
        User currentUser = getCurrentUserOrThrow();

        // 检查权限：管理员或班级老师
        boolean isAdmin = isAdmin(currentUser);
        
        if (!isAdmin && classId != null) {
            // 如果不是管理员，必须是该班级的老师
            if (!isTeacher(classId, currentUser.getId())) {
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
                Page<ClassJoinApplication> emptyPage = new Page<>(pageNum, pageSize, 0);
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
            return classJoinApplicationMapper.selectPage(appPage, queryWrapper);
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
        return classJoinApplicationMapper.selectPage(appPage, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveJoinApplication(Integer applicationId, Boolean approved, String comment) {
        User currentUser = getCurrentUserOrThrow();

        ClassJoinApplication application = classJoinApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "申请不存在", null);
        }

        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该申请已处理", null);
        }

        // 检查权限：管理员或班级老师
        boolean isAdmin = isAdmin(currentUser);
        boolean isClassTeacher = isTeacher(application.getClassId(), currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员或班级老师可以审核加入申请", null);
        }

        // 更新申请状态
        application.setStatus(approved ? 1 : 2);
        application.setReviewerId(currentUser.getId());
        application.setReviewTime(LocalDateTime.now());
        application.setReviewComment(comment);
        classJoinApplicationMapper.updateById(application);

        // 如果审核通过，添加成员（学生）
        if (approved) {
            ClassMember member = new ClassMember();
            member.setClassId(application.getClassId());
            member.setUserId(application.getApplicantId());
            member.setRole(3);  // 固定为学生
            member.setJoinTime(LocalDateTime.now());
            classMemberMapper.insert(member);

        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassInvitation inviteUserToClassWithApproval(Integer classId, String userAccount) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否有权限邀请（必须是老师或管理员）
        boolean isAdmin = isAdmin(currentUser);
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师或管理员可以邀请用户加入班级", null);
        }

        // 根据账号查询目标用户
        User targetUser = getUserByAccountOrThrow(userAccount);

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
                .eq("status", 0);  // 待处理
        classInvitationMapper.delete(oldInvitationQuery);

        // 创建新的邀请记录
        ClassInvitation invitation = new ClassInvitation();
        invitation.setClassId(classId);
        invitation.setInviterId(currentUser.getId());
        invitation.setInviteeUserId(targetUser.getId());
        invitation.setStatus(0);  // 待处理

        classInvitationMapper.insert(invitation);

        return invitation;
    }

    @Override
    public List<InvitationResponse> getMyInvitations(Integer userId, Integer status) {
        User currentUser = getCurrentUserOrThrow();

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

    @Override
    public List<InvitationResponse> getMyUserInvitations(Integer userId) {
        User currentUser = getCurrentUserOrThrow();

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void respondInvitation(Integer invitationId, Boolean accepted) {
        User currentUser = getCurrentUserOrThrow();

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
        invitation.setStatus(accepted ? 1 : 2);  // 1-已同意，2-已拒绝
        invitation.setResponseTime(LocalDateTime.now());
        classInvitationMapper.updateById(invitation);

        // 如果同意，添加为班级成员（学生）
        if (accepted) {
            // 再次检查是否已经是成员
            QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
            memberQuery.eq("class_id", invitation.getClassId()).eq("user_id", currentUser.getId());
            if (classMemberMapper.selectCount(memberQuery) == 0) {
                ClassMember member = new ClassMember();
                member.setClassId(invitation.getClassId());
                member.setUserId(currentUser.getId());
                member.setRole(0);  // 固定为学生
                member.setJoinTime(LocalDateTime.now());
                member.setInviteBy(invitation.getInviterId());
                classMemberMapper.insert(member);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String getInviteCode(Integer classId) {
        User currentUser = getCurrentUserOrThrow();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        boolean isAdminUser = isAdmin(currentUser);
        boolean isTeacherUser = isTeacher(classId, currentUser.getId());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetInviteCode(Integer classId) {
        User currentUser = getCurrentUserOrThrow();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        boolean isAdminUser = isAdmin(currentUser);
        boolean isTeacherUser = isTeacher(classId, currentUser.getId());
        if (!isAdminUser && !isTeacherUser) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师可以重置邀请码", null);
        }

        // 生成新码覆盖旧码，旧码立即失效
        String newCode = generateRandomCode(25);
        classInfo.setInviteCode(newCode);
        classInfoMapper.updateById(classInfo);

        return newCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassJoinApplication joinClassByInviteCode(String inviteCode) {
        User currentUser = getCurrentUserOrThrow();

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
        member.setRole(0);  // 以学生身份加入
        member.setJoinTime(LocalDateTime.now());
        classMemberMapper.insert(member);

        
        // 为了保持接口一致性，返回一个“已通过”的申请记录（虚拟）
        ClassJoinApplication application = new ClassJoinApplication();
        application.setClassId(classInfo.getId());
        application.setApplicantId(currentUser.getId());
        application.setStatus(1);  // 直接设置为已通过
        application.setReviewerId(currentUser.getId());  // 自动审核
        application.setReviewTime(LocalDateTime.now());
        application.setCreateTime(LocalDateTime.now());
        
        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferClassOwnership(Integer classId, Integer newOwnerId) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是班级所有者
        if (!classInfo.getOwnerId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级所有者可以转让所有权", null);
        }

        // 检查新所有者是否是班级成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", newOwnerId);
        ClassMember newOwnerMember = classMemberMapper.selectOne(memberQuery);
        
        if (newOwnerMember == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "新所有者必须是班级成员", null);
        }

        // 不能转让给自己
        if (newOwnerId.equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "不能转让给自己", null);
        }

        // 更新班级所有者
        classInfo.setOwnerId(newOwnerId);
        classInfoMapper.updateById(classInfo);

        // 将新所有者设置为老师
        newOwnerMember.setRole(1);
        classMemberMapper.updateById(newOwnerMember);

        // 将原所有者降级为班级助理（保留在班级中，不再拥有创建者权限）
        QueryWrapper<ClassMember> oldOwnerQuery = new QueryWrapper<>();
        oldOwnerQuery.eq("class_id", classId).eq("user_id", currentUser.getId());
        ClassMember oldOwnerMember = classMemberMapper.selectOne(oldOwnerQuery);
        if (oldOwnerMember != null) {
            oldOwnerMember.setRole(1);  // 降级为班级助理
            classMemberMapper.updateById(oldOwnerMember);
        }

    }

    /**
     * 生成指定长度的随机码（大小写字母+数字）
     */
    private String generateRandomCode(int length) {
        return RandomUtil.randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", length);
    }

    /**
     * 清理学生在该班级的所有作业提交和附件（软删除）
     * @param classId 班级ID
     * @param userId 学生用户ID
     */
    private void cleanupStudentSubmissions(Integer classId, Integer userId) {
        // 1. 先查询该班级的所有作业ID（避免SQL注入）
        QueryWrapper<WorkInfo> workQuery = new QueryWrapper<>();
        workQuery.eq("class_id", classId)
                .select("id");
        List<WorkInfo> worksInClass = workMapper.selectList(workQuery);
        
        if (worksInClass.isEmpty()) {
            log.warn("No works found in class {} for user {}", classId, userId);
            return;
        }
        
        List<Integer> workIds = worksInClass.stream()
                .map(WorkInfo::getId)
                .collect(Collectors.toList());
        
        // 2. 查询该学生在这些作业中的提交记录
        QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.eq("submitter_id", userId)
                      .eq("is_deleted", false)
                      .in("work_id", workIds);
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(submissionQuery);
        
        if (submissions.isEmpty()) {
            return;
        }

        List<Integer> submissionIds = submissions.stream()
                .map(WorkSubmission::getId)
                .toList();

        // 2. 批量软删除附件记录
        QueryWrapper<WorkSubmissionAttachment> attQuery = new QueryWrapper<>();
        attQuery.in("submission_id", submissionIds)
               .eq("is_deleted", false);
        List<WorkSubmissionAttachment> attachments = workSubmissionAttachmentMapper.selectList(attQuery);
        
        if (!attachments.isEmpty()) {
            for (WorkSubmissionAttachment attachment : attachments) {
                attachment.setIsDeleted(true);
            }
            workSubmissionAttachmentMapper.update(null, attQuery);
        }
        
        // 3. 批量软删除提交记录
        QueryWrapper<WorkSubmission> updateQuery = new QueryWrapper<>();
        updateQuery.in("id", submissionIds);
        WorkSubmission updateEntity = new WorkSubmission();
        updateEntity.setIsDeleted(true);
        workSubmissionMapper.update(updateEntity, updateQuery);
        
    }

    /**
     * 硬删除班级下的所有作业提交记录和附件
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
