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
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.domain.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.dto.ClassDetailResponse;
import top.thexiaola.dreamhwhub.module.work_management.dto.ClassMemberResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.util.UserUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * 班级管理服务实现类
 */
@Service
public class ClassServiceImpl implements ClassService {
    private static final Logger log = LoggerFactory.getLogger(ClassServiceImpl.class);

    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;

    public ClassServiceImpl(ClassInfoMapper classInfoMapper, ClassMemberMapper classMemberMapper, UserMapper userMapper) {
        this.classInfoMapper = classInfoMapper;
        this.classMemberMapper = classMemberMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassInfo createClass(String className, String description) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 创建班级
        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassName(className);
        classInfo.setDescription(description);
        classInfo.setCreatorId(currentUser.getId());
        classInfo.setClassCode(generateClassCode());
        classInfo.setStatus(1);
        classInfo.setCreateTime(LocalDateTime.now());
        classInfo.setUpdateTime(LocalDateTime.now());

        classInfoMapper.insert(classInfo);

        // 创建者自动成为老师
        ClassMember member = new ClassMember();
        member.setClassId(classInfo.getId());
        member.setUserId(currentUser.getId());
        member.setIsTeacher(true);
        member.setJoinTime(LocalDateTime.now());
        member.setInviteBy(null);

        classMemberMapper.insert(member);

        log.info("User {} created class: {}", currentUser.getId(), className);
        return classInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassMember joinClass(Integer classId, Boolean isTeacher) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 验证班级状态
        if (classInfo.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.CLASS_DISSOLVED, "班级已解散", null);
        }

        // 检查是否已经是成员
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", currentUser.getId());
        ClassMember existingMember = classMemberMapper.selectOne(queryWrapper);
        
        if (existingMember != null) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "你已经在该班级中", null);
        }

        // 创建成员记录
        ClassMember member = new ClassMember();
        member.setClassId(classId);
        member.setUserId(currentUser.getId());
        member.setIsTeacher(isTeacher);
        member.setJoinTime(LocalDateTime.now());
        member.setInviteBy(null);

        classMemberMapper.insert(member);

        log.info("User {} joined class {} as {}", currentUser.getId(), classId, isTeacher ? "TEACHER" : "STUDENT");
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

        // 更新班级状态为已解散（软删除）
        classInfo.setStatus(2);
        classInfo.setUpdateTime(LocalDateTime.now());
        classInfoMapper.updateById(classInfo);

        log.info("User {} deleted class {}", currentUser.getId(), classId);
    }

    @Override
    public List<ClassInfo> getUserClasses(Integer userId) {
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId);
        List<ClassMember> members = classMemberMapper.selectList(memberQuery);

        return members.stream()
                .map(member -> classInfoMapper.selectById(member.getClassId()))
                .filter(classInfo -> classInfo != null && classInfo.getStatus() == 1)
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
    @Transactional(rollbackFor = Exception.class)
    public ClassMember joinClassByCode(String classCode, Boolean isTeacher) {
        // 通过班级码查询班级
        QueryWrapper<ClassInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_code", classCode);
        ClassInfo classInfo = classInfoMapper.selectOne(queryWrapper);
        
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级邀请码无效", null);
        }

        return joinClass(classInfo.getId(), isTeacher);
    }

    @Override
    public ClassInfo getClassById(Integer classId) {
        return classInfoMapper.selectById(classId);
    }

    @Override
    public ClassInfo verifyClassCode(String classCode) {
        QueryWrapper<ClassInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_code", classCode);
        ClassInfo classInfo = classInfoMapper.selectOne(queryWrapper);
        
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级邀请码无效", null);
        }

        // 检查班级状态
        if (classInfo.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.CLASS_DISSOLVED, "班级已解散", null);
        }

        return classInfo;
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
                classInfo.getClassCode(),
                classInfo.getStatus(),
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
                    if (classInfo == null || classInfo.getStatus() != 1) {
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
                            classInfo.getClassCode(),
                            classInfo.getStatus(),
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

    /**
     * 生成 6 位随机班级邀请码
     */
    private String generateClassCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 确保唯一性
        QueryWrapper<ClassInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_code", code.toString());
        
        if (classInfoMapper.selectCount(queryWrapper) > 0) {
            return generateClassCode();
        }
        
        return code.toString();
    }
}
