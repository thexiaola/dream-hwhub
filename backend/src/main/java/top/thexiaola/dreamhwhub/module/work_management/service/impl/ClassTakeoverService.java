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
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassTakeoverApplication;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassTakeoverApplicationMapper;
import top.thexiaola.dreamhwhub.module.work_management.vo.ClassTakeoverResponse;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 班级接管服务
 * <p>
 * 班级创建者的教师身份被解除后，班级进入「冻结」状态：原创建者与班级老师均无法再管理，
 * 邀请码失效、不再接纳新学生。本校其他老师可申请接管：
 * 学校配置为自动同意（默认）时立即转移所有权，否则生成待学校管理员审核的申请。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassTakeoverService {

    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final ClassTakeoverApplicationMapper takeoverMapper;
    private final UserMapper userMapper;
    private final SchoolService schoolService;
    private final ClassAccessResolver classAccessResolver;
    private final UserLookupSupport userLookup;

    /**
     * 当前用户申请接管某冻结班级
     *
     * @param classId 班级 ID
     * @return 接管申请响应（自动同意时状态为已通过）
     */
    @Transactional(rollbackFor = Exception.class)
    public ClassTakeoverResponse applyTakeover(Integer classId) {
        User currentUser = userLookup.requireCurrentUser();

        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        // 仅冻结班级可被接管
        if (!classAccessResolver.isClassFrozen(classInfo)) {
            throw new BusinessException(BusinessErrorCode.TAKEOVER_NOT_ALLOWED, "该班级当前无需接管", null);
        }

        // 创建者本人不能接管自己的班级（应等待学校身份恢复）
        if (Objects.equals(classInfo.getOwnerId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.TAKEOVER_NOT_ALLOWED,
                    "你是该班级的创建者，无法接管自己的班级", null);
        }

        // 只有该班级所属学校的老师（含学校管理员）可以接管
        if (!schoolService.isSchoolTeacher(classInfo.getSchoolId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    "只有该班级所属学校的老师可以申请接管", null);
        }

        // 已有待处理的申请则不再重复提交
        QueryWrapper<ClassTakeoverApplication> pendingQuery = new QueryWrapper<>();
        pendingQuery.eq("class_id", classId)
                .eq("applicant_id", currentUser.getId())
                .eq("status", 0);
        if (takeoverMapper.selectCount(pendingQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.TAKEOVER_ALREADY_APPLIED, "你已提交过接管申请，请等待处理", null);
        }

        boolean autoApprove = schoolService.isClassTakeoverAutoApprove(classInfo.getSchoolId());

        ClassTakeoverApplication application = new ClassTakeoverApplication();
        application.setClassId(classId);
        application.setApplicantId(currentUser.getId());
        application.setStatus(autoApprove ? 1 : 0);
        application.setCreateTime(LocalDateTime.now());
        if (autoApprove) {
            application.setReviewTime(LocalDateTime.now());
            application.setReviewComment("系统自动同意");
        }
        takeoverMapper.insert(application);

        if (autoApprove) {
            transferOwnership(classInfo, currentUser.getId());
        }

        return toResponse(application, classInfo);
    }

    /**
     * 查询我在某班级的接管申请（最近一条）
     */
    public ClassTakeoverResponse getMyTakeover(Integer classId) {
        User currentUser = userLookup.requireCurrentUser();
        QueryWrapper<ClassTakeoverApplication> query = new QueryWrapper<>();
        query.eq("class_id", classId)
                .eq("applicant_id", currentUser.getId())
                .orderByDesc("create_time")
                .last("LIMIT 1");
        ClassTakeoverApplication application = takeoverMapper.selectOne(query);
        if (application == null) {
            return null;
        }
        return toResponse(application, classInfoMapper.selectById(classId));
    }

    /**
     * 分页/列表查询某学校的待审核接管申请（学校管理员）
     */
    public List<ClassTakeoverResponse> listSchoolTakeovers(Integer schoolId, Integer status) {
        User currentUser = userLookup.requireCurrentUser();
        if (!schoolService.isSchoolManager(schoolId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有学校管理员可以查看接管申请", null);
        }

        // 该校下的班级（一次取回，供批量转换取班级名与所属学校）
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.eq("school_id", schoolId);
        Map<Integer, ClassInfo> classMap = classInfoMapper.selectList(classQuery).stream()
                .collect(Collectors.toMap(ClassInfo::getId, c -> c));
        if (classMap.isEmpty()) {
            return Collections.emptyList();
        }

        QueryWrapper<ClassTakeoverApplication> query = new QueryWrapper<>();
        query.in("class_id", classMap.keySet());
        if (status != null) {
            query.eq("status", status);
        }
        query.orderByDesc("create_time");
        return toResponses(takeoverMapper.selectList(query), classMap);
    }

    /**
     * 当前用户可接管的冻结班级（本人是所属学校老师的那些）
     * <p>
     * 所有筛选条件（我所在学校、班级已冻结、非我创建、我未申请过）全部下推数据库，
     * 避免先取出全部班级再在内存里过滤。
     */
    public List<ClassTakeoverResponse> listAvailableTakeovers() {
        User currentUser = userLookup.requireCurrentUser();

        // 我作为老师/学校管理员加入的学校（角色下限下推到数据库）
        List<Integer> schoolIds = schoolService.getMembershipsByUserId(currentUser.getId(), 1).stream()
                .map(SchoolMember::getSchoolId)
                .distinct()
                .toList();
        if (schoolIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 冻结判定与后端 isClassFrozen 完全一致，直接等价改写为 SQL：
        // 创建者非平台管理员，且创建者在该班所属学校已不是老师（role < 1 或已非成员）
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.in("school_id", schoolIds)
                .ne("owner_id", currentUser.getId())
                .apply("NOT EXISTS (SELECT 1 FROM user u WHERE u.id = class_info.owner_id AND u.is_op = 1)")
                .apply("NOT EXISTS (SELECT 1 FROM school_member sm WHERE sm.school_id = class_info.school_id "
                        + "AND sm.user_id = class_info.owner_id AND sm.role >= 1)")
                // 我已提交待审核申请的班级，不再重复展示
                .apply("NOT EXISTS (SELECT 1 FROM class_takeover_application ta WHERE ta.class_id = class_info.id "
                        + "AND ta.applicant_id = {0} AND ta.status = 0)", currentUser.getId());
        List<ClassInfo> classes = classInfoMapper.selectList(classQuery);
        if (classes.isEmpty()) {
            return Collections.emptyList();
        }

        // 以当前用户为「拟申请人」构造占位申请，批量转响应（一次补齐用户名与学校身份）
        LocalDateTime now = LocalDateTime.now();
        List<ClassTakeoverApplication> placeholders = new ArrayList<>(classes.size());
        Map<Integer, ClassInfo> classMap = new HashMap<>();
        for (ClassInfo c : classes) {
            ClassTakeoverApplication placeholder = new ClassTakeoverApplication();
            placeholder.setClassId(c.getId());
            placeholder.setApplicantId(currentUser.getId());
            placeholder.setStatus(0);
            placeholder.setCreateTime(now);
            placeholders.add(placeholder);
            classMap.put(c.getId(), c);
        }
        return toResponses(placeholders, classMap);
    }

    /**
     * 学校管理员审核接管申请
     *
     * @param applicationId 申请 ID
     * @param approved      是否通过
     * @param comment       审核意见
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewTakeover(Integer applicationId, Boolean approved, String comment) {
        User currentUser = userLookup.requireCurrentUser();

        ClassTakeoverApplication application = takeoverMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_CLASS, "接管申请不存在", null);
        }
        if (!Integer.valueOf(0).equals(application.getStatus())) {
            throw new BusinessException(BusinessErrorCode.ALREADY_IN_CLASS, "该申请已处理", null);
        }

        ClassInfo classInfo = classInfoMapper.selectById(application.getClassId());
        if (classInfo == null) {
            throw new BusinessException(BusinessErrorCode.CLASS_NOT_FOUND, "班级不存在", null);
        }

        if (!schoolService.isSchoolManager(classInfo.getSchoolId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有学校管理员可以审核接管申请", null);
        }

        application.setStatus(Boolean.TRUE.equals(approved) ? 1 : 2);
        application.setReviewerId(currentUser.getId());
        application.setReviewTime(LocalDateTime.now());
        application.setReviewComment(comment);
        takeoverMapper.updateById(application);

        if (Boolean.TRUE.equals(approved)) {
            transferOwnership(classInfo, application.getApplicantId());
        }
    }

    /**
     * 将班级所有权转移给接管人，并清理该班其余待处理接管申请
     */
    private void transferOwnership(ClassInfo classInfo, Integer newOwnerId) {
        Integer classId = classInfo.getId();

        // 接管人加入班级并成为老师
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId).eq("user_id", newOwnerId);
        ClassMember member = classMemberMapper.selectOne(memberQuery);
        if (member == null) {
            member = new ClassMember();
            member.setClassId(classId);
            member.setUserId(newOwnerId);
            member.setRole(1);
            member.setJoinTime(LocalDateTime.now());
            classMemberMapper.insert(member);
        } else {
            member.setRole(1);
            classMemberMapper.updateById(member);
        }

        // 转移所有权
        classInfo.setOwnerId(newOwnerId);
        classInfoMapper.updateById(classInfo);

        // 该班其余待处理接管申请一并作废，避免重复转移。
        // 一次 SQL 批量置为已拒绝，避免逐条 updateById
        UpdateWrapper<ClassTakeoverApplication> others = new UpdateWrapper<>();
        others.eq("class_id", classId)
                .eq("status", 0)
                .ne("applicant_id", newOwnerId)
                .set("status", 2)
                .set("review_time", LocalDateTime.now())
                .set("review_comment", "班级已被其他老师接管");
        takeoverMapper.update(null, others);

        log.info("Class {} ownership transferred to user {}", classId, newOwnerId);
    }

    /** 单条申请转响应（用于单个查询场景） */
    private ClassTakeoverResponse toResponse(ClassTakeoverApplication application, ClassInfo classInfo) {
        Map<Integer, ClassInfo> classMap = classInfo == null
                ? Collections.emptyMap()
                : Map.of(classInfo.getId(), classInfo);
        return toResponses(List.of(application), classMap).get(0);
    }

    /**
     * 批量把接管申请转为响应：一次取回全部申请人/审核人用户名，按学校批量取回申请人学校身份，
     * 避免逐条查询（N+1）。
     *
     * @param applications 申请列表
     * @param classMap     班级 ID -> 班级信息（用于取班级名与班级所属学校）
     */
    private List<ClassTakeoverResponse> toResponses(List<ClassTakeoverApplication> applications,
            Map<Integer, ClassInfo> classMap) {
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量取回涉及的用户（申请人 + 审核人）
        Set<Integer> userIds = new HashSet<>();
        for (ClassTakeoverApplication app : applications) {
            if (app.getApplicantId() != null) {
                userIds.add(app.getApplicantId());
            }
            if (app.getReviewerId() != null) {
                userIds.add(app.getReviewerId());
            }
        }
        Map<Integer, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 申请人的学校内身份：按「班级所属学校」批量取回（同一学校一次查询）
        Map<Integer, Set<Integer>> schoolToApplicants = new HashMap<>();
        for (ClassTakeoverApplication app : applications) {
            ClassInfo classInfo = classMap.get(app.getClassId());
            if (classInfo == null || classInfo.getSchoolId() == null || app.getApplicantId() == null) {
                continue;
            }
            schoolToApplicants.computeIfAbsent(classInfo.getSchoolId(), k -> new HashSet<>())
                    .add(app.getApplicantId());
        }
        Map<String, SchoolMember> memberMap = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : schoolToApplicants.entrySet()) {
            for (SchoolMember m : schoolService.getMembersByUserIds(entry.getKey(), entry.getValue()).values()) {
                memberMap.put(entry.getKey() + ":" + m.getUserId(), m);
            }
        }

        List<ClassTakeoverResponse> result = new ArrayList<>(applications.size());
        for (ClassTakeoverApplication application : applications) {
            ClassTakeoverResponse response = new ClassTakeoverResponse();
            response.setId(application.getId());
            response.setClassId(application.getClassId());
            response.setApplicantId(application.getApplicantId());
            response.setStatus(application.getStatus());
            response.setReviewerId(application.getReviewerId());
            response.setReviewTime(application.getReviewTime());
            response.setReviewComment(application.getReviewComment());
            response.setCreateTime(application.getCreateTime());

            ClassInfo classInfo = classMap.get(application.getClassId());
            if (classInfo != null) {
                response.setClassName(classInfo.getClassName());
            }

            User applicant = application.getApplicantId() == null ? null : userMap.get(application.getApplicantId());
            if (applicant != null) {
                response.setApplicantUsername(applicant.getUsername());
            }
            if (application.getReviewerId() != null) {
                User reviewer = userMap.get(application.getReviewerId());
                if (reviewer != null) {
                    response.setReviewerUsername(reviewer.getUsername());
                }
            }

            // 申请人学校身份（姓名与学工号）
            if (classInfo != null && classInfo.getSchoolId() != null && application.getApplicantId() != null) {
                SchoolMember member = memberMap.get(classInfo.getSchoolId() + ":" + application.getApplicantId());
                if (member != null) {
                    response.setApplicantName(member.getRealName());
                    response.setApplicantNo(member.getStaffNo());
                }
            }
            result.add(response);
        }
        return result;
    }
}
