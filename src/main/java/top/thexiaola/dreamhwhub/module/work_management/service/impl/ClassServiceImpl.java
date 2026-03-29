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
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.dto.ClassDetailResponse;
import top.thexiaola.dreamhwhub.module.work_management.dto.ClassMemberResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassApplicationMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
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

    public ClassServiceImpl(ClassInfoMapper classInfoMapper, ClassMemberMapper classMemberMapper, 
                           UserMapper userMapper, ClassApplicationMapper classApplicationMapper) {
        this.classInfoMapper = classInfoMapper;
        this.classMemberMapper = classMemberMapper;
        this.userMapper = userMapper;
        this.classApplicationMapper = classApplicationMapper;
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
        if (classInfo != null && classInfo.getCreatorId().equals(currentUser.getId())) {
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
        if (!classInfo.getCreatorId().equals(currentUser.getId())) {
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
        User creator = userMapper.selectById(classInfo.getCreatorId());
        String creatorName = creator != null ? creator.getUsername() : "未知";

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
                classInfo.getCreatorId(),
                creatorName,
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
                    User creator = userMapper.selectById(classInfo.getCreatorId());
                    String creatorName = creator != null ? creator.getUsername() : "未知";

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
                            classInfo.getCreatorId(),
                            creatorName,
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
    public ClassApplication submitJoinClassRequest(Integer classId, Boolean isTeacher) {
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

        // 创建申请记录
        ClassApplication application = new ClassApplication();
        application.setType(2);  // 加入班级申请
        application.setClassId(classId);
        application.setApplicantId(currentUser.getId());
        application.setTargetRole(isTeacher);
        application.setStatus(0);  // 待审核
        application.setCreateTime(LocalDateTime.now());

        classApplicationMapper.insert(application);

        log.info("User {} submitted join class request: classId={}, role={}", 
                currentUser.getId(), classId, isTeacher ? "TEACHER" : "STUDENT");
        return application;
    }

    @Override
    public List<ClassApplication> getPendingApplications() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查是否是管理员（permission >= 100）
        if (currentUser.getPermission() == null || currentUser.getPermission() < 100) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有管理员可以查看待审核申请", null);
        }

        QueryWrapper<ClassApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 0)  // 待审核
                   .orderByDesc("create_time");
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
                classInfo.setCreatorId(application.getApplicantId());
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
                // 加入班级申请通过 - 直接添加为成员
                ClassMember member = new ClassMember();
                member.setClassId(application.getClassId());
                member.setUserId(application.getApplicantId());
                member.setIsTeacher(application.getTargetRole());
                member.setJoinTime(LocalDateTime.now());
                classMemberMapper.insert(member);

                log.info("Join class application approved, user {} joined class {}", 
                        application.getApplicantId(), application.getClassId());
            }
        }

        log.info("User {} approved application: id={}, type={}, approved={}", 
                currentUser.getId(), applicationId, application.getType(), approved);
    }
}
