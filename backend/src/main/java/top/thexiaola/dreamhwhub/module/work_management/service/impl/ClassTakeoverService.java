package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

        return toResponse(application, classInfo, autoApprove);
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
        return toResponse(application, classInfoMapper.selectById(classId), null);
    }

    /**
     * 分页/列表查询某学校的待审核接管申请（学校管理员）
     */
    public List<ClassTakeoverResponse> listSchoolTakeovers(Integer schoolId, Integer status) {
        User currentUser = userLookup.requireCurrentUser();
        if (!schoolService.isSchoolManager(schoolId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有学校管理员可以查看接管申请", null);
        }

        // 该校下的班级
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.eq("school_id", schoolId).select("id");
        List<Integer> classIds = classInfoMapper.selectList(classQuery).stream()
                .map(ClassInfo::getId)
                .toList();
        if (classIds.isEmpty()) {
            return Collections.emptyList();
        }

        QueryWrapper<ClassTakeoverApplication> query = new QueryWrapper<>();
        query.in("class_id", classIds);
        if (status != null) {
            query.eq("status", status);
        }
        query.orderByDesc("create_time");
        return takeoverMapper.selectList(query).stream()
                .map(app -> toResponse(app, classInfoMapper.selectById(app.getClassId()), null))
                .toList();
    }

    /**
     * 当前用户可接管的冻结班级（本人是所属学校老师的那些）
     */
    public List<ClassTakeoverResponse> listAvailableTakeovers() {
        User currentUser = userLookup.requireCurrentUser();

        // 我作为老师/学校管理员加入的学校
        List<SchoolMember> memberships = schoolService.getMembershipsByUserId(currentUser.getId()).stream()
                .filter(m -> m.getRole() != null && m.getRole() >= 1)
                .toList();
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> schoolIds = memberships.stream().map(SchoolMember::getSchoolId).distinct().toList();

        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.in("school_id", schoolIds);
        List<ClassInfo> classes = classInfoMapper.selectList(classQuery);

        // 我已提交待审核申请的班级，不再重复展示
        QueryWrapper<ClassTakeoverApplication> myPendingQuery = new QueryWrapper<>();
        myPendingQuery.eq("applicant_id", currentUser.getId()).eq("status", 0).select("class_id");
        Set<Integer> myPendingClassIds = takeoverMapper.selectList(myPendingQuery).stream()
                .map(ClassTakeoverApplication::getClassId)
                .collect(Collectors.toSet());

        return classes.stream()
                // 冻结、且我不是创建者、且尚未申请
                .filter(classAccessResolver::isClassFrozen)
                .filter(c -> !Objects.equals(c.getOwnerId(), currentUser.getId()))
                .filter(c -> !myPendingClassIds.contains(c.getId()))
                .map(c -> {
                    ClassTakeoverApplication placeholder = new ClassTakeoverApplication();
                    placeholder.setClassId(c.getId());
                    placeholder.setApplicantId(currentUser.getId());
                    placeholder.setStatus(0);
                    placeholder.setCreateTime(LocalDateTime.now());
                    return toResponse(placeholder, c, null);
                })
                .toList();
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

        // 该班其余待处理接管申请一并作废，避免重复转移
        QueryWrapper<ClassTakeoverApplication> others = new QueryWrapper<>();
        others.eq("class_id", classId).eq("status", 0).ne("applicant_id", newOwnerId);
        List<ClassTakeoverApplication> pending = takeoverMapper.selectList(others);
        for (ClassTakeoverApplication app : pending) {
            app.setStatus(2);
            app.setReviewTime(LocalDateTime.now());
            app.setReviewComment("班级已被其他老师接管");
            takeoverMapper.updateById(app);
        }

        log.info("Class {} ownership transferred to user {}", classId, newOwnerId);
    }

    private ClassTakeoverResponse toResponse(ClassTakeoverApplication application, ClassInfo classInfo,
            Boolean autoApprove) {
        ClassTakeoverResponse response = new ClassTakeoverResponse();
        response.setId(application.getId());
        response.setClassId(application.getClassId());
        response.setApplicantId(application.getApplicantId());
        response.setStatus(application.getStatus());
        response.setReviewerId(application.getReviewerId());
        response.setReviewTime(application.getReviewTime());
        response.setReviewComment(application.getReviewComment());
        response.setCreateTime(application.getCreateTime());

        if (classInfo != null) {
            response.setClassName(classInfo.getClassName());
        }

        User applicant = application.getApplicantId() != null
                ? userMapper.selectById(application.getApplicantId())
                : null;
        if (applicant != null) {
            response.setApplicantUsername(applicant.getUsername());
        }
        if (application.getReviewerId() != null) {
            User reviewer = userMapper.selectById(application.getReviewerId());
            if (reviewer != null) {
                response.setReviewerUsername(reviewer.getUsername());
            }
        }

        // 申请人学校身份（姓名与学工号）
        if (classInfo != null && classInfo.getSchoolId() != null && application.getApplicantId() != null) {
            SchoolMember schoolMember = schoolService.getMember(classInfo.getSchoolId(), application.getApplicantId());
            if (schoolMember != null) {
                response.setApplicantName(schoolMember.getRealName());
                response.setApplicantNo(schoolMember.getStaffNo());
            }
        }
        return response;
    }
}
