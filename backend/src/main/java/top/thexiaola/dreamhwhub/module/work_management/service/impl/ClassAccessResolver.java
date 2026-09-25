package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.module.permission.service.PermissionService;
import top.thexiaola.dreamhwhub.module.school.constant.SchoolMemberRole;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;

import java.util.*;

/**
 * 班级访问解析器
 * 判定用户在某班级中的身份与权限，并解析其对外展示的班级角色
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassAccessResolver {

    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final SchoolService schoolService;

    /**
     * 具备「班级管理能力」的权限节点：拥有其中任意一个即可按教师身份管理所有班级。
     * 仅包含写操作节点（管理任意班级、解散班级、踢出成员、添加老师），
     * 只读的 class:view_all 与仅用于审批的 class:approve_join 不在此列。
     */
    private static final Set<String> CLASS_ADMIN_NODES = Set.of(
            PermissionNodes.CLASS_UPDATE,
            PermissionNodes.CLASS_DISSOLVE,
            PermissionNodes.CLASS_MEMBER_KICK,
            PermissionNodes.CLASS_TEACHER_ADD);

    /**
     * 判断用户是否具备任意班级管理权限（用于「管理员可管理所有班级」这类宽泛判定）
     *
     * @param user 用户对象
     * @return true-具备班级管理权限
     */
    public boolean isClassAdmin(User user) {
        return user != null && permissionService.hasAnyPermission(user.getId(), CLASS_ADMIN_NODES);
    }

    /**
     * 班级创建者是否仍具备教师身份（以班级所属学校内的角色为准）。
     * 创建者在该校已是学生或不再是成员时，视为教师身份已解除。
     *
     * @param classInfo 班级信息
     * @return true-创建者仍具备教师身份
     */
    public boolean isOwnerActive(ClassInfo classInfo) {
        if (classInfo == null || classInfo.getSchoolId() == null || classInfo.getOwnerId() == null) {
            return true;
        }
        SchoolMember owner = schoolService.getMember(classInfo.getSchoolId(), classInfo.getOwnerId());
        return owner != null && owner.getRole() != null && owner.getRole() >= SchoolMemberRole.TEACHER;
    }

    /**
     * 班级是否已「冻结」：创建者教师身份被解除，且创建者并非平台管理员。
     * <p>
     * 冻结期间：原创建者与班级老师均无法再管理该班，邀请码失效、不再接纳新学生；
     * 该校其他老师可申请接管。创建者教师身份恢复后自动解冻。
     *
     * @param classInfo 班级信息
     * @return true-班级已冻结
     */
    public boolean isClassFrozen(ClassInfo classInfo) {
        if (classInfo == null) {
            return false;
        }
        // 平台管理员持有的班级不因学校身份变化而冻结
        if (permissionService.isOp(classInfo.getOwnerId())) {
            return false;
        }
        return !isOwnerActive(classInfo);
    }

    /**
     * 批量判定多个班级的创建者是否仍具备教师身份（以班级所属学校内的角色为准）。
     * <p>
     * 一次按学校批量取回相关成员记录，避免逐班查询；缺少学校/创建者信息时按「仍活跃」处理，
     * 与 {@link #isOwnerActive(ClassInfo)} 的单班口径一致。
     *
     * @param classes 班级列表
     * @return classId -> 创建者是否仍具教师身份
     */
    public Map<Integer, Boolean> loadOwnerActiveMap(Collection<ClassInfo> classes) {
        Map<Integer, Boolean> result = new HashMap<>();
        if (classes == null || classes.isEmpty()) {
            return result;
        }
        // 按学校收集需要判定的创建者
        Map<Integer, Set<Integer>> schoolToOwners = new HashMap<>();
        for (ClassInfo classInfo : classes) {
            if (classInfo == null || classInfo.getId() == null) {
                continue;
            }
            if (classInfo.getSchoolId() == null || classInfo.getOwnerId() == null) {
                result.put(classInfo.getId(), true);
                continue;
            }
            schoolToOwners.computeIfAbsent(classInfo.getSchoolId(), k -> new HashSet<>())
                    .add(classInfo.getOwnerId());
        }
        // 批量取回各校成员身份
        Map<String, SchoolMember> memberMap = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : schoolToOwners.entrySet()) {
            for (SchoolMember m : schoolService.getMembersByUserIds(entry.getKey(), entry.getValue()).values()) {
                memberMap.put(entry.getKey() + ":" + m.getUserId(), m);
            }
        }
        for (ClassInfo classInfo : classes) {
            if (classInfo == null || classInfo.getId() == null
                    || classInfo.getSchoolId() == null || classInfo.getOwnerId() == null) {
                continue;
            }
            SchoolMember owner = memberMap.get(classInfo.getSchoolId() + ":" + classInfo.getOwnerId());
            result.put(classInfo.getId(),
                    owner != null && owner.getRole() != null && owner.getRole() >= SchoolMemberRole.TEACHER);
        }
        return result;
    }

    /**
     * 校验班级未冻结；已冻结时抛出业务异常（用于入班、邀请等写入路径）
     *
     * @param classInfo 班级信息
     */
    public void requireClassActive(ClassInfo classInfo) {
        if (isClassFrozen(classInfo)) {
            throw new BusinessException(BusinessErrorCode.CLASS_FROZEN,
                    "该班级的老师已失去教师身份，班级暂不接受新成员", null);
        }
    }

    public boolean isOrdinaryTeacher(Integer classId, Integer userId) {
        // 检查是否是班级成员且是老师
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("role", 1);
        ClassMember member = classMemberMapper.selectOne(queryWrapper);

        if (member == null) {
            return false;
        }

        // 检查是否是创建者
        ClassInfo classEntity = classInfoMapper.selectById(classId);
        return classEntity == null || !classEntity.getOwnerId().equals(userId); // 创建者不是普通老师
    }

    public boolean isTeacher(Integer classId, Integer userId) {
        // 具备班级管理权限的用户可以像老师一样管理所有班级
        User user = userMapper.selectById(userId);
        if (isClassAdmin(user)) {
            return true;
        }
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        // 班级冻结（创建者教师身份被解除）时，班级老师暂不可管理，需先申请接管
        if (isClassFrozen(classInfo)) {
            return false;
        }
        // 检查是否是班级创建者
        if (classInfo != null && classInfo.getOwnerId().equals(userId)) {
            return true;
        }
        // 检查是否是班级老师
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("role", 1);
        return classMemberMapper.selectCount(queryWrapper) > 0;
    }

    public boolean isStudent(Integer classId, Integer userId) {
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId).eq("role", 0);
        return classMemberMapper.selectCount(queryWrapper) > 0;
    }

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

    public boolean isClassMember(Integer classId, Integer userId) {
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", userId);
        return classMemberMapper.selectCount(queryWrapper) > 0;
    }

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

    /**
     * 查询班级所属学校名称，未关联学校时返回 null
     */
    public String resolveSchoolName(Integer schoolId) {
        if (schoolId == null) {
            return null;
        }
        return schoolService.getSchoolNames(List.of(schoolId)).get(schoolId);
    }

    /**
     * 获取用户在班级中的角色名称（自动查询其学校身份以区分老师与课代表）
     *
     * @param classInfo 班级信息
     * @param member    班级成员信息
     * @return 角色名称（创建者/老师/课代表/学生）
     */
    public String getUserRole(ClassInfo classInfo, ClassMember member) {
        return getUserRole(classInfo, member,
                member == null ? null : getSchoolMemberOf(classInfo, member.getUserId()));
    }

    /**
     * 获取用户在班级中的角色名称
     * 拥有班级管理员权限的成员：学校老师与学校管理员显示为「老师」，学校学生显示为「课代表」
     *
     * @param classInfo    班级信息
     * @param member       班级成员信息
     * @param schoolMember 该用户在学校中的身份，用于区分老师与课代表，可为 null
     * @return 角色名称（创建者/老师/课代表/学生）
     */
    public String getUserRole(ClassInfo classInfo, ClassMember member, SchoolMember schoolMember) {
        if (classInfo == null || member == null) {
            return null;
        }

        // 如果是班级创建者
        if (classInfo.getOwnerId().equals(member.getUserId())) {
            return "创建者";
        }

        Integer role = member.getRole();
        if (role == null || role == 0) {
            return "学生";
        }

        // 学校学生获得的班级管理员权限称为「课代表」
        if (schoolMember != null && Objects.equals(schoolMember.getRole(), SchoolMemberRole.STUDENT)) {
            return "课代表";
        }
        return "老师";
    }

    /**
     * 获取用户在班级中的角色代码
     * 
     * @param classInfo 班级信息
     * @param member    班级成员信息
     * @return 角色代码：1-老师，0-学生，null-非成员
     */
    public Integer getUserRoleCode(ClassInfo classInfo, ClassMember member) {
        if (classInfo == null || member == null) {
            return null;
        }

        // 如果是班级创建者，返回特殊标记
        if (classInfo.getOwnerId().equals(member.getUserId())) {
            return 1; // 创建者也算老师
        }

        return member.getRole();
    }

    /**
     * 批量加载班级所属学校的成员身份（姓名与学工号）
     */
    public Map<Integer, SchoolMember> loadSchoolMembers(ClassInfo classInfo, Collection<Integer> userIds) {
        if (classInfo == null || classInfo.getSchoolId() == null || userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return schoolService.getMembersByUserIds(classInfo.getSchoolId(), userIds);
    }

    /**
     * 按班级 ID 批量加载成员的学校内身份（姓名/学工号），返回以用户 ID 为键的映射。
     * 班级不存在或未关联学校、或用户集合为空时返回空映射。
     *
     * @param classId 班级 ID
     * @param userIds 用户 ID 集合
     * @return 用户 ID 到学校成员记录的映射
     */
    public Map<Integer, SchoolMember> loadSchoolMembersByClassId(Integer classId, Collection<Integer> userIds) {
        if (classId == null || userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return loadSchoolMembers(classInfoMapper.selectById(classId), userIds);
    }

    /**
     * 校验用户在班级所属学校有成员身份，姓名与学工号均取自该身份
     *
     * @return 该用户的学校成员记录
     */
    public SchoolMember requireSchoolMember(ClassInfo classInfo, Integer userId) {
        if (classInfo == null || classInfo.getSchoolId() == null) {
            throw new BusinessException(BusinessErrorCode.SCHOOL_NOT_FOUND, "班级未关联学校", null);
        }
        SchoolMember member = schoolService.getMember(classInfo.getSchoolId(), userId);
        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_SCHOOL, "请先加入该班级所属学校", null);
        }
        return member;
    }

    /**
     * 查询用户在班级所属学校的成员身份，用于区分「老师」与「课代表」，非成员返回 null
     */
    public SchoolMember getSchoolMemberOf(ClassInfo classInfo, Integer userId) {
        if (classInfo == null || classInfo.getSchoolId() == null || userId == null) {
            return null;
        }
        return schoolService.getMember(classInfo.getSchoolId(), userId);
    }

    /**
     * 根据学校身份决定用户加入班级时的角色：学校老师与学校管理员自动获得班级管理员权限
     *
     * @return 1-拥有班级管理员权限，0-普通成员
     */
    public int resolveClassRole(SchoolMember schoolMember) {
        if (schoolMember != null && schoolMember.getRole() != null
                && schoolMember.getRole() >= SchoolMemberRole.TEACHER) {
            return 1;
        }
        return 0;
    }
}
