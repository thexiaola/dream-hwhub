package top.thexiaola.dreamhwhub.module.school.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import top.thexiaola.dreamhwhub.module.school.constant.SchoolMemberRole;
import top.thexiaola.dreamhwhub.module.school.dto.*;
import top.thexiaola.dreamhwhub.module.school.entity.School;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolJoinApplication;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolJoinApplicationMapper;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMapper;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMemberMapper;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.school.vo.*;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassJoinApplication;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassUserInvitation;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassJoinApplicationMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassUserInvitationMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.support.WorkSubmissionCleaner;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学校管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private final SchoolMapper schoolMapper;
    private final SchoolMemberMapper schoolMemberMapper;
    private final SchoolJoinApplicationMapper schoolJoinApplicationMapper;
    private final UserMapper userMapper;
    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final ClassJoinApplicationMapper classJoinApplicationMapper;
    private final ClassUserInvitationMapper classUserInvitationMapper;
    private final WorkSubmissionCleaner workSubmissionCleaner;
    private final UserLookupSupport userLookup;

    /**
     * 查询学校，不存在则抛出异常
     */
    private School getSchoolOrThrow(Integer schoolId) {
        School school = schoolId == null ? null : schoolMapper.selectById(schoolId);
        if (school == null) {
            throw new BusinessException(BusinessErrorCode.SCHOOL_NOT_FOUND, "学校不存在", null);
        }
        return school;
    }

    /**
     * 查询用户在学校的成员记录
     */
    private SchoolMember getMemberOrNull(Integer schoolId, Integer userId) {
        if (schoolId == null || userId == null) {
            return null;
        }
        QueryWrapper<SchoolMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("school_id", schoolId).eq("user_id", userId);
        return schoolMemberMapper.selectOne(queryWrapper);
    }

    /**
     * 校验当前用户具备学校管理权限（该校的学校管理员或平台管理员）
     */
    private void requireSchoolManager(User user, Integer schoolId) {
        if (userLookup.hasPermission(user, PermissionNodes.SCHOOL_UPDATE)) {
            return;
        }
        SchoolMember member = getMemberOrNull(schoolId, user.getId());
        if (member != null && Objects.equals(member.getRole(), SchoolMemberRole.ADMIN)) {
            return;
        }
        throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有学校管理员可以执行该操作", null);
    }

    /**
     * 转义 LIKE 通配符，避免用户输入的 %、_、! 被当作通配符（配合 SQL 中的 ESCAPE '!'）
     */
    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * 校验学工号在同一学校内未被占用
     *
     * @param excludeUserId 需要排除的用户 ID，无则传 null
     */
    private void validateStaffNoAvailable(Integer schoolId, String staffNo, Integer excludeUserId) {
        if (StrUtil.isBlank(staffNo)) {
            return;
        }
        QueryWrapper<SchoolMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("school_id", schoolId).eq("staff_no", StrUtil.trim(staffNo));
        if (excludeUserId != null) {
            memberQuery.ne("user_id", excludeUserId);
        }
        if (schoolMemberMapper.selectCount(memberQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.DUPLICATE_STAFF_NO, "该学工号在该学校已被占用", null);
        }

        // 未被成员占用但已被他人待审核申请占用时同样拒绝
        QueryWrapper<SchoolJoinApplication> applicationQuery = new QueryWrapper<>();
        applicationQuery.eq("school_id", schoolId)
                .eq("applicant_no", StrUtil.trim(staffNo))
                .eq("status", 0);
        if (excludeUserId != null) {
            applicationQuery.ne("applicant_id", excludeUserId);
        }
        if (schoolJoinApplicationMapper.selectCount(applicationQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.DUPLICATE_STAFF_NO, "该学工号已被其他待审核申请使用", null);
        }
    }

    /**
     * 校验学校名称未被占用
     */
    private void validateSchoolNameAvailable(String schoolName, Integer excludeSchoolId) {
        QueryWrapper<School> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("school_name", schoolName);
        if (excludeSchoolId != null) {
            queryWrapper.ne("id", excludeSchoolId);
        }
        if (schoolMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(BusinessErrorCode.SCHOOL_NAME_EXISTS, "学校名称已被占用", null);
        }
    }

    /**
     * 统计学校内指定角色的成员数量
     *
     * @param role 角色代码，null 表示统计全部成员
     */
    private long countMembers(Integer schoolId, Integer role) {
        QueryWrapper<SchoolMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("school_id", schoolId);
        if (role != null) {
            queryWrapper.eq("role", role);
        }
        return schoolMemberMapper.selectCount(queryWrapper);
    }

    /**
     * 批量统计多所学校的成员构成（一次 GROUP BY school_id, role）。
     *
     * @param schoolIds 学校 ID 集合
     * @return 学校 ID -> [成员总数, 学校管理员数, 老师数, 学生数]
     */
    private Map<Integer, long[]> countMemberRolesBySchools(Collection<Integer> schoolIds) {
        Map<Integer, long[]> result = new HashMap<>();
        if (schoolIds == null || schoolIds.isEmpty()) {
            return result;
        }
        QueryWrapper<SchoolMember> query = new QueryWrapper<>();
        query.in("school_id", schoolIds)
                .select("school_id", "role", "COUNT(*) AS cnt")
                .groupBy("school_id", "role");
        for (Map<String, Object> row : schoolMemberMapper.selectMaps(query)) {
            Object schoolId = row.get("school_id");
            Object role = row.get("role");
            Object cnt = row.get("cnt");
            if (!(schoolId instanceof Number sid) || !(cnt instanceof Number count)) {
                continue;
            }
            long[] arr = result.computeIfAbsent(sid.intValue(), k -> new long[4]);
            arr[0] += count.longValue();
            if (role instanceof Number r) {
                switch (r.intValue()) {
                    case SchoolMemberRole.ADMIN -> arr[1] += count.longValue();
                    case SchoolMemberRole.TEACHER -> arr[2] += count.longValue();
                    case SchoolMemberRole.STUDENT -> arr[3] += count.longValue();
                    default -> { }
                }
            }
        }
        return result;
    }

    /**
     * 批量统计多所学校的班级数（一次 GROUP BY school_id）。
     *
     * @param schoolIds 学校 ID 集合
     * @return 学校 ID -> 班级数
     */
    private Map<Integer, Long> countClassesBySchools(Collection<Integer> schoolIds) {
        Map<Integer, Long> result = new HashMap<>();
        if (schoolIds == null || schoolIds.isEmpty()) {
            return result;
        }
        QueryWrapper<ClassInfo> query = new QueryWrapper<>();
        query.in("school_id", schoolIds)
                .select("school_id", "COUNT(*) AS cnt")
                .groupBy("school_id");
        for (Map<String, Object> row : classInfoMapper.selectMaps(query)) {
            Object schoolId = row.get("school_id");
            Object cnt = row.get("cnt");
            if (schoolId instanceof Number sid && cnt instanceof Number count) {
                result.put(sid.intValue(), count.longValue());
            }
        }
        return result;
    }

    /**
     * 构建学校详情响应
     */
    private SchoolDetailResponse buildDetail(School school, User currentUser) {
        long memberCount = countMembers(school.getId(), null);
        long adminCount = countMembers(school.getId(), SchoolMemberRole.ADMIN);
        long teacherCount = countMembers(school.getId(), SchoolMemberRole.TEACHER);
        long studentCount = countMembers(school.getId(), SchoolMemberRole.STUDENT);

        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.eq("school_id", school.getId());
        long classCount = classInfoMapper.selectCount(classQuery);

        SchoolMember myMember = currentUser == null ? null : getMemberOrNull(school.getId(), currentUser.getId());
        Integer myApplicationStatus = null;
        String myApplicationComment = null;
        if (myMember == null && currentUser != null) {
            SchoolJoinApplication latest = selectLatestApplication(school.getId(), currentUser.getId());
            if (latest != null) {
                myApplicationStatus = latest.getStatus();
                myApplicationComment = latest.getReviewComment();
            }
        }

        // 待审核数量只对能管理该校的人有意义
        Long pendingApplicationCount = null;
        if (currentUser != null && canManageSchool(currentUser, myMember)) {
            QueryWrapper<SchoolJoinApplication> pendingQuery = new QueryWrapper<>();
            pendingQuery.eq("school_id", school.getId()).eq("status", 0);
            pendingApplicationCount = schoolJoinApplicationMapper.selectCount(pendingQuery);
        }

        return new SchoolDetailResponse(
                school.getId(),
                school.getSchoolName(),
                school.getDescription(),
                school.getAllowJoinWithoutApproval(),
                isClassTakeoverAutoApprove(school.getId()),
                memberCount,
                adminCount,
                teacherCount,
                studentCount,
                classCount,
                school.getCreateTime(),
                myMember != null,
                myMember != null ? myMember.getRole() : null,
                myMember != null ? SchoolMemberRole.nameOf(myMember.getRole()) : null,
                myMember != null ? myMember.getStaffNo() : null,
                myMember != null ? myMember.getRealName() : null,
                myApplicationStatus,
                pendingApplicationCount,
                myApplicationComment);
    }

    /**
     * 批量构建学校详情（用于「我的学校」列表）：
     * 成员统计、班级数、我的成员身份、我最新申请、待审核数均批量取回，避免逐校查询。
     *
     * @param schools     学校列表（调用者已按同一用户过滤）
     * @param currentUser 当前用户
     * @return 学校详情列表（顺序与入参一致）
     */
    private List<SchoolDetailResponse> buildDetails(List<School> schools, User currentUser) {
        if (schools.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> schoolIds = schools.stream().map(School::getId).toList();

        Map<Integer, long[]> roleStats = countMemberRolesBySchools(schoolIds);
        Map<Integer, Long> classCounts = countClassesBySchools(schoolIds);
        Map<Integer, Boolean> takeoverAutoMap = getClassTakeoverAutoApprove(schoolIds);

        // 我在这些学校的成员身份（一次取回，以 schoolId 为键）
        Map<Integer, SchoolMember> myMemberMap = loadMyMembershipsBySchools(currentUser.getId(), schoolIds);

        // 我未加入的学校：一次取回最新申请，按学校建索引
        Map<Integer, SchoolJoinApplication> myApplicationMap = new HashMap<>();
        Set<Integer> noMemberSchoolIds = schoolIds.stream()
                .filter(id -> !myMemberMap.containsKey(id))
                .collect(Collectors.toSet());
        if (!noMemberSchoolIds.isEmpty()) {
            QueryWrapper<SchoolJoinApplication> appQuery = new QueryWrapper<>();
            appQuery.eq("applicant_id", currentUser.getId())
                    .in("school_id", noMemberSchoolIds)
                    .orderByDesc("create_time");
            for (SchoolJoinApplication app : schoolJoinApplicationMapper.selectList(appQuery)) {
                myApplicationMap.putIfAbsent(app.getSchoolId(), app);
            }
        }

        // 我作为管理员的学校（用于给出待审核数）：权限节点只判定一次，避免逐校查库；
        // 一次聚合各校待审核数
        boolean isSchoolUpdateAdmin = userLookup.hasPermission(currentUser, PermissionNodes.SCHOOL_UPDATE);
        Set<Integer> manageableSchoolIds = schools.stream()
                .filter(s -> canManageSchool(isSchoolUpdateAdmin, myMemberMap.get(s.getId())))
                .map(School::getId)
                .collect(Collectors.toSet());
        Map<Integer, Long> pendingCountMap = countPendingApplicationsBySchools(manageableSchoolIds);

        List<SchoolDetailResponse> result = new ArrayList<>(schools.size());
        for (School school : schools) {
            long[] stats = roleStats.getOrDefault(school.getId(), new long[4]);
            SchoolMember myMember = myMemberMap.get(school.getId());
            SchoolJoinApplication myApplication = myApplicationMap.get(school.getId());
            result.add(new SchoolDetailResponse(
                    school.getId(),
                    school.getSchoolName(),
                    school.getDescription(),
                    school.getAllowJoinWithoutApproval(),
                    takeoverAutoMap.getOrDefault(school.getId(), true),
                    stats[0],
                    stats[1],
                    stats[2],
                    stats[3],
                    classCounts.getOrDefault(school.getId(), 0L),
                    school.getCreateTime(),
                    myMember != null,
                    myMember != null ? myMember.getRole() : null,
                    myMember != null ? SchoolMemberRole.nameOf(myMember.getRole()) : null,
                    myMember != null ? myMember.getStaffNo() : null,
                    myMember != null ? myMember.getRealName() : null,
                    myApplication != null ? myApplication.getStatus() : null,
                    pendingCountMap.get(school.getId()),
                    myApplication != null ? myApplication.getReviewComment() : null));
        }
        return result;
    }

    /**
     * 批量统计多所学校的待审核加入申请数（一次 GROUP BY school_id）
     */
    private Map<Integer, Long> countPendingApplicationsBySchools(Collection<Integer> schoolIds) {
        Map<Integer, Long> result = new HashMap<>();
        if (schoolIds == null || schoolIds.isEmpty()) {
            return result;
        }
        QueryWrapper<SchoolJoinApplication> query = new QueryWrapper<>();
        query.in("school_id", schoolIds)
                .eq("status", 0)
                .select("school_id", "COUNT(*) AS cnt")
                .groupBy("school_id");
        for (Map<String, Object> row : schoolJoinApplicationMapper.selectMaps(query)) {
            Object schoolId = row.get("school_id");
            Object cnt = row.get("cnt");
            if (schoolId instanceof Number sid && cnt instanceof Number count) {
                result.put(sid.intValue(), count.longValue());
            }
        }
        return result;
    }

    /**
     * 批量查询用户在指定的多所学校中的成员记录，返回以 schoolId 为键的映射。
     */
    private Map<Integer, SchoolMember> loadMyMembershipsBySchools(Integer userId, Collection<Integer> schoolIds) {
        if (userId == null || schoolIds == null || schoolIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<SchoolMember> query = new QueryWrapper<>();
        query.eq("user_id", userId).in("school_id", schoolIds);
        return schoolMemberMapper.selectList(query).stream()
                .collect(Collectors.toMap(SchoolMember::getSchoolId, m -> m, (a, b) -> a));
    }

    /**
     * 是否可管理该校：拥有学校管理权限（平台管理员），或在该校担任学校管理员
     */
    private boolean canManageSchool(User user, SchoolMember myMember) {
        return canManageSchool(userLookup.hasPermission(user, PermissionNodes.SCHOOL_UPDATE), myMember);
    }

    /**
     * 是否可管理该校（权限节点已由调用方判定，避免批量场景逐校查库）
     */
    private boolean canManageSchool(boolean hasSchoolUpdatePermission, SchoolMember myMember) {
        if (hasSchoolUpdatePermission) {
            return true;
        }
        return myMember != null && Objects.equals(myMember.getRole(), SchoolMemberRole.ADMIN);
    }

    /**
     * 查询用户在某学校最新的一条加入申请
     */
    private SchoolJoinApplication selectLatestApplication(Integer schoolId, Integer applicantId) {
        QueryWrapper<SchoolJoinApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("school_id", schoolId)
                .eq("applicant_id", applicantId)
                .orderByDesc("create_time")
                .last("LIMIT 1");
        return schoolJoinApplicationMapper.selectOne(queryWrapper);
    }

    /**
     * 将申请实体批量转换为响应对象（批量补齐学校名与用户名）
     */
    private List<SchoolJoinApplicationResponse> toApplicationResponses(List<SchoolJoinApplication> applications) {
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> schoolIds = applications.stream()
                .map(SchoolJoinApplication::getSchoolId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, String> schoolNames = new HashMap<>();
        if (!schoolIds.isEmpty()) {
            QueryWrapper<School> schoolQuery = new QueryWrapper<>();
            schoolQuery.in("id", schoolIds);
            for (School school : schoolMapper.selectList(schoolQuery)) {
                schoolNames.put(school.getId(), school.getSchoolName());
            }
        }

        Set<Integer> userIds = new HashSet<>();
        for (SchoolJoinApplication application : applications) {
            if (application.getApplicantId() != null) {
                userIds.add(application.getApplicantId());
            }
            if (application.getReviewerId() != null) {
                userIds.add(application.getReviewerId());
            }
        }
        Map<Integer, String> usernames = new HashMap<>();
        if (!userIds.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", userIds).select("id", "username");
            for (User user : userMapper.selectList(userQuery)) {
                usernames.put(user.getId(), user.getUsername());
            }
        }

        List<SchoolJoinApplicationResponse> responses = new ArrayList<>(applications.size());
        for (SchoolJoinApplication application : applications) {
            responses.add(new SchoolJoinApplicationResponse(
                    application.getId(),
                    application.getSchoolId(),
                    schoolNames.get(application.getSchoolId()),
                    application.getApplicantId(),
                    usernames.get(application.getApplicantId()),
                    application.getApplicantName(),
                    application.getApplicantNo(),
                    application.getStatus(),
                    application.getReviewerId(),
                    usernames.get(application.getReviewerId()),
                    application.getReviewComment(),
                    application.getCreateTime(),
                    application.getReviewTime()));
        }
        return responses;
    }

    /**
     * 将学校成员实体批量转换为响应对象（批量补齐用户名）
     */
    private List<SchoolMemberResponse> toMemberResponses(List<SchoolMember> members) {
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> userIds = members.stream()
                .map(SchoolMember::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, String> usernames = new HashMap<>();
        if (!userIds.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", userIds).select("id", "username");
            for (User user : userMapper.selectList(userQuery)) {
                usernames.put(user.getId(), user.getUsername());
            }
        }

        List<SchoolMemberResponse> responses = new ArrayList<>(members.size());
        for (SchoolMember member : members) {
            responses.add(toMemberResponse(member, usernames.get(member.getUserId())));
        }
        return responses;
    }

    /**
     * 将单个学校成员实体转换为响应对象
     */
    private SchoolMemberResponse toMemberResponse(SchoolMember member, String username) {
        return new SchoolMemberResponse(
                member.getId(),
                member.getSchoolId(),
                member.getUserId(),
                username,
                member.getRealName(),
                member.getStaffNo(),
                member.getRole(),
                SchoolMemberRole.nameOf(member.getRole()),
                member.getJoinTime());
    }

    @Override
    public Page<SchoolVO> listSchools(String keyword, Integer pageNum, Integer pageSize) {
        QueryWrapper<School> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like("school_name", keyword.trim());
        }
        queryWrapper.orderByDesc("create_time");

        Page<School> schoolPage = schoolMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        List<School> schools = schoolPage.getRecords();

        // 成员数与接管策略一次性批量取回，避免逐校查询（N+1）
        List<Integer> schoolIds = schools.stream().map(School::getId).toList();
        Map<Integer, long[]> roleStats = countMemberRolesBySchools(schoolIds);
        Map<Integer, Boolean> takeoverAutoMap = getClassTakeoverAutoApprove(schoolIds);

        List<SchoolVO> records = schools.stream()
                .map(school -> new SchoolVO(
                        school.getId(),
                        school.getSchoolName(),
                        school.getDescription(),
                        school.getAllowJoinWithoutApproval(),
                        takeoverAutoMap.getOrDefault(school.getId(), true),
                        roleStats.getOrDefault(school.getId(), new long[4])[0],
                        school.getCreateTime()))
                .toList();

        Page<SchoolVO> page = new Page<>(pageNum, pageSize, schoolPage.getTotal());
        page.setRecords(records);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchoolDetailResponse createSchool(CreateSchoolRequest request) {
        User currentUser = userLookup.requireCurrentUser();

        String schoolName = StrUtil.trim(request.getSchoolName());
        validateSchoolNameAvailable(schoolName, null);

        School school = new School();
        school.setSchoolName(schoolName);
        school.setDescription(StrUtil.trim(request.getDescription()));
        school.setAllowJoinWithoutApproval(Boolean.TRUE.equals(request.getAllowJoinWithoutApproval()));
        schoolMapper.insert(school);

        log.info("School created, id: {}, name: {}", school.getId(), schoolName);
        return buildDetail(school, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchoolDetailResponse updateSchool(Integer schoolId, UpdateSchoolRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        School school = getSchoolOrThrow(schoolId);
        requireSchoolManager(currentUser, schoolId);

        String schoolName = StrUtil.trim(request.getSchoolName());
        validateSchoolNameAvailable(schoolName, schoolId);

        school.setSchoolName(schoolName);
        school.setDescription(StrUtil.trim(request.getDescription()));
        schoolMapper.updateById(school);

        return buildDetail(school, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveSchool(Integer schoolId) {
        User currentUser = userLookup.requireCurrentUser();
        getSchoolOrThrow(schoolId);

        // 解散学校仅限平台管理员：school:dissolve 是「可授予的权限节点」，
        // 若只校验节点，被授予该节点的学校管理员也能解散，因此这里强制要求 OP 身份
        if (!userLookup.isPlatformAdmin(currentUser)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有平台管理员可以解散学校", null);
        }

        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.eq("school_id", schoolId);
        if (classInfoMapper.selectCount(classQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.SCHOOL_HAS_CLASSES,
                    "学校下仍存在班级，请先解散这些班级", null);
        }

        QueryWrapper<SchoolMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("school_id", schoolId);
        schoolMemberMapper.delete(memberQuery);

        QueryWrapper<SchoolJoinApplication> applicationQuery = new QueryWrapper<>();
        applicationQuery.eq("school_id", schoolId);
        schoolJoinApplicationMapper.delete(applicationQuery);

        schoolMapper.deleteById(schoolId);
        log.info("School dissolved, id: {}", schoolId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchoolMemberResponse assignSchoolAdmin(Integer schoolId, AssignSchoolAdminRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        getSchoolOrThrow(schoolId);

        if (!userLookup.hasPermission(currentUser, PermissionNodes.SCHOOL_ADMIN_ASSIGN)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有平台管理员可以指派学校管理员", null);
        }

        User target = userLookup.findByAccountOrThrow(request.getUserAccount());
        SchoolMember member = getMemberOrNull(schoolId, target.getId());

        if (Boolean.TRUE.equals(request.getAssigned())) {
            if (member == null) {
                String staffNo = StrUtil.trim(request.getStaffNo());
                String realName = StrUtil.trim(request.getRealName());
                if (StrUtil.isBlank(staffNo) || StrUtil.isBlank(realName)) {
                    throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING,
                            "该用户尚未加入学校，请填写其学工号与姓名", null);
                }
                validateStaffNoAvailable(schoolId, staffNo, null);

                member = new SchoolMember();
                member.setSchoolId(schoolId);
                member.setUserId(target.getId());
                member.setStaffNo(staffNo);
                member.setRealName(realName);
                member.setRole(SchoolMemberRole.ADMIN);
                member.setJoinTime(LocalDateTime.now());
                schoolMemberMapper.insert(member);

                // 该用户成为学校成员后，其待审核的加入申请不再需要
                QueryWrapper<SchoolJoinApplication> applicationQuery = new QueryWrapper<>();
                applicationQuery.eq("school_id", schoolId)
                        .eq("applicant_id", target.getId())
                        .eq("status", 0);
                schoolJoinApplicationMapper.delete(applicationQuery);
            } else {
                member.setRole(SchoolMemberRole.ADMIN);
                schoolMemberMapper.updateById(member);

                // 新指派的学校管理员，其所在班级的成员角色同步提升为班级管理员
                syncClassManagerRole(schoolId, member.getUserId(), 1);
            }
        } else {
            if (member == null) {
                throw new BusinessException(BusinessErrorCode.NOT_IN_SCHOOL, "该用户不是该学校的成员", null);
            }
            member.setRole(SchoolMemberRole.TEACHER);
            schoolMemberMapper.updateById(member);
        }

        return toMemberResponse(member, target.getUsername());
    }

    @Override
    public SchoolDetailResponse getSchoolDetail(Integer schoolId) {
        School school = getSchoolOrThrow(schoolId);
        return buildDetail(school, UserUtils.getCurrentUser());
    }

    @Override
    public List<SchoolDetailResponse> getMySchools(Integer minRoleCode, String keyword) {
        User currentUser = userLookup.requireCurrentUser();

        QueryWrapper<SchoolMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", currentUser.getId());
        // 角色下限下推到数据库，用于取「我可建班的学校」等子集
        if (minRoleCode != null) {
            memberQuery.ge("role", minRoleCode);
        }
        // 学校名称关键字下推到数据库：用子查询命中我加入且名称匹配的学校，
        // 避免先取出全部成员记录再在内存里筛选
        if (StrUtil.isNotBlank(keyword)) {
            memberQuery.apply("school_id IN (SELECT id FROM school WHERE school_name LIKE {0} ESCAPE '!')",
                    "%" + escapeLike(keyword.trim()) + "%");
        }
        memberQuery.orderByDesc("join_time");
        List<SchoolMember> members = schoolMemberMapper.selectList(memberQuery);
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> schoolIds = members.stream().map(SchoolMember::getSchoolId).distinct().toList();
        QueryWrapper<School> schoolQuery = new QueryWrapper<>();
        schoolQuery.in("id", schoolIds);
        Map<Integer, School> schoolMap = schoolMapper.selectList(schoolQuery).stream()
                .collect(Collectors.toMap(School::getId, s -> s));

        // 按「我的学校」列表顺序收集学校实体，统计与身份信息一次性批量构建（避免逐校查询）
        List<School> schools = members.stream()
                .map(SchoolMember::getSchoolId)
                .map(schoolMap::get)
                .filter(Objects::nonNull)
                .toList();
        return buildDetails(schools, currentUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchoolJoinApplicationResponse joinSchool(Integer schoolId, JoinSchoolRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        School school = getSchoolOrThrow(schoolId);

        if (getMemberOrNull(schoolId, currentUser.getId()) != null) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_SCHOOL, "你已经在该学校中", null);
        }

        String realName = StrUtil.trim(request.getRealName());
        String staffNo = StrUtil.trim(request.getStaffNo());
        validateStaffNoAvailable(schoolId, staffNo, currentUser.getId());

        // 同一用户在同一学校只保留一条申请（表上有 (school_id, applicant_id) 唯一键），
        // 重新提交时先清掉历史申请，含已被拒绝的记录
        QueryWrapper<SchoolJoinApplication> oldQuery = new QueryWrapper<>();
        oldQuery.eq("school_id", schoolId)
                .eq("applicant_id", currentUser.getId());
        schoolJoinApplicationMapper.delete(oldQuery);

        // 学校关闭审核时直接加入，无需学校管理员验证
        if (Boolean.TRUE.equals(school.getAllowJoinWithoutApproval())) {
            SchoolMember member = new SchoolMember();
            member.setSchoolId(schoolId);
            member.setUserId(currentUser.getId());
            member.setStaffNo(staffNo);
            member.setRealName(realName);
            member.setRole(SchoolMemberRole.STUDENT);
            member.setJoinTime(LocalDateTime.now());
            schoolMemberMapper.insert(member);

            SchoolJoinApplication approved = new SchoolJoinApplication();
            approved.setSchoolId(schoolId);
            approved.setApplicantId(currentUser.getId());
            approved.setApplicantName(realName);
            approved.setApplicantNo(staffNo);
            approved.setStatus(1);
            approved.setReviewerId(currentUser.getId());
            approved.setReviewTime(LocalDateTime.now());
            approved.setCreateTime(LocalDateTime.now());
            return toApplicationResponses(List.of(approved)).get(0);
        }

        SchoolJoinApplication application = new SchoolJoinApplication();
        application.setSchoolId(schoolId);
        application.setApplicantId(currentUser.getId());
        application.setApplicantName(realName);
        application.setApplicantNo(staffNo);
        application.setStatus(0);
        schoolJoinApplicationMapper.insert(application);

        return toApplicationResponses(List.of(application)).get(0);
    }

    @Override
    public SchoolJoinApplicationResponse getMyJoinApplication(Integer schoolId) {
        User currentUser = userLookup.requireCurrentUser();
        SchoolJoinApplication application = selectLatestApplication(schoolId, currentUser.getId());
        if (application == null) {
            return null;
        }
        return toApplicationResponses(List.of(application)).get(0);
    }

    @Override
    public Page<SchoolJoinApplicationResponse> getJoinApplications(Integer schoolId, Integer status,
            Integer pageNum, Integer pageSize) {
        User currentUser = userLookup.requireCurrentUser();
        requireSchoolManager(currentUser, schoolId);

        QueryWrapper<SchoolJoinApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("school_id", schoolId);
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("create_time");

        Page<SchoolJoinApplication> applicationPage = schoolJoinApplicationMapper.selectPage(
                new Page<>(pageNum, pageSize), queryWrapper);

        Page<SchoolJoinApplicationResponse> page = new Page<>(pageNum, pageSize, applicationPage.getTotal());
        page.setRecords(toApplicationResponses(applicationPage.getRecords()));
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveJoinApplication(Integer schoolId, ApproveSchoolJoinRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        getSchoolOrThrow(schoolId);
        requireSchoolManager(currentUser, schoolId);

        SchoolJoinApplication application = schoolJoinApplicationMapper.selectById(request.getApplicationId());
        if (application == null || !Objects.equals(application.getSchoolId(), schoolId)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "加入申请不存在", null);
        }
        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(BusinessErrorCode.DUPLICATE_APPLICATION, "该申请已处理", null);
        }

        reviewOne(schoolId, application, Boolean.TRUE.equals(request.getApproved()), request.getComment(),
                currentUser.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchReviewResult batchApproveJoinApplications(Integer schoolId, BatchApproveSchoolJoinRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        getSchoolOrThrow(schoolId);
        requireSchoolManager(currentUser, schoolId);

        // 一次批量取出待审核申请，避免在循环里逐个 selectById（N 次往返）；
        // 去重后逐个处理，重复 ID 不会重复审核
        List<Integer> applicationIds = request.getApplicationIds() == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(request.getApplicationIds()));
        if (applicationIds.isEmpty()) {
            return new BatchReviewResult(0, 0);
        }
        Map<Integer, SchoolJoinApplication> applicationMap = schoolJoinApplicationMapper
                .selectByIds(applicationIds).stream()
                .collect(Collectors.toMap(SchoolJoinApplication::getId, a -> a));

        boolean approved = Boolean.TRUE.equals(request.getApproved());
        int handled = 0;
        int skipped = 0;
        for (Integer applicationId : applicationIds) {
            SchoolJoinApplication application = applicationMap.get(applicationId);
            // 跳过不属于该校或已被处理过的申请
            if (application == null || !Objects.equals(application.getSchoolId(), schoolId)
                    || !Integer.valueOf(0).equals(application.getStatus())) {
                skipped++;
                continue;
            }
            try {
                reviewOne(schoolId, application, approved, request.getComment(), currentUser.getId());
                handled++;
            } catch (BusinessException e) {
                // 单条不满足审核条件（如学工号已被占用）时跳过，不影响其余申请
                log.warn("Skip school {} application {} in batch review: {}", schoolId, applicationId, e.getMessage());
                skipped++;
            }
        }
        return new BatchReviewResult(handled, skipped);
    }

    @Override
    public Page<SchoolJoinApplicationResponse> listAllJoinApplications(Integer schoolId, Integer status,
            Integer pageNum, Integer pageSize) {
        QueryWrapper<SchoolJoinApplication> queryWrapper = new QueryWrapper<>();
        if (schoolId != null) {
            queryWrapper.eq("school_id", schoolId);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("create_time");

        Page<SchoolJoinApplication> applicationPage = schoolJoinApplicationMapper.selectPage(
                new Page<>(pageNum, pageSize), queryWrapper);

        Page<SchoolJoinApplicationResponse> page = new Page<>(pageNum, pageSize, applicationPage.getTotal());
        page.setRecords(toApplicationResponses(applicationPage.getRecords()));
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchReviewResult batchApproveAllJoinApplications(BatchApproveSchoolJoinRequest request) {
        User currentUser = userLookup.requireCurrentUser();

        // 一次批量取出申请，避免在循环里逐个 selectById（N 次往返）
        List<Integer> applicationIds = request.getApplicationIds() == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(request.getApplicationIds()));
        if (applicationIds.isEmpty()) {
            return new BatchReviewResult(0, 0);
        }
        Map<Integer, SchoolJoinApplication> applicationMap = schoolJoinApplicationMapper
                .selectByIds(applicationIds).stream()
                .collect(Collectors.toMap(SchoolJoinApplication::getId, a -> a));

        boolean approved = Boolean.TRUE.equals(request.getApproved());
        int handled = 0;
        int skipped = 0;
        for (Integer applicationId : applicationIds) {
            SchoolJoinApplication application = applicationMap.get(applicationId);
            // 只处理仍处于待审核的申请，其余跳过
            if (application == null || !Integer.valueOf(0).equals(application.getStatus())) {
                skipped++;
                continue;
            }
            try {
                // 逐条按申请所属学校校验操作人权限：正常情况下平台管理员已由接口注解放行，
                // 这里保留校验是为了将来接口权限放宽后仍按学校隔离
                requireSchoolManager(currentUser, application.getSchoolId());
                reviewOne(application.getSchoolId(), application, approved, request.getComment(), currentUser.getId());
                handled++;
            } catch (BusinessException e) {
                log.warn("Skip application {} in batch review: {}", applicationId, e.getMessage());
                skipped++;
            }
        }
        return new BatchReviewResult(handled, skipped);
    }

    /**
     * 审核单条加入申请：写入审核结果，通过时补建学校成员
     */
    private void reviewOne(Integer schoolId, SchoolJoinApplication application, boolean approved,
            String comment, Integer reviewerId) {
        SchoolMember existingMember = getMemberOrNull(schoolId, application.getApplicantId());

        // 通过前先确认申请携带完整的姓名与学工号，且学工号未被占用
        String applicantName = null;
        String applicantNo = null;
        if (approved && existingMember == null) {
            applicantName = StrUtil.trim(application.getApplicantName());
            applicantNo = StrUtil.trim(application.getApplicantNo());
            if (StrUtil.isBlank(applicantName) || StrUtil.isBlank(applicantNo)) {
                throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "该申请缺少姓名或学工号，无法通过", null);
            }
            validateStaffNoAvailable(schoolId, applicantNo, application.getApplicantId());
        }

        application.setStatus(approved ? 1 : 2);
        application.setReviewerId(reviewerId);
        application.setReviewTime(LocalDateTime.now());
        // 通过申请无需说明原因，只有拒绝时才记录审核意见
        application.setReviewComment(approved ? null : comment);
        schoolJoinApplicationMapper.updateById(application);

        if (approved && existingMember == null) {
            SchoolMember member = new SchoolMember();
            member.setSchoolId(schoolId);
            member.setUserId(application.getApplicantId());
            member.setStaffNo(applicantNo);
            member.setRealName(applicantName);
            member.setRole(SchoolMemberRole.STUDENT);
            member.setJoinTime(LocalDateTime.now());
            schoolMemberMapper.insert(member);
        }
    }

    /**
     * 同步用户在本校各班级中的成员角色
     * 学校老师与学校管理员自动拥有班级管理员权限，其学校身份变更时需要一并调整班级角色
     *
     * @param targetRole 目标班级角色：1-拥有班级管理员权限，0-普通成员
     */
    private void syncClassManagerRole(Integer schoolId, Integer userId, int targetRole) {
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.eq("school_id", schoolId).select("id");
        List<Integer> classIds = classInfoMapper.selectList(classQuery).stream()
                .map(ClassInfo::getId)
                .toList();
        if (classIds.isEmpty()) {
            return;
        }

        UpdateWrapper<ClassMember> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("class_id", classIds)
                .eq("user_id", userId)
                .ne("role", targetRole)
                .set("role", targetRole);
        classMemberMapper.update(null, updateWrapper);
    }

    @Override
    public Page<SchoolMemberResponse> getSchoolMembers(Integer schoolId, String keyword,
            Integer pageNum, Integer pageSize) {
        User currentUser = userLookup.requireCurrentUser();
        requireSchoolManager(currentUser, schoolId);

        QueryWrapper<SchoolMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("school_id", schoolId);
        if (StrUtil.isNotBlank(keyword)) {
            String trimmedKeyword = keyword.trim();
            List<Integer> matchedUserIds = selectUserIdsByKeyword(trimmedKeyword);
            queryWrapper.and(w -> {
                w.like("real_name", trimmedKeyword).or().like("staff_no", trimmedKeyword);
                if (!matchedUserIds.isEmpty()) {
                    w.or().in("user_id", matchedUserIds);
                }
            });
        }
        queryWrapper.orderByDesc("role").orderByAsc("id");

        Page<SchoolMember> memberPage = schoolMemberMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);

        Page<SchoolMemberResponse> page = new Page<>(pageNum, pageSize, memberPage.getTotal());
        page.setRecords(toMemberResponses(memberPage.getRecords()));
        return page;
    }

    /**
     * 按用户名或邮箱关键字查询匹配的用户 ID
     */
    private List<Integer> selectUserIdsByKeyword(String keyword) {
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.like("username", keyword).or().like("email", keyword);
        userQuery.select("id");
        return userMapper.selectList(userQuery).stream()
                .map(User::getId)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(Integer schoolId, UpdateSchoolMemberRoleRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        getSchoolOrThrow(schoolId);
        requireSchoolManager(currentUser, schoolId);

        SchoolMember member = getMemberOrNull(schoolId, request.getUserId());
        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_SCHOOL, "该用户不是该学校的成员", null);
        }
        if (Objects.equals(member.getUserId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "不能修改自己的学校角色", null);
        }
        if (Objects.equals(member.getRole(), SchoolMemberRole.ADMIN)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "学校管理员由平台管理员指派，不能在此调整", null);
        }

        Integer previousRole = member.getRole();
        member.setRole(request.getRole());
        schoolMemberMapper.updateById(member);

        // 学校老师与学校管理员在本校各班级中自动拥有班级管理员权限，身份变更时同步班级角色；
        // 仅在学校身份确实发生老师/学生切换时同步，避免把学生已有的课代表身份一并抹掉
        boolean wasTeacherRole = previousRole != null && previousRole >= SchoolMemberRole.TEACHER;
        boolean isTeacherRole = request.getRole() != null && request.getRole() >= SchoolMemberRole.TEACHER;
        if (isTeacherRole && !wasTeacherRole) {
            syncClassManagerRole(schoolId, member.getUserId(), 1);
        } else if (!isTeacherRole && wasTeacherRole) {
            syncClassManagerRole(schoolId, member.getUserId(), 0);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberIdentity(Integer schoolId, UpdateSchoolMemberIdentityRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        getSchoolOrThrow(schoolId);
        requireSchoolManager(currentUser, schoolId);

        SchoolMember member = getMemberOrNull(schoolId, request.getUserId());
        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_SCHOOL, "该用户不是该学校的成员", null);
        }

        String staffNo = StrUtil.trim(request.getStaffNo());
        validateStaffNoAvailable(schoolId, staffNo, member.getUserId());

        member.setRealName(StrUtil.trim(request.getRealName()));
        member.setStaffNo(staffNo);
        schoolMemberMapper.updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Integer schoolId, Integer userId) {
        User currentUser = userLookup.requireCurrentUser();
        getSchoolOrThrow(schoolId);
        requireSchoolManager(currentUser, schoolId);

        if (Objects.equals(currentUser.getId(), userId)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "不能移出自己，如需离开请使用退出学校", null);
        }

        SchoolMember member = getMemberOrNull(schoolId, userId);
        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_SCHOOL, "该用户不是该学校的成员", null);
        }
        if (Objects.equals(member.getRole(), SchoolMemberRole.ADMIN)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "学校管理员由平台管理员指派，不能在此移出", null);
        }

        List<Integer> classIds = removeFromSchoolClasses(schoolId, userId);
        clearPendingClassRequests(classIds, userId);
        schoolMemberMapper.deleteById(member.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveSchool(Integer schoolId) {
        User currentUser = userLookup.requireCurrentUser();
        getSchoolOrThrow(schoolId);

        SchoolMember member = getMemberOrNull(schoolId, currentUser.getId());
        if (member == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_SCHOOL, "你不是该学校的成员", null);
        }
        if (Objects.equals(member.getRole(), SchoolMemberRole.ADMIN)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "学校管理员需先由平台管理员取消管理员身份后才能退出学校", null);
        }

        List<Integer> classIds = removeFromSchoolClasses(schoolId, currentUser.getId());
        clearPendingClassRequests(classIds, currentUser.getId());
        schoolMemberMapper.deleteById(member.getId());
    }

    /**
     * 将成员从学校内的全部班级中移出，并清理其在这些班级的作业提交
     *
     * @return 学校下的班级 ID 列表，供后续清理待处理申请使用
     * @throws BusinessException 该成员是学校内某班级的创建者时抛出，需先转让或解散该班级
     */
    private List<Integer> removeFromSchoolClasses(Integer schoolId, Integer userId) {
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.eq("school_id", schoolId);
        List<ClassInfo> classes = classInfoMapper.selectList(classQuery);
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }

        // 班级创建者不能随学校移出而移除，需先转让或解散其班级
        for (ClassInfo classInfo : classes) {
            if (Objects.equals(classInfo.getOwnerId(), userId)) {
                throw new BusinessException(BusinessErrorCode.CREATOR_CANNOT_LEAVE,
                        "该成员是班级「" + classInfo.getClassName() + "」的创建者，请先转让或解散该班级", null);
            }
        }

        List<Integer> classIds = classes.stream().map(ClassInfo::getId).toList();

        // 一次清理该用户在这些班级的全部作业提交（不按班级循环）
        workSubmissionCleaner.cleanupClassSubmissions(classIds, userId);

        // 一次删除其在这些班级的成员记录（单条 SQL）
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.in("class_id", classIds).eq("user_id", userId);
        classMemberMapper.delete(memberQuery);

        return classIds;
    }

    /**
     * 清除用户在该校所有班级中尚未处理的入班申请与邀请，避免移出学校后留下无法处理的待办
     */
    private void clearPendingClassRequests(List<Integer> classIds, Integer userId) {
        if (classIds.isEmpty()) {
            return;
        }

        QueryWrapper<ClassJoinApplication> applicationQuery = new QueryWrapper<>();
        applicationQuery.in("class_id", classIds).eq("applicant_id", userId).eq("status", 0);
        classJoinApplicationMapper.delete(applicationQuery);

        QueryWrapper<ClassUserInvitation> invitationQuery = new QueryWrapper<>();
        invitationQuery.in("class_id", classIds).eq("invitee_id", userId).eq("status", 0);
        classUserInvitationMapper.delete(invitationQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setJoinApprovalRequired(Integer schoolId, SetSchoolJoinApprovalRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        School school = getSchoolOrThrow(schoolId);
        requireSchoolManager(currentUser, schoolId);

        school.setAllowJoinWithoutApproval(Boolean.TRUE.equals(request.getAllowJoinWithoutApproval()));
        schoolMapper.updateById(school);
    }

    @Override
    public boolean isSchoolTeacher(Integer schoolId, Integer userId) {
        SchoolMember member = getMemberOrNull(schoolId, userId);
        return member != null && member.getRole() != null && member.getRole() >= SchoolMemberRole.TEACHER;
    }

    @Override
    public boolean isSchoolManager(Integer schoolId, Integer userId) {
        User user = userMapper.selectById(userId);
        return canManageSchool(user, getMemberOrNull(schoolId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setClassTakeoverAutoApprove(Integer schoolId, SetClassTakeoverApprovalRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        School school = getSchoolOrThrow(schoolId);
        requireSchoolManager(currentUser, schoolId);

        school.setAutoApproveClassTakeover(Boolean.TRUE.equals(request.getAutoApproveClassTakeover()));
        schoolMapper.updateById(school);
    }

    @Override
    public boolean isClassTakeoverAutoApprove(Integer schoolId) {
        if (schoolId == null) {
            return true;
        }
        School school = schoolMapper.selectById(schoolId);
        // 缺省（历史数据为 null）视为自动同意
        return school == null || !Boolean.FALSE.equals(school.getAutoApproveClassTakeover());
    }

    @Override
    public Map<Integer, Boolean> getClassTakeoverAutoApprove(Collection<Integer> schoolIds) {
        if (schoolIds == null || schoolIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<School> query = new QueryWrapper<>();
        query.in("id", schoolIds).select("id", "auto_approve_class_takeover");
        Map<Integer, Boolean> result = new HashMap<>();
        for (School school : schoolMapper.selectList(query)) {
            // 缺省（历史数据为 null）视为自动同意
            result.put(school.getId(), !Boolean.FALSE.equals(school.getAutoApproveClassTakeover()));
        }
        return result;
    }

    @Override
    public void requireSchoolExists(Integer schoolId) {
        getSchoolOrThrow(schoolId);
    }

    @Override
    public Map<Integer, String> getSchoolNames(Collection<Integer> schoolIds) {
        if (schoolIds == null || schoolIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<School> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", schoolIds).select("id", "school_name");
        return schoolMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(School::getId, School::getSchoolName, (a, b) -> a));
    }

    @Override
    public SchoolMember getMember(Integer schoolId, Integer userId) {
        return getMemberOrNull(schoolId, userId);
    }

    @Override
    public boolean isSchoolMember(Integer schoolId, Integer userId) {
        return getMemberOrNull(schoolId, userId) != null;
    }

    @Override
    public List<SchoolMember> getMembershipsByUserId(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<SchoolMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return schoolMemberMapper.selectList(queryWrapper);
    }

    @Override
    public List<SchoolMember> getMembershipsByUserId(Integer userId, Integer minRoleCode) {
        if (userId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<SchoolMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        // 角色下限下推到数据库，避免取出全部成员记录后再在内存里过滤
        if (minRoleCode != null) {
            queryWrapper.ge("role", minRoleCode);
        }
        return schoolMemberMapper.selectList(queryWrapper);
    }

    @Override
    public Map<Integer, SchoolMember> getMembersByUserIds(Integer schoolId, Collection<Integer> userIds) {
        if (schoolId == null || userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<SchoolMember> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("school_id", schoolId).in("user_id", userIds);
        return schoolMemberMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(SchoolMember::getUserId, m -> m, (a, b) -> a));
    }
}
