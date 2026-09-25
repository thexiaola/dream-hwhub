package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 班级查询服务
 * 负责班级详情、成员列表与「我的班级」等只读查询与响应组装
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassQueryService {

    private final ClassInfoMapper classInfoMapper;
    private final ClassAccessResolver classAccessResolver;
    private final ClassMemberMapper classMemberMapper;
    private final ClassTakeoverApplicationMapper classTakeoverApplicationMapper;
    private final UserMapper userMapper;
    private final SchoolService schoolService;
    private final UserLookupSupport userLookup;

    public List<Integer> getTeacherClassIds(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 具备班级管理权限的用户可管理所有班级，返回全部班级 ID
        User user = userMapper.selectById(userId);
        if (classAccessResolver.isClassAdmin(user)) {
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

    public List<ClassInfo> getClassByIds(List<Integer> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<ClassInfo> query = new QueryWrapper<>();
        query.in("id", classIds);
        return classInfoMapper.selectList(query);
    }

    public ClassDetailResponse getClassDetail(Integer classId) {
        // 查询班级信息
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 权限校验：拥有查看全部班级权限者可查看任意班级，普通用户仅可查看自己所在的班级；
        // 班级冻结时，本校其他老师可进入查看并申请接管
        User currentUser = userLookup.requireCurrentUser();
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_VIEW_ALL);
        boolean isMember = classAccessResolver.isClassMember(classId, currentUser.getId());
        boolean canViewFrozenAsTeacher = classAccessResolver.isClassFrozen(classInfo)
                && schoolService.isSchoolTeacher(classInfo.getSchoolId(), currentUser.getId());
        if (!isAdmin && !isMember && !canViewFrozenAsTeacher) {
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
        String userRole = classAccessResolver.getUserRole(classInfo, member);

        String schoolName = classAccessResolver.resolveSchoolName(classInfo.getSchoolId());

        ClassDetailResponse response = new ClassDetailResponse(
                classInfo.getId(),
                classInfo.getClassName(),
                classInfo.getSchoolId(),
                schoolName,
                classInfo.getOwnerId(),
                ownerName,
                userRole,
                classAccessResolver.getUserRoleCode(classInfo, member),
                memberCount,
                teacherCount,
                studentCount,
                classInfo.getDescription(),
                classInfo.getAllowStudentInvite(),
                classInfo.getCreateTime());
        fillTakeoverFields(response, classInfo, currentUser, member);
        return response;
    }

    /**
     * 分页查询我加入的班级
     *
     * @param schoolId     仅返回该学校下的班级，为空则不限学校
     * @param roleCode     1-仅返回我在该班拥有班级管理员权限的班级，0-仅返回我是普通成员的班级，为空则不限角色
     * @param excludeOwner 为 true 时排除我是创建者的班级（用于「我听的课」）
     */
    public Page<ClassDetailResponse> getMyClasses(Integer userId, Integer pageNum, Integer pageSize,
            Integer schoolId, Integer roleCode, Boolean excludeOwner) {
        // 按班级成员关系分页查询（管理员管理全部班级走 getAdminManageClasses）
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId);

        // 学校、角色等筛选条件全部下推数据库，避免取回全量后在内存过滤
        if (schoolId != null) {
            memberQuery.apply("class_id IN (SELECT id FROM class_info WHERE school_id = {0})", schoolId);
        }
        if (roleCode != null) {
            if (roleCode >= 1) {
                // 拥有班级管理员权限：成员角色为管理员，或本人是该班创建者
                memberQuery.and(wrapper -> wrapper.eq("role", 1)
                        .or()
                        .apply("class_id IN (SELECT id FROM class_info WHERE owner_id = {0})", userId));
            } else if (roleCode == 0) {
                memberQuery.eq("role", 0)
                        .apply("class_id NOT IN (SELECT id FROM class_info WHERE owner_id = {0})", userId);
            }
        }
        // 与 roleCode 同时传入时表示「在我不是创建者的班里」，此处仅用于「我听的课」
        if (Boolean.TRUE.equals(excludeOwner)) {
            memberQuery.apply("class_id NOT IN (SELECT id FROM class_info WHERE owner_id = {0})", userId);
        }

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

    public Page<ClassDetailResponse> getAdminManageClasses(Integer userId, Integer pageNum, Integer pageSize,
            String keyword, Integer schoolId) {
        // 仅拥有查看全部班级权限者可管理班级列表
        User currentUser = userMapper.selectById(userId);
        if (!userLookup.hasPermission(currentUser, PermissionNodes.CLASS_VIEW_ALL)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "仅管理员可管理全部班级", null);
        }

        // 分页查询全部班级（可按班级名称关键字、所属学校过滤，条件全部下推数据库）
        QueryWrapper<ClassInfo> queryWrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            queryWrapper.like("class_name", keyword.trim());
        }
        if (schoolId != null) {
            queryWrapper.eq("school_id", schoolId);
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
     * <p>
     * 所有关联数据（成员统计、创建者、学校名、接管策略、冻结判定）均批量取回，
     * 不在循环里逐班查询，避免 N+1。
     *
     * @param forceTeacherRole 为 true 时所有班级的角色统一按"老师"返回（管理员视角）
     */
    private List<ClassDetailResponse> buildClassDetailResponses(
            List<Integer> classIds, Map<Integer, ClassMember> memberMap, boolean forceTeacherRole) {
        // 批量查询班级信息
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.in("id", classIds);
        List<ClassInfo> classes = classInfoMapper.selectList(classQuery);
        Map<Integer, ClassInfo> classMap = classes.stream()
                .collect(Collectors.toMap(ClassInfo::getId, c -> c));

        // 批量查询创建者信息
        List<Integer> ownerIds = classMap.values().stream()
                .map(ClassInfo::getOwnerId)
                .distinct()
                .collect(Collectors.toList());
        final Map<Integer, User> userMap;
        if (!ownerIds.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", ownerIds);
            userMap = userMapper.selectList(userQuery).stream().collect(Collectors.toMap(User::getId, u -> u));
        } else {
            userMap = new HashMap<>();
        }

        // 批量查询学校名
        Set<Integer> schoolIds = classMap.values().stream()
                .map(ClassInfo::getSchoolId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> schoolNameMap = schoolService.getSchoolNames(schoolIds);
        // 批量查询各校「接管是否自动同意」
        Map<Integer, Boolean> takeoverAutoMap = schoolService.getClassTakeoverAutoApprove(schoolIds);

        // 批量查询各班成员统计（一次 GROUP BY class_id, role，取代每班 3 次 count）
        Map<Integer, long[]> memberStatsMap = loadMemberStats(classIds);

        // 批量判定创建者是否仍具教师身份（一次取回相关学校成员）
        Map<Integer, Boolean> ownerActiveMap = classAccessResolver.loadOwnerActiveMap(classes);

        // 批量查询「我」在各班所属学校的身份（区分老师与课代表），避免逐班查询
        Map<String, SchoolMember> selfSchoolMemberMap = loadSelfSchoolMembers(classes, memberMap);

        // 转换为响应对象
        return classIds.stream()
                .map(classId -> {
                    ClassInfo classInfo = classMap.get(classId);
                    if (classInfo == null) {
                        return null;
                    }

                    User owner = userMap.get(classInfo.getOwnerId());
                    String ownerName = owner != null ? owner.getUsername() : "未知";

                    long[] stats = memberStatsMap.getOrDefault(classId, new long[3]);

                    // 确定用户角色（管理员视角统一按老师处理）
                    ClassMember selfMember = memberMap.get(classId);
                    SchoolMember selfSchoolMember = selfMember == null ? null
                            : selfSchoolMemberMap.get(classInfo.getSchoolId() + ":" + selfMember.getUserId());
                    String role = forceTeacherRole ? "老师"
                            : classAccessResolver.getUserRole(classInfo, selfMember, selfSchoolMember);

                    // 冻结判定：创建者非平台管理员且已失去教师身份
                    boolean ownerIsOp = owner != null && Boolean.TRUE.equals(owner.getIsOp());
                    boolean ownerActive = ownerActiveMap.getOrDefault(classId, true);

                    ClassDetailResponse response = new ClassDetailResponse(
                            classInfo.getId(),
                            classInfo.getClassName(),
                            classInfo.getSchoolId(),
                            schoolNameMap.get(classInfo.getSchoolId()),
                            classInfo.getOwnerId(),
                            ownerName,
                            role,
                            forceTeacherRole ? 1 : classAccessResolver.getUserRoleCode(classInfo, selfMember),
                            stats[0],
                            stats[1],
                            stats[2],
                            classInfo.getDescription(),
                            classInfo.getAllowStudentInvite(),
                            classInfo.getCreateTime());
                    // 列表场景不携带「我是否可接管」的个性化判定（批量列表用管理员/成员视角），
                    // 仅给出班级是否冻结，供前端展示状态
                    response.setFrozen(!ownerIsOp && !ownerActive);
                    response.setOwnerActive(ownerActive);
                    response.setCanTakeover(false);
                    response.setTakeoverPending(false);
                    response.setTakeoverAutoApprove(takeoverAutoMap.getOrDefault(classInfo.getSchoolId(), true));
                    return response;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 批量统计各班成员数/老师数/学生数：一次 GROUP BY class_id + role，返回
     * classId -> [memberCount, teacherCount, studentCount]
     */
    private Map<Integer, long[]> loadMemberStats(Collection<Integer> classIds) {
        Map<Integer, long[]> result = new HashMap<>();
        if (classIds == null || classIds.isEmpty()) {
            return result;
        }
        QueryWrapper<ClassMember> query = new QueryWrapper<>();
        query.in("class_id", classIds)
                .select("class_id", "role", "COUNT(*) AS cnt")
                .groupBy("class_id", "role");
        for (Map<String, Object> row : classMemberMapper.selectMaps(query)) {
            Object classId = row.get("class_id");
            Object role = row.get("role");
            Object cnt = row.get("cnt");
            if (!(classId instanceof Number cid) || !(cnt instanceof Number count)) {
                continue;
            }
            long[] arr = result.computeIfAbsent(cid.intValue(), k -> new long[3]);
            arr[0] += count.longValue();
            if (role instanceof Number r) {
                if (r.intValue() == 1) {
                    arr[1] += count.longValue();
                } else if (r.intValue() == 0) {
                    arr[2] += count.longValue();
                }
            }
        }
        return result;
    }

    /**
     * 批量查询「我」在每个班级所属学校中的成员身份（用于区分老师与课代表）。
     * 按学校批量取回后以 schoolId:userId 为键，避免逐班查询。
     */
    private Map<String, SchoolMember> loadSelfSchoolMembers(
            List<ClassInfo> classes, Map<Integer, ClassMember> memberMap) {
        Map<Integer, Set<Integer>> schoolToUsers = new HashMap<>();
        for (ClassInfo classInfo : classes) {
            if (classInfo.getSchoolId() == null) {
                continue;
            }
            ClassMember selfMember = memberMap.get(classInfo.getId());
            if (selfMember != null && selfMember.getUserId() != null) {
                schoolToUsers.computeIfAbsent(classInfo.getSchoolId(), k -> new HashSet<>())
                        .add(selfMember.getUserId());
            }
        }
        Map<String, SchoolMember> result = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : schoolToUsers.entrySet()) {
            for (SchoolMember m : schoolService.getMembersByUserIds(entry.getKey(), entry.getValue()).values()) {
                result.put(entry.getKey() + ":" + m.getUserId(), m);
            }
        }
        return result;
    }

    /**
     * 填充班级冻结与接管相关字段（详情/个人班级列表场景，携带针对当前用户的个性化判定）
     */
    private void fillTakeoverFields(ClassDetailResponse response, ClassInfo classInfo, User currentUser,
            ClassMember selfMember) {
        boolean frozen = classAccessResolver.isClassFrozen(classInfo);
        response.setFrozen(frozen);
        response.setOwnerActive(classAccessResolver.isOwnerActive(classInfo));
        response.setTakeoverAutoApprove(schoolService.isClassTakeoverAutoApprove(classInfo.getSchoolId()));

        boolean pending = false;
        boolean canTakeover = false;
        if (frozen && currentUser != null && !Objects.equals(classInfo.getOwnerId(), currentUser.getId())) {
            // 是否已提交待审核的接管申请
            QueryWrapper<ClassTakeoverApplication> pendingQuery = new QueryWrapper<>();
            pendingQuery.eq("class_id", classInfo.getId())
                    .eq("applicant_id", currentUser.getId())
                    .eq("status", 0);
            pending = classTakeoverApplicationMapper.selectCount(pendingQuery) > 0;
            // 仅该校老师可申请接管（本班已接管的老师无需再接管）
            canTakeover = !pending
                    && schoolService.isSchoolTeacher(classInfo.getSchoolId(), currentUser.getId());
        }
        response.setTakeoverPending(pending);
        response.setCanTakeover(canTakeover);
    }

    public Page<ClassMemberResponse> getClassMembers(Integer classId, Integer pageNum, Integer pageSize) {
        User currentUser = userLookup.requireCurrentUser();

        // 验证班级是否存在
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 权限校验：拥有查看全部班级权限者不受限制，普通用户只能查询自己创建或加入的班级
        boolean isAdmin = userLookup.hasPermission(currentUser, PermissionNodes.CLASS_VIEW_ALL);
        boolean isClassMember = classAccessResolver.isClassMember(classId, currentUser.getId());

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

        // 批量查询学校成员身份（姓名与学工号）
        final Map<Integer, SchoolMember> schoolMemberMap = classAccessResolver.loadSchoolMembers(classInfo, userIds);

        // 查询班级教师总数（role=1 表示教师）
        QueryWrapper<ClassMember> teacherCountQuery = new QueryWrapper<>();
        teacherCountQuery.eq("class_id", classId).eq("role", 1);
        long teacherCount = classMemberMapper.selectCount(teacherCountQuery);

        List<ClassMemberResponse> responses = pagedResult.getRecords().stream()
                .map(member -> {
                    // 从缓存中获取用户信息
                    User user = userMap.get(member.getUserId());
                    String userName = user != null ? user.getUsername() : "未知";

                    SchoolMember schoolMember = schoolMemberMap.get(member.getUserId());

                    // 确定角色（学校学生获得班级管理员权限时显示为课代表）
                    String role = classAccessResolver.getUserRole(classInfo, member, schoolMember);

                    return new ClassMemberResponse(
                            member.getId(),
                            member.getUserId(),
                            userName,
                            schoolMember != null ? schoolMember.getRealName() : null,
                            schoolMember != null ? schoolMember.getStaffNo() : null,
                            role,
                            classAccessResolver.getUserRoleCode(classInfo, member),
                            member.getJoinTime(),
                            teacherCount);
                })
                .toList();

        // 构建分页结果
        Page<ClassMemberResponse> page = new Page<>(pageNum, pageSize, pagedResult.getTotal());
        page.setRecords(responses);
        return page;
    }

    public List<ClassMemberResponse> getAllClassMembers(Integer classId) {
        ClassInfo classInfo = classInfoMapper.selectById(classId);

        QueryWrapper<ClassMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("class_id", classId);

        // 不分页，查询所有成员
        List<ClassMember> members = classMemberMapper.selectList(queryWrapper);

        // 批量查询用户信息
        List<Integer> userIds = members.stream().map(ClassMember::getUserId).distinct().toList();
        final Map<Integer, User> userMap;
        if (!userIds.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", userIds);
            userMap = userMapper.selectList(userQuery).stream().collect(Collectors.toMap(User::getId, u -> u));
        } else {
            userMap = new HashMap<>();
        }

        // 批量查询学校成员身份（姓名与学工号）
        final Map<Integer, SchoolMember> schoolMemberMap = classAccessResolver.loadSchoolMembers(classInfo, userIds);

        return members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    String userName = user != null ? user.getUsername() : "未知";

                    SchoolMember schoolMember = schoolMemberMap.get(member.getUserId());

                    // 确定角色（学校学生获得班级管理员权限时显示为课代表）
                    String role = classAccessResolver.getUserRole(classInfo, member, schoolMember);

                    return new ClassMemberResponse(
                            member.getId(),
                            member.getUserId(),
                            userName,
                            schoolMember != null ? schoolMember.getRealName() : null,
                            schoolMember != null ? schoolMember.getStaffNo() : null,
                            role,
                            classAccessResolver.getUserRoleCode(classInfo, member),
                            member.getJoinTime(),
                            null);
                })
                .toList();
    }
}
