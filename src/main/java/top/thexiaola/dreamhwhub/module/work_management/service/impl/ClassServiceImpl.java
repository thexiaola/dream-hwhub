package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInviteApplication;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.dto.ClassDetailResponse;
import top.thexiaola.dreamhwhub.module.work_management.dto.ClassMemberResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassApplicationMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInviteApplicationMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.util.UserUtils;

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
    private final ClassApplicationMapper classApplicationMapper;
    private final ClassInviteApplicationMapper classInviteApplicationMapper;

    public ClassServiceImpl(ClassInfoMapper classInfoMapper, ClassMemberMapper classMemberMapper, 
                           UserMapper userMapper, ClassApplicationMapper classApplicationMapper,
                           ClassInviteApplicationMapper classInviteApplicationMapper) {
        this.classInfoMapper = classInfoMapper;
        this.classMemberMapper = classMemberMapper;
        this.userMapper = userMapper;
        this.classApplicationMapper = classApplicationMapper;
        this.classInviteApplicationMapper = classInviteApplicationMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassMember addTeacherToClass(Integer classId, String userAccount) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("user_no", userAccount);
        User targetUser = userMapper.selectOne(userQuery);
        
        if (targetUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }

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
    public ClassMember inviteUserToClass(Integer classId, String userAccount) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否有权限邀请（班级成员或管理员）
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isClassMember = isClassMember(classId, currentUser.getId());
        
        if (!isAdmin && !isClassMember) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级成员或管理员可以邀请用户加入班级", null);
        }

        // 如果是学生邀请，需要走审核流程
        boolean isClassStudent = isStudent(classId, currentUser.getId());
        if (isClassStudent && !isAdmin) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "学生邀请用户需要通过审核流程，请使用 studentInviteUser 方法", null);
        }

        // 根据账号查询目标用户
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("user_no", userAccount);
        User targetUser = userMapper.selectOne(userQuery);
        
        if (targetUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }

        // 检查目标用户是否已经是成员
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", targetUser.getId());
        ClassMember existingMember = classMemberMapper.selectOne(memberQuery);
        
        if (existingMember != null) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该用户已经在班级中", null);
        }

        // 创建新的成员记录（邀请加入也只能是学生）
        ClassMember member = new ClassMember();
        member.setClassId(classId);
        member.setUserId(targetUser.getId());
        member.setIsTeacher(false);  // 固定为学生
        member.setJoinTime(LocalDateTime.now());
        member.setInviteBy(currentUser.getId());

        classMemberMapper.insert(member);

        log.info("User {} invited user {} to class {} as STUDENT", 
                currentUser.getId(), userAccount, classId);
        return member;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setStudentAsAssistantTeacher(Integer classId, Integer studentUserId) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是老师（包括创建者和助理老师）
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有老师可以设置助理老师", null);
        }

        // 助理老师不能设置其他学生为助理老师
        boolean isOrdinaryTeacher = isOrdinaryTeacher(classId, currentUser.getId());
        if (isOrdinaryTeacher && !isAdmin) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "助理老师不能设置其他学生为助理老师", null);
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
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是老师（包括创建者和助理老师）或管理员
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isClassTeacher = isTeacher(classId, currentUser.getId());
        
        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或管理员可以移除学生", null);
        }

        // 不能移除自己
        if (studentUserId.equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能移除自己，请使用退出班级功能", null);
        }

        // 不能移除其他老师（包括创建者和助理老师）
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

        // 删除学生成员记录
        classMemberMapper.deleteById(studentMember.getId());

        log.info("User {} removed student {} from class {}", 
                currentUser.getId(), studentUserId, classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void demoteAssistantTeacher(Integer classId, Integer teacherUserId) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是创建者或管理员
        boolean isAdmin = currentUser.getPermission() != null && currentUser.getPermission() >= 100;
        boolean isCreator = classInfo.getOwnerId().equals(currentUser.getId());
        
        if (!isAdmin && !isCreator) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级创建者或管理员可以取消助理老师权限", null);
        }

        // 不能操作自己
        if (teacherUserId.equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能操作自己", null);
        }

        // 检查目标用户是否是老师（包括助理老师）
        QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("class_id", classId).eq("user_id", teacherUserId).eq("is_teacher", true);
        ClassMember teacherMember = classMemberMapper.selectOne(teacherQuery);
        
        if (teacherMember == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "该用户不是班级老师或不在该班级中", null);
        }

        // 不能取消创建者的权限（虽然创建者不会是助理老师，但为了安全还是检查一下）
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
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.eq("user_no", userAccount);
        User targetUser = userMapper.selectOne(userQuery);
        
        if (targetUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }

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
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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

        // 如果审核通过，添加被邀请人到班级
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
                    ClassMember member = new ClassMember();
                    member.setClassId(application.getClassId());
                    member.setUserId(targetUser.getId());
                    member.setIsTeacher(false);  // 学生邀请的人只能作为学生加入
                    member.setJoinTime(LocalDateTime.now());
                    member.setInviteBy(application.getInviterId());
                    
                    classMemberMapper.insert(member);

                    log.info("Invite application approved, user {} joined class {} as STUDENT", 
                            targetUser.getId(), application.getClassId());
                }
            }
        }

        log.info("User {} approved invite application: id={}, classId={}, approved={}", 
                currentUser.getId(), applicationId, application.getClassId(), approved);
    }

    @Override
    public List<ClassInviteApplication> getPendingInviteApplications(Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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
    public void leaveClass(Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查是否是成员
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", currentUser.getId());
        ClassMember member = classMemberMapper.selectOne(queryWrapper);
        
        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "你不是该班级的成员", null);
        }

        // 如果是创建者，不能退出（需要先转让或删除班级）
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo != null && classInfo.getOwnerId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.CREATOR_CANNOT_LEAVE, "创建者不能退出班级", null);
        }

        classMemberMapper.delete(queryWrapper);

        log.info("User {} left class {}", currentUser.getId(), classId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 只有创建者可以删除班级
        if (!classInfo.getOwnerId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有创建者可以删除班级", null);
        }

        // 直接删除班级（级联删除相关数据）
        classInfoMapper.deleteById(classId);

        log.info("User {} deleted class {}", currentUser.getId(), classId);
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
            if (member != null && member.getIsTeacher() != null) {
                userRole = member.getIsTeacher() ? "TEACHER" : "STUDENT";
            }
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
    public List<ClassDetailResponse> getMyClasses(Integer userId) {
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId);
        List<ClassMember> members = classMemberMapper.selectList(memberQuery);

        return members.stream()
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

                    return new ClassDetailResponse(
                            classInfo.getId(),
                            classInfo.getClassName(),
                            classInfo.getOwnerId(),
                            ownerName,
                            member.getIsTeacher() != null ? (member.getIsTeacher() ? "TEACHER" : "STUDENT") : null,
                            memberCount,
                            teacherCount,
                            studentCount
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<ClassMemberResponse> getClassMembers(Integer classId) {
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId);
        List<ClassMember> members = classMemberMapper.selectList(queryWrapper);

        return members.stream()
                .map(member -> {
                    User user = userMapper.selectById(member.getUserId());
                    String userName = user != null ? user.getUsername() : "未知";
                    String userNo = user != null ? user.getUserNo() : "未知";
                    String role = member.getIsTeacher() != null ? (member.getIsTeacher() ? "TEACHER" : "STUDENT") : "未知";

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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(Integer classId, Integer userId, Boolean isTeacher) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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
    public ClassApplication submitCreateClassRequest(String className, String description) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 创建申请记录
        ClassApplication application = new ClassApplication();
        application.setType(1);  // 创建班级申请
        application.setApplicantId(currentUser.getId());
        application.setClassName(className);
        application.setDescription(description);
        application.setStatus(0);  // 待审核
        application.setCreateTime(LocalDateTime.now());

        classApplicationMapper.insert(application);

        log.info("User {} submitted create class request: {}", currentUser.getId(), className);
        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassApplication submitJoinClassRequest(Integer classId) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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
        QueryWrapper<ClassApplication> appQuery = new QueryWrapper<>();
        appQuery.eq("type", 2)
                .eq("class_id", classId)
                .eq("applicant_id", currentUser.getId())
                .eq("status", 0);
        if (classApplicationMapper.selectCount(appQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "你已有待审核的申请", null);
        }

        // 创建申请记录（加入班级只能成为学生）
        ClassApplication application = new ClassApplication();
        application.setType(2);  // 加入班级申请
        application.setClassId(classId);
        application.setApplicantId(currentUser.getId());
        application.setStatus(0);  // 待审核
        application.setCreateTime(LocalDateTime.now());

        classApplicationMapper.insert(application);

        log.info("User {} submitted join class request: classId={}, role=STUDENT", 
                currentUser.getId(), classId);
        return application;
    }

    @Override
    public List<ClassApplication> getClassApplications(Integer type, Integer status, Integer classId, Integer applicantId) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查是否是管理员（permission >= 100）
        if (currentUser.getPermission() == null || currentUser.getPermission() < 100) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员可以查看申请列表", null);
        }

        QueryWrapper<ClassApplication> queryWrapper = new QueryWrapper<>();
        
        // 动态添加筛选条件
        if (type != null) {
            queryWrapper.eq("type", type);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (classId != null) {
            queryWrapper.eq("class_id", classId);
        }
        if (applicantId != null) {
            queryWrapper.eq("applicant_id", applicantId);
        }
        
        // 按创建时间倒序排列
        queryWrapper.orderByDesc("create_time");
        
        return classApplicationMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveApplication(Integer applicationId, Boolean approved, String comment) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查是否是管理员（permission >= 100）
        if (currentUser.getPermission() == null || currentUser.getPermission() < 100) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员可以审核申请", null);
        }

        ClassApplication application = classApplicationMapper.selectById(applicationId);
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
        classApplicationMapper.updateById(application);

        // 如果审核通过，执行相应操作
        if (approved) {
            if (Integer.valueOf(1).equals(application.getType())) {
                // 创建班级申请通过 - 自动创建班级
                ClassInfo classInfo = new ClassInfo();
                classInfo.setClassName(application.getClassName());
                classInfo.setDescription(application.getDescription());
                classInfo.setOwnerId(application.getApplicantId());
                classInfo.setCreateTime(LocalDateTime.now());
                classInfo.setUpdateTime(LocalDateTime.now());
                classInfoMapper.insert(classInfo);

                // 创建者自动成为老师
                ClassMember member = new ClassMember();
                member.setClassId(classInfo.getId());
                member.setUserId(application.getApplicantId());
                member.setIsTeacher(true);
                member.setJoinTime(LocalDateTime.now());
                classMemberMapper.insert(member);
            } else if (Integer.valueOf(2).equals(application.getType())) {
                // 加入班级申请通过 - 只能成为学生
                ClassMember member = new ClassMember();
                member.setClassId(application.getClassId());
                member.setUserId(application.getApplicantId());
                member.setIsTeacher(false);  // 固定为学生
                member.setJoinTime(LocalDateTime.now());
                classMemberMapper.insert(member);

                log.info("Join class application approved, user {} joined class {} as STUDENT", 
                        application.getApplicantId(), application.getClassId());
            }
        }

        log.info("User {} approved application: id={}, type={}, approved={}", 
                currentUser.getId(), applicationId, application.getType(), approved);
    }
}
