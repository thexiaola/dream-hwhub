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
import top.thexiaola.dreamhwhub.module.school.dto.ApproveSchoolJoinRequest;
import top.thexiaola.dreamhwhub.module.school.dto.AssignSchoolAdminRequest;
import top.thexiaola.dreamhwhub.module.school.dto.CreateSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.dto.JoinSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.dto.SetSchoolJoinApprovalRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolMemberIdentityRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolMemberRoleRequest;
import top.thexiaola.dreamhwhub.module.school.dto.UpdateSchoolRequest;
import top.thexiaola.dreamhwhub.module.school.entity.School;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolJoinApplication;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolJoinApplicationMapper;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMapper;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMemberMapper;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolDetailResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolJoinApplicationResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolMemberResponse;
import top.thexiaola.dreamhwhub.module.school.vo.SchoolVO;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        if (myMember == null && currentUser != null) {
            SchoolJoinApplication latest = selectLatestApplication(school.getId(), currentUser.getId());
            if (latest != null) {
                myApplicationStatus = latest.getStatus();
            }
        }

        return new SchoolDetailResponse(
                school.getId(),
                school.getSchoolName(),
                school.getDescription(),
                school.getAllowJoinWithoutApproval(),
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
                myApplicationStatus);
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

    /**
     * 将学校实体转换为简要响应对象
     */
    private SchoolVO toSchoolVO(School school) {
        return new SchoolVO(
                school.getId(),
                school.getSchoolName(),
                school.getDescription(),
                school.getAllowJoinWithoutApproval(),
                countMembers(school.getId(), null),
                school.getCreateTime());
    }

    @Override
    public Page<SchoolVO> listSchools(String keyword, Integer pageNum, Integer pageSize) {
        QueryWrapper<School> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like("school_name", keyword.trim());
        }
        queryWrapper.orderByDesc("create_time");

        Page<School> schoolPage = schoolMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        List<SchoolVO> records = schoolPage.getRecords().stream()
                .map(this::toSchoolVO)
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

        if (!userLookup.hasPermission(currentUser, PermissionNodes.SCHOOL_DISSOLVE)) {
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
    public List<SchoolDetailResponse> getMySchools() {
        User currentUser = userLookup.requireCurrentUser();

        QueryWrapper<SchoolMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", currentUser.getId()).orderByDesc("join_time");
        List<SchoolMember> members = schoolMemberMapper.selectList(memberQuery);
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> schoolIds = members.stream().map(SchoolMember::getSchoolId).distinct().toList();
        QueryWrapper<School> schoolQuery = new QueryWrapper<>();
        schoolQuery.in("id", schoolIds);
        Map<Integer, School> schoolMap = schoolMapper.selectList(schoolQuery).stream()
                .collect(Collectors.toMap(School::getId, s -> s));

        return members.stream()
                .map(SchoolMember::getSchoolId)
                .map(schoolMap::get)
                .filter(Objects::nonNull)
                .map(school -> buildDetail(school, currentUser))
                .toList();
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

        boolean approved = Boolean.TRUE.equals(request.getApproved());
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
        application.setReviewerId(currentUser.getId());
        application.setReviewTime(LocalDateTime.now());
        application.setReviewComment(request.getComment());
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

        for (ClassInfo classInfo : classes) {
            workSubmissionCleaner.cleanupClassSubmissions(classInfo.getId(), userId);

            QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
            memberQuery.eq("class_id", classInfo.getId()).eq("user_id", userId);
            classMemberMapper.delete(memberQuery);
        }

        return classes.stream().map(ClassInfo::getId).toList();
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
