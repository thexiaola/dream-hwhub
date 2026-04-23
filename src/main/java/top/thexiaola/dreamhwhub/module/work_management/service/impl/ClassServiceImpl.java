package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Objects;

/**
 * 班级管理服务实现类
 */
@Service
public class ClassServiceImpl implements ClassService {
    private static final Logger log = LoggerFactory.getLogger(ClassServiceImpl.class);

    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;
    private final ClassCreateApplicationMapper classCreateApplicationMapper;
    private final ClassJoinApplicationMapper classJoinApplicationMapper;
    private final ClassInviteApplicationMapper classInviteApplicationMapper;
    private final ClassInvitationMapper classInvitationMapper;
    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper;
    private final WorkMapper workMapper;
    private final WorkAttachmentMapper workAttachmentMapper;

    public ClassServiceImpl(ClassInfoMapper classInfoMapper, ClassMemberMapper classMemberMapper, 
                           UserMapper userMapper, ClassCreateApplicationMapper classCreateApplicationMapper,
                           ClassJoinApplicationMapper classJoinApplicationMapper,
                           ClassInviteApplicationMapper classInviteApplicationMapper,
                           ClassInvitationMapper classInvitationMapper,
                           WorkSubmissionMapper workSubmissionMapper,
                           WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper,
                           WorkMapper workMapper,
                           WorkAttachmentMapper workAttachmentMapper) {
        this.classInfoMapper = classInfoMapper;
        this.classMemberMapper = classMemberMapper;
        this.userMapper = userMapper;
        this.classCreateApplicationMapper = classCreateApplicationMapper;
        this.classJoinApplicationMapper = classJoinApplicationMapper;
        this.classInviteApplicationMapper = classInviteApplicationMapper;
        this.classInvitationMapper = classInvitationMapper;
        this.workSubmissionMapper = workSubmissionMapper;
        this.workSubmissionAttachmentMapper = workSubmissionAttachmentMapper;
        this.workMapper = workMapper;
        this.workAttachmentMapper = workAttachmentMapper;
    }

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
     * 根据账号查询用户，如果不存在则抛出异常
     * @param userAccount 用户账号（学号）
     * @return 用户对象
     */
    private User getUserByAccountOrThrow(String userAccount) {
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("user_no", userAccount);
        User targetUser = userMapper.selectOne(userQuery);
        
        if (targetUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }
        return targetUser;
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
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
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
            existingMember.setIsTeacher(true);
            classMemberMapper.updateById(existingMember);
            log.info("User {} updated user {} to TEACHER in class {}", currentUser.getId(), userAccount, classId);
            return existingMember;
        }

        // 创建新的老师成员记录
        ClassMember member = new ClassMember();
        member.setClassId(classId);
        member.setUserId(targetUser.getId());
        member.setIsTeacher(true);  // 设置为老师
        member.setJoinTime(LocalDateTime.now());
        member.setInviteBy(currentUser.getId());

        classMemberMapper.insert(member);

        log.info("User {} added user {} as TEACHER to class {}", currentUser.getId(), userAccount, classId);
        return member;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setStudentAsAssistantTeacher(Integer classId, Integer studentUserId) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是老师（包括创建者和班级助理）
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师可以设置班级助理", null);
        }

        // 班级助理不能设置其他学生为班级助理
        boolean isOrdinaryTeacher = isOrdinaryTeacher(classId, currentUser.getId());
        if (isOrdinaryTeacher && !isAdmin) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "班级助理不能设置其他学生为班级助理", null);
        }

        // 检查目标用户是否是学生
        QueryWrapper<ClassMember> studentQuery = new QueryWrapper<>();
        studentQuery.eq("class_id", classId).eq("user_id", studentUserId).eq("is_teacher", false);
        ClassMember studentMember = classMemberMapper.selectOne(studentQuery);
        
        if (studentMember == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "该用户不是班级学生或不在该班级中", null);
        }

        // 更新为老师
        studentMember.setIsTeacher(true);
        classMemberMapper.updateById(studentMember);

        log.info("User {} set student {} as assistant teacher in class {}", 
                currentUser.getId(), studentUserId, classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudentFromClass(Integer classId, Integer studentUserId) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是老师（包括创建者和班级助理）或管理员
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或管理员可以移除学生", null);
        }

        // 不能移除自己
        if (studentUserId.equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能移除自己，请使用退出班级功能", null);
        }

        // 不能移除其他老师（包括创建者和班级助理）
        QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("class_id", classId).eq("user_id", studentUserId).eq("is_teacher", true);
        if (classMemberMapper.selectCount(teacherQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能移除老师", null);
        }

        // 检查目标用户是否是学生
        QueryWrapper<ClassMember> studentQuery = new QueryWrapper<>();
        studentQuery.eq("class_id", classId).eq("user_id", studentUserId).eq("is_teacher", false);
        ClassMember studentMember = classMemberMapper.selectOne(studentQuery);
        
        if (studentMember == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "该用户不是班级学生或不在该班级中", null);
        }

        // 级联删除该学生在该班级的所有作业提交和附件
        cleanupStudentSubmissions(classId, studentUserId);

        // 删除学生成员记录
        classMemberMapper.deleteById(studentMember.getId());

        log.info("User {} removed student {} from class {}", 
                currentUser.getId(), studentUserId, classId);
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
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
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
        teacherQuery.eq("class_id", classId).eq("user_id", teacherUserId).eq("is_teacher", true);
        ClassMember teacherMember = classMemberMapper.selectOne(teacherQuery);
        
        if (teacherMember == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "该用户不是班级老师或不在该班级中", null);
        }

        // 不能取消创建者的权限（虽然创建者不会是班级助理，但为了安全还是检查一下）
        if (classInfo.getOwnerId().equals(teacherUserId)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能取消创建者的权限", null);
        }

        // 降级为学生
        teacherMember.setIsTeacher(false);
        classMemberMapper.updateById(teacherMember);

        log.info("User {} demoted user {} from assistant teacher to student in class {}", 
                currentUser.getId(), teacherUserId, classId);
    }

    @Override
    public boolean isOrdinaryTeacher(Integer classId, Integer userId) {
        // 检查是否是班级成员且是老师
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("is_teacher", true);
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
    public ClassInviteApplication studentInviteUser(Integer classId, String userAccount) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是班级内的学生
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isClassStudent = isStudent(classId, currentUser.getId());
        
        if (!isAdmin && !isClassStudent) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级内的学生才能提交邀请申请", null);
        }

        // 根据账号查询目标用户
        User targetUser = getUserByAccountOrThrow(userAccount);

        // 检查目标用户是否已经是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", targetUser.getId());
        if (classMemberMapper.selectCount(memberQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该用户已经在班级中", null);
        }

        // 检查是否有待审核的邀请申请
        QueryWrapper<ClassInviteApplication> appQuery = new QueryWrapper<>();
        appQuery.eq("class_id", classId)
                .eq("inviter_id", currentUser.getId())
                .eq("invitee_account", userAccount)
                .eq("status", 0);
        if (classInviteApplicationMapper.selectCount(appQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "已有待审核的邀请申请", null);
        }

        // 创建邀请申请记录
        ClassInviteApplication application = new ClassInviteApplication();
        application.setClassId(classId);
        application.setInviterId(currentUser.getId());
        application.setInviteeAccount(userAccount);
        application.setStatus(0);  // 待审核
        application.setCreateTime(LocalDateTime.now());

        classInviteApplicationMapper.insert(application);

        log.info("Student {} submitted invite application: classId={}, invitee={}", 
                currentUser.getId(), classId, userAccount);
        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveInviteApplication(Integer applicationId, Boolean approved, String comment) {
        User currentUser = getCurrentUserOrThrow();

        ClassInviteApplication application = classInviteApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "邀请申请不存在", null);
        }

        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该申请已处理", null);
        }

        // 检查审核人是否是老师或管理员
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isClassTeacher = isTeacher(application.getClassId(), currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或管理员可以审核邀请申请", null);
        }

        // 更新申请状态
        application.setStatus(approved ? 1 : 2);
        application.setReviewerId(currentUser.getId());
        application.setReviewTime(LocalDateTime.now());
        application.setReviewComment(comment);
        classInviteApplicationMapper.updateById(application);

        // 如果审核通过，创建邀请记录等待被邀请人确认
        if (approved) {
            // 根据账号查询目标用户
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.eq("user_no", application.getInviteeAccount());
            User targetUser = userMapper.selectOne(userQuery);
            
            if (targetUser != null) {
                // 检查是否已经是成员（双重检查）
                QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
                memberQuery.eq("class_id", application.getClassId())
                          .eq("user_id", targetUser.getId());
                if (classMemberMapper.selectCount(memberQuery) == 0) {
                    // 创建邀请记录，等待被邀请人确认
                    ClassInvitation invitation = new ClassInvitation();
                    invitation.setClassId(application.getClassId());
                    invitation.setInviterId(application.getInviterId());
                    invitation.setInviteeUserId(targetUser.getId());
                    invitation.setStatus(0); // 待确认
                    invitation.setExpireTime(LocalDateTime.now().plusDays(7)); // 7天有效期
                    invitation.setCreateTime(LocalDateTime.now());
                    
                    classInvitationMapper.insert(invitation);

                    log.info("Invite application approved, invitation created for user {} to join class {}", 
                            targetUser.getId(), application.getClassId());
                }
            }
        }

        log.info("User {} approved invite application: id={}, classId={}, approved={}", 
                currentUser.getId(), applicationId, application.getClassId(), approved);
    }

    @Override
    public List<ClassInviteApplication> getPendingInviteApplications(Integer classId) {
        User currentUser = getCurrentUserOrThrow();

        // 检查是否是班级内的老师或管理员
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或管理员可以查看邀请申请", null);
        }

        QueryWrapper<ClassInviteApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId)
                   .eq("status", 0)  // 待审核
                   .orderByDesc("create_time");
        return classInviteApplicationMapper.selectList(queryWrapper);
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

        // 级联删除该学生在该班级的所有作业提交和附件
        cleanupStudentSubmissions(classId, currentUser.getId());

        classMemberMapper.delete(queryWrapper);

        log.info("User {} left class {}", currentUser.getId(), classId);
        return classInfo.getClassName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveClass(Integer classId) {
        User currentUser = getCurrentUserOrThrow();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 只有创建者可以解散班级
        if (!classInfo.getOwnerId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有创建者可以解散班级", null);
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
            log.info("Hard deleted {} work attachment records in class {}", attachmentCount, classId);
            
            // 删除作业信息
            int workCount = workMapper.delete(workQuery);
            log.info("Hard deleted {} work records in class {}", workCount, classId);
        }

        // 3. 删除班级成员记录
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId);
        int memberCount = classMemberMapper.delete(memberQuery);
        log.info("Deleted {} class member records in class {}", memberCount, classId);

        // 4. 删除班级邀请记录
        QueryWrapper<ClassInvitation> invitationQuery = new QueryWrapper<>();
        invitationQuery.eq("class_id", classId);
        int invitationCount = classInvitationMapper.delete(invitationQuery);
        log.info("Deleted {} class invitation records in class {}", invitationCount, classId);

        // 5. 删除班级加入申请记录
        QueryWrapper<ClassJoinApplication> joinAppQuery = new QueryWrapper<>();
        joinAppQuery.eq("class_id", classId);
        int joinAppCount = classJoinApplicationMapper.delete(joinAppQuery);
        log.info("Deleted {} join application records in class {}", joinAppCount, classId);

        // 6. 删除班级邀请申请记录
        QueryWrapper<ClassInviteApplication> inviteAppQuery = new QueryWrapper<>();
        inviteAppQuery.eq("class_id", classId);
        int inviteAppCount = classInviteApplicationMapper.delete(inviteAppQuery);
        log.info("Deleted {} invite application records in class {}", inviteAppCount, classId);

        // 7. 最后删除班级信息
        classInfoMapper.deleteById(classId);

        log.info("User {} dissolved class {} completely with all related data", currentUser.getId(), classId);
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
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("is_teacher", true);
        return classMemberMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    public boolean isStudent(Integer classId, Integer userId) {
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("is_teacher", false);
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
    public ClassInfo getClassById(Integer classId) {
        return classInfoMapper.selectById(classId);
    }

    @Override
    public ClassDetailResponse getClassDetail(Integer classId) {
        // 查询班级信息
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 查询创建者信息
        User owner = userMapper.selectById(classInfo.getOwnerId());
        String ownerName = owner != null ? owner.getUsername() : "未知";

        // 查询成员统计
        QueryWrapper<ClassMember> countQuery = new QueryWrapper<>();
        countQuery.eq("class_id", classId);
        long memberCount = classMemberMapper.selectCount(countQuery);

        QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("class_id", classId).eq("is_teacher", true);
        long teacherCount = classMemberMapper.selectCount(teacherQuery);

        QueryWrapper<ClassMember> studentQuery = new QueryWrapper<>();
        studentQuery.eq("class_id", classId).eq("is_teacher", false);
        long studentCount = classMemberMapper.selectCount(studentQuery);

        // 查询当前用户在该班级的角色
        User currentUser = UserUtils.getCurrentUser();
        String userRole = null;
        if (currentUser != null) {
            QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
            memberQuery.eq("class_id", classId).eq("user_id", currentUser.getId());
            ClassMember member = classMemberMapper.selectOne(memberQuery);
            userRole = getUserRole(classInfo, member);
        }

        return new ClassDetailResponse(
                classInfo.getId(),
                classInfo.getClassName(),
                classInfo.getOwnerId(),
                ownerName,
                userRole,
                memberCount,
                teacherCount,
                studentCount
        );
    }

    @Override
    public Page<ClassDetailResponse> getMyClasses(Integer userId, Integer pageNum, Integer pageSize) {
        // 默认分页参数
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;  // 限制最大每页数量

        // 第一步：使用MyBatisPlus分页查询用户的班级成员关系
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId);
        Page<ClassMember> memberPage = new Page<>(pageNum, pageSize);
        Page<ClassMember> pagedMembers = classMemberMapper.selectPage(memberPage, memberQuery);

        // 第二步：转换为响应对象
        List<ClassDetailResponse> responses = pagedMembers.getRecords().stream()
                .map(member -> {
                    ClassInfo classInfo = classInfoMapper.selectById(member.getClassId());
                    if (classInfo == null) {
                        return null;
                    }

                    // 查询创建者信息
                    User owner = userMapper.selectById(classInfo.getOwnerId());
                    String ownerName = owner != null ? owner.getUsername() : "未知";

                    // 查询成员统计
                    QueryWrapper<ClassMember> countQuery = new QueryWrapper<>();
                    countQuery.eq("class_id", classInfo.getId());
                    long memberCount = classMemberMapper.selectCount(countQuery);

                    QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
                    teacherQuery.eq("class_id", classInfo.getId()).eq("is_teacher", true);
                    long teacherCount = classMemberMapper.selectCount(teacherQuery);

                    QueryWrapper<ClassMember> studentQuery = new QueryWrapper<>();
                    studentQuery.eq("class_id", classInfo.getId()).eq("is_teacher", false);
                    long studentCount = classMemberMapper.selectCount(studentQuery);

                    // 确定用户角色
                    String role = getUserRole(classInfo, member);

                    return new ClassDetailResponse(
                            classInfo.getId(),
                            classInfo.getClassName(),
                            classInfo.getOwnerId(),
                            ownerName,
                            role,
                            memberCount,
                            teacherCount,
                            studentCount
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        // 构建分页结果
        Page<ClassDetailResponse> page = new Page<>(pageNum, pageSize, pagedMembers.getTotal());
        page.setRecords(responses);
        return page;
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
        
        if (classInfo.getOwnerId().equals(member.getUserId())) {
            return "创建者";
        } else if (member.getIsTeacher()) {
            return "班级助理";
        } else {
            return "学生";
        }
    }

    @Override
    public Page<ClassMemberResponse> getClassMembers(Integer classId, Integer pageNum, Integer pageSize) {
        // 默认分页参数
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;  // 限制最大每页数量

        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId);
        
        // 使用MyBatisPlus分页
        Page<ClassMember> memberPage = new Page<>(pageNum, pageSize);
        Page<ClassMember> pagedResult = classMemberMapper.selectPage(memberPage, queryWrapper);

        List<ClassMemberResponse> responses = pagedResult.getRecords().stream()
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
                            member.getJoinTime()
                    );
                })
                .toList();

        // 构建分页结果
        Page<ClassMemberResponse> page = new Page<>(pageNum, pageSize, pagedResult.getTotal());
        page.setRecords(responses);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(Integer classId, Integer userId, Boolean isTeacher) {
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

        member.setIsTeacher(isTeacher);
        classMemberMapper.updateById(member);

        log.info("User {} updated user {} role to {} in class {}", 
                currentUser.getId(), userId, isTeacher ? "TEACHER" : "STUDENT", classId);
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

        log.info("User {} submitted create class request: {}", currentUser.getId(), className);
        
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
        if (currentUser.getPermission() == null || currentUser.getPermission() < 100) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员可以查看创建申请列表", null);
        }

        // 默认分页参数
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;  // 限制最大每页数量

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
        if (currentUser.getPermission() == null || currentUser.getPermission() < 100) {
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
            member.setIsTeacher(true);
            member.setJoinTime(LocalDateTime.now());
            classMemberMapper.insert(member);

            log.info("Create class application approved, class created: id={}, name={}", 
                    classInfo.getId(), classInfo.getClassName());
        }

        log.info("User {} approved create class application: id={}, approved={}", 
                currentUser.getId(), applicationId, approved);
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

        log.info("User {} submitted join class request: classId={}", 
                currentUser.getId(), classId);
        
        // 构建响应对象
        return new JoinClassApplicationResponse(
                application.getId(),
                application.getClassId(),
                application.getApplicantId(),
                application.getStatus(),
                application.getCreateTime()
        );
    }

    @Override
    public Page<ClassJoinApplication> getJoinApplications(Integer classId, Integer status, Integer pageNum, Integer pageSize) {
        User currentUser = getCurrentUserOrThrow();

        // 检查权限：管理员或班级老师
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        
        if (!isAdmin && classId != null) {
            // 如果不是管理员，必须是该班级的老师
            if (!isTeacher(classId, currentUser.getId())) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员或班级老师可以查看加入申请", null);
            }
        } else if (!isAdmin) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员可以查看所有加入申请", null);
        }

        // 默认分页参数
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;  // 限制最大每页数量

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
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
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
            member.setIsTeacher(false);  // 固定为学生
            member.setJoinTime(LocalDateTime.now());
            classMemberMapper.insert(member);

            log.info("Join class application approved, user {} joined class {} as STUDENT", 
                    application.getApplicantId(), application.getClassId());
        }

        log.info("User {} approved join class application: id={}, approved={}", 
                currentUser.getId(), applicationId, approved);
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
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
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

        // 检查是否已经有待处理的邀请
        QueryWrapper<ClassInvitation> invitationQuery = new QueryWrapper<>();
        invitationQuery.eq("class_id", classId)
                .eq("invitee_user_id", targetUser.getId())
                .eq("status", 0);  // 待处理
        if (classInvitationMapper.selectCount(invitationQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "已发送过邀请，请等待用户响应", null);
        }

        // 创建邀请记录（7天有效期）
        ClassInvitation invitation = new ClassInvitation();
        invitation.setClassId(classId);
        invitation.setInviterId(currentUser.getId());
        invitation.setInviteeUserId(targetUser.getId());
        invitation.setStatus(0);  // 待处理
        invitation.setExpireTime(LocalDateTime.now().plusDays(7));

        classInvitationMapper.insert(invitation);

        log.info("User {} sent invitation to user {} for class {}", 
                currentUser.getId(), userAccount, classId);
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
            response.setExpireTime(invitation.getExpireTime());
            response.setResponseTime(invitation.getResponseTime());
            response.setResponseComment(invitation.getResponseComment());
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
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void respondInvitation(Integer invitationId, Boolean accepted, String comment) {
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

        // 检查是否过期
        if (invitation.getExpireTime() != null && LocalDateTime.now().isAfter(invitation.getExpireTime())) {
            invitation.setStatus(3);  // 已过期
            classInvitationMapper.updateById(invitation);
            throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, "邀请已过期", null);
        }

        // 更新邀请状态
        invitation.setStatus(accepted ? 1 : 2);  // 1-已同意，2-已拒绝
        invitation.setResponseTime(LocalDateTime.now());
        invitation.setResponseComment(comment);
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
                member.setIsTeacher(false);  // 固定为学生
                member.setJoinTime(LocalDateTime.now());
                member.setInviteBy(invitation.getInviterId());
                classMemberMapper.insert(member);

                log.info("User {} accepted invitation and joined class {} as STUDENT", 
                        currentUser.getId(), invitation.getClassId());
            }
        } else {
            log.info("User {} rejected invitation to class {}", 
                    currentUser.getId(), invitation.getClassId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateOrRefreshInviteCode(Integer classId) {
        User currentUser = getCurrentUserOrThrow();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是老师或管理员
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师可以生成邀请码", null);
        }

        // 生成6位随机邀请码
        String inviteCode = generateRandomCode(6);
        
        // 更新班级邀请码
        classInfo.setInviteCode(inviteCode);
        classInfoMapper.updateById(classInfo);

        log.info("User {} generated invite code {} for class {}", currentUser.getId(), inviteCode, classId);
        return inviteCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassJoinApplication joinClassByInviteCode(String inviteCode) {
        User currentUser = getCurrentUserOrThrow();

        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "邀请码不能为空", null);
        }

        // 根据邀请码查找班级
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.eq("invite_code", inviteCode.trim());
        ClassInfo classInfo = classInfoMapper.selectOne(classQuery);
        
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "邀请码无效或已过期", null);
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
        member.setIsTeacher(false);  // 以学生身份加入
        member.setJoinTime(LocalDateTime.now());
        classMemberMapper.insert(member);

        log.info("User {} joined class {} directly via invite code {}", 
                currentUser.getId(), classInfo.getId(), inviteCode);
        
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
        newOwnerMember.setIsTeacher(true);
        classMemberMapper.updateById(newOwnerMember);

        // 将原所有者降级为班级助理（保留在班级中）
        QueryWrapper<ClassMember> oldOwnerQuery = new QueryWrapper<>();
        oldOwnerQuery.eq("class_id", classId).eq("user_id", currentUser.getId());
        ClassMember oldOwnerMember = classMemberMapper.selectOne(oldOwnerQuery);
        if (oldOwnerMember != null) {
            oldOwnerMember.setIsTeacher(true);  // 保持老师身份
            classMemberMapper.updateById(oldOwnerMember);
        }

        log.info("User {} transferred ownership of class {} to user {}", 
                currentUser.getId(), classId, newOwnerId);
    }

    /**
     * 生成指定长度的随机码
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    /**
     * 清理学生在该班级的所有作业提交和附件（软删除）
     * @param classId 班级ID
     * @param userId 学生用户ID
     */
    private void cleanupStudentSubmissions(Integer classId, Integer userId) {
        // 1. 查询该学生在该班级所有作业中的提交记录
        QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.eq("submitter_id", userId)
                      .eq("is_deleted", false)
                      .inSql("work_id", "SELECT id FROM work_info WHERE class_id = " + classId);
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(submissionQuery);
        
        if (submissions.isEmpty()) {
            log.info("No submissions found for user {} in class {}", userId, classId);
            return;
        }

        // 2. 软删除每个提交的附件记录
        for (WorkSubmission submission : submissions) {
            QueryWrapper<WorkSubmissionAttachment> attQuery = new QueryWrapper<>();
            attQuery.eq("submission_id", submission.getId())
                   .eq("is_deleted", false);
            List<WorkSubmissionAttachment> attachments = workSubmissionAttachmentMapper.selectList(attQuery);
            
            // 软删除附件记录
            for (WorkSubmissionAttachment attachment : attachments) {
                attachment.setIsDeleted(true);
                workSubmissionAttachmentMapper.updateById(attachment);
                log.info("Soft deleted student submission attachment record: id={}", attachment.getId());
            }
        }
        
        // 3. 软删除所有提交记录
        for (WorkSubmission submission : submissions) {
            submission.setIsDeleted(true);
            workSubmissionMapper.updateById(submission);
        }
        log.info("Soft deleted {} submission records for user {} in class {}", 
                submissions.size(), userId, classId);
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
            log.info("No works found in class {}", classId);
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
            log.info("No submissions found in class {}", classId);
            return;
        }

        List<Integer> submissionIds = submissions.stream()
                .map(WorkSubmission::getId)
                .toList();

        // 3. 硬删除所有提交附件记录
        QueryWrapper<WorkSubmissionAttachment> attQuery = new QueryWrapper<>();
        attQuery.in("submission_id", submissionIds);
        int attachmentCount = workSubmissionAttachmentMapper.delete(attQuery);
        log.info("Hard deleted {} submission attachment records in class {}", attachmentCount, classId);

        // 4. 硬删除所有提交记录
        int submissionCount = workSubmissionMapper.delete(submissionQuery);
        log.info("Hard deleted {} submission records in class {}", submissionCount, classId);
    }
}
