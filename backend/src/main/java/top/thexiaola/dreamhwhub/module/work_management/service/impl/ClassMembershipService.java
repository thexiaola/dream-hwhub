package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.module.school.constant.SchoolMemberRole;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.support.WorkSubmissionCleaner;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * 班级成员服务
 * 负责成员角色调整、踢出、退出班级与所有权转让等成员生命周期操作
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassMembershipService {

    private final ClassInfoMapper classInfoMapper;
    private final ClassAccessResolver classAccessResolver;
    private final ClassMemberMapper classMemberMapper;
    private final WorkSubmissionCleaner workSubmissionCleaner;
    private final UserLookupSupport userLookup;

    @Transactional(rollbackFor = Exception.class)
    public void batchSetAssistantTeachers(Integer classId, List<Integer> studentUserIds) {
        User currentUser = userLookup.requireCurrentUser();

        ClassInfo classEntity = classInfoMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_TEACHER_ADD);
        boolean isClassTeacher = classAccessResolver.isTeacher(classId, currentUser.getId());

        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级管理员可以设置课代表", null);
        }

        boolean isOrdinaryTeacher = classAccessResolver.isOrdinaryTeacher(classId, currentUser.getId());
        if (isOrdinaryTeacher && !isAdmin) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    "只有班级创建者或平台管理员可以设置课代表", null);
        }

        // 一次性批量更新：把符合条件的成员角色直接置为 1（课代表），
        // 取代「逐个 selectOne + updateById」的 N 次往返
        List<Integer> candidates = studentUserIds.stream()
                .filter(id -> id != null && !id.equals(currentUser.getId()))
                .distinct()
                .toList();
        if (candidates.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "没有符合条件的学生可以被设置为课代表", null);
        }

        UpdateWrapper<ClassMember> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("class_id", classId)
                .eq("role", 0)
                .in("user_id", candidates)
                .set("role", 1);
        int successCount = classMemberMapper.update(null, updateWrapper);

        if (successCount == 0) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "没有符合条件的学生可以被设置为课代表", null);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public void batchKickStudentsFromClass(Integer classId, List<Integer> studentUserIds) {
        User currentUser = userLookup.requireCurrentUser();

        ClassInfo classEntity = classInfoMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_MEMBER_KICK);
        boolean isClassTeacher = classAccessResolver.isTeacher(classId, currentUser.getId());

        if (!isAdmin && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师或管理员可以踢出学生", null);
        }

        boolean isOrdinaryTeacher = classAccessResolver.isOrdinaryTeacher(classId, currentUser.getId());

        // 一次查询取出候选成员，避免在循环里逐个 selectOne（N 次往返）
        List<Integer> candidates = studentUserIds.stream()
                .filter(id -> id != null
                        && !id.equals(currentUser.getId())
                        && !id.equals(classEntity.getOwnerId()))
                .distinct()
                .toList();
        if (candidates.isEmpty()) {
            return;
        }

        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).in("user_id", candidates);
        List<ClassMember> members = classMemberMapper.selectList(memberQuery);

        // 过滤规则下沉到本次已取回的结果集（仅针对候选人，数据量受入参限制）
        List<Integer> kickIds = new ArrayList<>();
        for (ClassMember member : members) {
            // 普通助理不能踢出其他助理
            if (isOrdinaryTeacher && !isAdmin && member.getRole() != null && member.getRole() == 1) {
                continue;
            }
            kickIds.add(member.getId());
        }
        if (kickIds.isEmpty()) {
            return;
        }

        // 批量清理被踢学生的作业提交（一次 SQL 覆盖全部学生），再批量删除成员记录（单条 SQL）
        List<Integer> kickedUserIds = members.stream().map(ClassMember::getUserId).distinct().toList();
        workSubmissionCleaner.cleanupClassSubmissions(List.of(classId), kickedUserIds);
        classMemberMapper.deleteByIds(kickIds);

    }

    @Transactional(rollbackFor = Exception.class)
    public void demoteAssistantTeacher(Integer classId, Integer teacherUserId) {
        User currentUser = userLookup.requireCurrentUser();

        // 验证班级是否存在
        ClassInfo classEntity = classInfoMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 检查当前用户是否是创建者或拥有添加班级老师权限
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_TEACHER_ADD);
        boolean isCreator = classEntity.getOwnerId().equals(currentUser.getId());

        if (!isAdmin && !isCreator) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级创建者或平台管理员可以取消课代表权限", null);
        }

        // 不能操作自己
        if (teacherUserId.equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能操作自己", null);
        }

        // 检查目标用户是否拥有班级管理员权限
        QueryWrapper<ClassMember> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("class_id", classId).eq("user_id", teacherUserId).eq("role", 1);
        ClassMember teacherMember = classMemberMapper.selectOne(teacherQuery);

        if (teacherMember == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "该成员不是课代表或不在该班级中", null);
        }

        // 不能取消创建者的权限（虽然创建者不会是课代表，但为了安全还是检查一下）
        if (classEntity.getOwnerId().equals(teacherUserId)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能取消创建者的权限", null);
        }

        // 学校老师与学校管理员的班级管理员权限随学校身份自动获得，不能单独取消
        SchoolMember schoolMember = classAccessResolver.getSchoolMemberOf(classEntity, teacherUserId);
        if (schoolMember != null && schoolMember.getRole() != null
                && schoolMember.getRole() >= SchoolMemberRole.TEACHER) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    "该成员是学校老师或学校管理员，其班级管理员权限不能取消；如需调整请变更其学校身份", null);
        }

        // 取消班级管理员权限，降为普通成员
        teacherMember.setRole(0);
        classMemberMapper.updateById(teacherMember);

    }

    @Transactional(rollbackFor = Exception.class)
    public String leaveClass(Integer classId) {
        User currentUser = userLookup.requireCurrentUser();

        // 检查是否是成员
        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId).eq("user_id", currentUser.getId());
        ClassMember member = classMemberMapper.selectOne(queryWrapper);

        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "你不是该班级的成员", null);
        }

        // 获取班级名称
        ClassInfo classEntity = classInfoMapper.selectById(classId);
        if (classEntity == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 如果是创建者，不能退出（需要先转让或解散班级）
        if (classEntity.getOwnerId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.CREATOR_CANNOT_LEAVE, "创建者不能退出班级", null);
        }

        // 级联软删除该学生在该班级的所有作业提交和附件
        workSubmissionCleaner.cleanupClassSubmissions(classId, currentUser.getId());

        // 硬删除学生成员记录（从班级中移除）
        classMemberMapper.deleteById(member.getId());

        return classEntity.getClassName();
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferClassOwnership(Integer classId, Integer newOwnerId) {
        User currentUser = userLookup.requireCurrentUser();

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
            oldOwnerMember.setRole(1); // 原创建者保留班级管理员权限
            classMemberMapper.updateById(oldOwnerMember);
        }

    }

}
