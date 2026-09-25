package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.message.service.SiteMessageService;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.work_management.constant.WorkType;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.ExamConfigDto;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.entity.*;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ExamSessionMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ExamViolationMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkQuestionService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkQuestionVO;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;
import top.thexiaola.dreamhwhub.support.session.UserUtils;
import top.thexiaola.dreamhwhub.support.validation.FileUploadValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 作业服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkServiceImpl implements WorkService {

    // 文件存储目录（运行目录下的 attachments/work，作业附件）
    private static final String UPLOAD_DIR = "attachments/work/";

    private final WorkMapper workMapper;
    private final WorkAttachmentMapper workAttachmentMapper;
    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper;
    private final ExamSessionMapper examSessionMapper;
    private final ExamViolationMapper examViolationMapper;
    private final ClassService classService;
    private final UserMapper userMapper;
    private final UserLookupSupport userLookup;
    private final ClassInfoMapper classInfoMapper;
    private final SchoolService schoolService;
    private final SiteMessageService siteMessageService;
    private final WorkQuestionService workQuestionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkResponse createWork(CreateWorkRequest request) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 检查权限（只有班级老师可以发布作业）
        if (!classService.isTeacher(request.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以发布作业", null);
        }

        // 创建作业
        WorkInfo workInfo = new WorkInfo();
        workInfo.setTitle(request.getTitle());
        workInfo.setDescription(request.getDescription());
        workInfo.setPublisherId(currentUser.getId());
        workInfo.setClassId(request.getClassId());
        workInfo.setDeadline(request.getDeadline());
        workInfo.setTotalScore(request.getTotalScore());
        workInfo.setAllowLateSubmit(request.getAllowLateSubmit() != null ? request.getAllowLateSubmit() : true);
        workInfo.setPublishTime(LocalDateTime.now());
        workInfo.setCreateTime(LocalDateTime.now());
        workInfo.setUpdateTime(LocalDateTime.now());

        // 类型与考试配置（作业默认 homework，不带反作弊）
        applyWorkTypeAndExamConfig(workInfo, request.getWorkType(), request.getExamConfig());

        workMapper.insert(workInfo);

        // 保存结构化题目（可选）：含题目时标记 hasQuestions，学生将逐题作答、客观题自动评判
        boolean hasQuestions = request.getQuestions() != null && !request.getQuestions().isEmpty();
        if (hasQuestions) {
            workQuestionService.replaceQuestions(workInfo.getId(), request.getQuestions());
            workInfo.setHasQuestions(true);
            workMapper.updateById(workInfo);
        }

        // 保存附件（直接上传的文件）
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            saveWorkAttachmentsDirectly(currentUser.getId(), workInfo.getId(), request.getAttachments());
        }

        // 向该班学生发站内信（按班级所属学校隔离）；通知失败不影响作业发布本身
        try {
            ClassInfo classInfo = classInfoMapper.selectById(workInfo.getClassId());
            siteMessageService.notifyWorkPublished(
                    workInfo.getClassId(),
                    classInfo != null ? classInfo.getClassName() : null,
                    workInfo.getId(),
                    workInfo.getTitle(),
                    currentUser.getId());
        } catch (Exception e) {
            log.warn("Failed to notify work published for work {}: {}", workInfo.getId(), e.getMessage());
        }

        // 转换为WorkResponse
        return convertToWorkResponse(workInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkResponse updateWork(UpdateWorkRequest request) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询作业
        WorkInfo workInfo = workMapper.selectById(request.getId());
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查权限（只有班级老师可以修改作业）
        if (!classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以修改作业", null);
        }

        // 如果作业已发布，不允许修改发布时间
        LocalDateTime now = LocalDateTime.now();
        boolean isPublished = workInfo.getPublishTime() != null && !now.isBefore(workInfo.getPublishTime());
        if (isPublished && request.getPublishTime() != null) {
            throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, "已发布的作业不能修改发布时间", null);
        }

        // 如果已有学生提交，不允许修改总分
        if (!request.getTotalScore().equals(workInfo.getTotalScore())) {
            QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
            submissionQuery.eq("work_id", workInfo.getId())
                          .eq("is_deleted", false);
            long submissionCount = workSubmissionMapper.selectCount(submissionQuery);
            if (submissionCount > 0) {
                throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, 
                        "已有学生提交作业，无法修改总分", null);
            }
        }

        // 更新截止时间（允许设置为任意时间；请求为 null 表示清除截止时间，即永久有效）
        workInfo.setDeadline(request.getDeadline());

        // 更新作业
        workInfo.setTitle(request.getTitle());
        workInfo.setDescription(request.getDescription());
        workInfo.setTotalScore(request.getTotalScore());
        if (request.getAllowLateSubmit() != null) {
            workInfo.setAllowLateSubmit(request.getAllowLateSubmit());
        }
        if (request.getPublishTime() != null) {
            workInfo.setPublishTime(request.getPublishTime());
        }
        workInfo.setUpdateTime(LocalDateTime.now());

        // 类型与考试配置（仅当请求显式携带时更新）
        if (request.getWorkType() != null || request.getExamConfig() != null) {
            applyWorkTypeAndExamConfig(workInfo, request.getWorkType(), request.getExamConfig());
        }

        // 题目整体替换（仅当请求显式携带 questions 字段时）：空数组表示清除题目
        if (request.getQuestions() != null) {
            workQuestionService.replaceQuestions(workInfo.getId(), request.getQuestions());
            workInfo.setHasQuestions(!request.getQuestions().isEmpty());
        }

        workMapper.updateById(workInfo);
        
        // 处理附件更新（直接上传）
        handleAttachmentUpdates(workInfo.getId(), request.getRemovedAttachmentIds(), request.getAttachments());
        
        // 转换为WorkResponse
        return convertToWorkResponse(workInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWork(Integer workId) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询作业
        WorkInfo workInfo = workMapper.selectById(workId);
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查权限（只有班级老师可以删除作业）
        if (!classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以删除作业", null);
        }

        // 计算当前状态
        Integer currentStatus = calculateWorkStatus(workInfo);
        
        // 级联删除所有关联数据
        cascadeDeleteWork(workId);
        
    }

    @Override
    public WorkResponse getWorkById(Integer workId) {
        WorkInfo workInfo = workMapper.selectById(workId);
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查权限：仅本班成员（老师/助理/学生）或管理员可查看
        User currentUser = UserUtils.getCurrentUser();
        Integer status = calculateWorkStatus(workInfo);
        if (currentUser == null
                || !classService.isClassMember(workInfo.getClassId(), currentUser.getId())
                   && !classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "无权查看该作业", null);
        }
        if (status == 0) { // 0-未发布：只有老师可以查看
            if (!classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "该作业尚未发布，无法查看", null);
            }
        }

        // 填充附件列表
        workInfo.setAttachments(getWorkAttachments(workId));

        // 转换为WorkResponse
        WorkResponse response = convertToWorkResponse(workInfo);
        // 教师侧详情：填充题目明细（含参考答案），便于编辑与评阅
        if (classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            fillQuestions(response, workId);
        }
        return response;
    }

    @Override
    public Page<WorkResponse> getWorkList(Integer status, Integer classId, Integer pageNum, Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        // 指定班级时的成员身份校验（未登录按非成员处理）
        if (classId != null && (currentUser == null || !classService.isClassMember(classId, currentUser.getId()))) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    currentUser == null ? "用户未登录" : "您不是该班级成员，无法查看该班作业", null);
        }

        // 任教班级范围：拥有老师权限的班级（指定班级时仅判断该班是否任教）
        List<Integer> teacherClassIds;
        if (classId != null) {
            teacherClassIds = currentUser != null && classService.isTeacher(classId, currentUser.getId())
                    ? Collections.singletonList(classId) : Collections.emptyList();
        } else {
            teacherClassIds = currentUser != null
                    ? classService.getTeacherClassIds(currentUser.getId()) : Collections.emptyList();
        }

        // 可见班级范围：指定班级时仅该班；否则为任教与所在班级的并集（管理员经任教接口覆盖全部班级）
        List<Integer> visibleClassIds;
        if (classId != null) {
            visibleClassIds = Collections.singletonList(classId);
        } else if (currentUser == null) {
            visibleClassIds = Collections.emptyList();
        } else {
            Set<Integer> mergedClassIds = new LinkedHashSet<>(teacherClassIds);
            mergedClassIds.addAll(classService.getMemberClassIds(currentUser.getId()));
            visibleClassIds = new ArrayList<>(mergedClassIds);
        }

        // 构建查询条件
        QueryWrapper<WorkInfo> queryWrapper = new QueryWrapper<>();

        // 按状态筛选
        if (status != null && status == 0) {
            // 未发布：只有班级老师可以看到自己管理的班级的未发布作业
            if (currentUser == null) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "用户未登录", null);
            }

            if (teacherClassIds.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以查看未发布作业", null);
            }

            queryWrapper.gt("publish_time", now)
                       .in("class_id", teacherClassIds);
        } else if (status != null && status == 1) {
            // 已发布：仅可见班级范围内的作业
            if (visibleClassIds.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "您尚未加入任何班级，无法查看作业列表", null);
            }
            queryWrapper.in("class_id", visibleClassIds)
                       .le("publish_time", now)
                       .and(wrapper -> wrapper.isNull("deadline").or().gt("deadline", now));
        } else if (status != null && status == 2) {
            // 已结束：仅可见班级范围内的作业
            if (visibleClassIds.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "您尚未加入任何班级，无法查看作业列表", null);
            }
            queryWrapper.in("class_id", visibleClassIds)
                       .isNotNull("deadline")
                       .le("deadline", now);
        } else {
            // status=null: 返回可见班级范围内的作业（已发布 + 已结束 + 任教班级的未发布）
            if (visibleClassIds.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "您尚未加入任何班级，无法查看作业列表", null);
            }

            if (!teacherClassIds.isEmpty()) {
                // 任教班级：可看到任教的未发布作业 + 可见范围内的已发布和已结束作业
                // 注意：这里不能用 .and(w->A).or().and(w->B) 的嵌套写法——
                // MyBatis-Plus 的 .and(Consumer) 内部会追加 AND 连接词，覆盖前面的 OR，导致三组条件被 AND 连接。
                queryWrapper
                    // 未发布作业（仅限老师管理的班级）
                    .gt("publish_time", now).in("class_id", teacherClassIds)
                    .or(w -> w.le("publish_time", now).in("class_id", visibleClassIds)
                              // 已发布：无截止或未截止
                              .and(x -> x.isNull("deadline").or().gt("deadline", now)))
                    // 已结束作业（限可见班级范围）
                    .or(w -> w.isNotNull("deadline").le("deadline", now).in("class_id", visibleClassIds));
            } else {
                // 纯学生：仅可见所在班级的已发布和已结束作业
                queryWrapper
                    // 已发布作业（限所在班级，无截止或未截止）
                    .le("publish_time", now).in("class_id", visibleClassIds)
                    .and(x -> x.isNull("deadline").or().gt("deadline", now))
                    // 已结束作业（限所在班级）
                    .or(w -> w.isNotNull("deadline").le("deadline", now).in("class_id", visibleClassIds));
            }
        }
        
        
        // 排序：置顶的作业在前，然后按创建时间倒序，最后按 id 保证分页顺序稳定
        queryWrapper.orderByDesc("is_pinned")
                   .orderByDesc("create_time")
                   .orderByDesc("id");
        
        // 执行分页查询
        Page<WorkInfo> workPage = new Page<>(pageNum, pageSize);
        Page<WorkInfo> pagedResult = workMapper.selectPage(workPage, queryWrapper);
        
        if (pagedResult.getRecords().isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }
        
        // 批量查询优化：收集所有需要的ID
        List<Integer> publisherIds = pagedResult.getRecords().stream()
                .map(WorkInfo::getPublisherId)
                .distinct()
                .collect(Collectors.toList());
        
        List<Integer> classIds = pagedResult.getRecords().stream()
                .map(WorkInfo::getClassId)
                .distinct()
                .collect(Collectors.toList());
        
        List<Integer> workIds = pagedResult.getRecords().stream()
                .map(WorkInfo::getId)
                .collect(Collectors.toList());
        
        // 批量查询用户信息
        final Map<Integer, User> userMap;
        if (!publisherIds.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", publisherIds);
            List<User> users = userMapper.selectList(userQuery);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        } else {
            userMap = new HashMap<>();
        }
        
        // 批量查询发布人的学校内身份（学校成员才有校内姓名）
        final Map<String, SchoolMember> publisherMemberMap = loadPublisherMembers(pagedResult.getRecords());

        // 批量查询班级信息
        final Map<Integer, ClassInfo> classMap;
        if (!classIds.isEmpty()) {
            List<ClassInfo> classes =
                classService.getClassByIds(classIds);
            classMap = classes.stream().collect(Collectors.toMap(
                ClassInfo::getId, c -> c));
        } else {
            classMap = new HashMap<>();
        }
        
        // 批量查询已交人数（每个作业按去重的提交人统计，排除软删除）
        final Map<Integer, Integer> submittedCountMap;
        if (!workIds.isEmpty()) {
            // 已交人数在数据库内聚合：GROUP BY work_id + COUNT(DISTINCT submitter_id)，
            // 避免取回全部提交行再在内存里分组去重计数
            QueryWrapper<WorkSubmission> submitCntQuery = new QueryWrapper<>();
            submitCntQuery.in("work_id", workIds)
                         .eq("is_deleted", false)
                         .select("work_id", "COUNT(DISTINCT submitter_id) AS cnt")
                         .groupBy("work_id");
            List<Map<String, Object>> submittedRows = workSubmissionMapper.selectMaps(submitCntQuery);
            submittedCountMap = new HashMap<>();
            for (Map<String, Object> row : submittedRows) {
                Object workId = row.get("work_id");
                Object cnt = row.get("cnt");
                if (workId instanceof Number id && cnt instanceof Number count) {
                    submittedCountMap.put(id.intValue(), count.intValue());
                }
            }
        } else {
            submittedCountMap = new HashMap<>();
        }
        
        // 批量查询附件
        final Map<Integer, List<WorkResponse.AttachmentInfo>> attachmentMap;
        if (!workIds.isEmpty()) {
            QueryWrapper<WorkAttachment> attQuery = new QueryWrapper<>();
            attQuery.in("work_id", workIds);
            List<WorkAttachment> allAttachments = workAttachmentMapper.selectList(attQuery);
            
            attachmentMap = allAttachments.stream()
                    .collect(Collectors.groupingBy(
                        WorkAttachment::getWorkId,
                        Collectors.mapping(att -> new WorkResponse.AttachmentInfo(
                            att.getId(),
                            att.getFileName(),
                            att.getFilePath(),
                            att.getFileSize(),
                            att.getFileType(),
                            att.getUploadTime()
                        ), Collectors.toList())
                    ));
        } else {
            attachmentMap = new HashMap<>();
        }
        
        // 转换为响应对象（数据库已完成所有过滤，无需再过滤）
        List<WorkResponse> responses = pagedResult.getRecords().stream()
                .map(work -> {
                    WorkResponse response = new WorkResponse();
                    response.setId(work.getId());
                    response.setTitle(work.getTitle());
                    response.setDescription(work.getDescription());
                    response.setPublisherId(work.getPublisherId());

                    // 发布人标识：学校成员展示校内姓名，非成员（如管理员）退回用户名
                    User publisher = userMap.get(work.getPublisherId());
                    response.setPublisherName(publisher != null ? publisher.getUsername() : null);
                    SchoolMember publisherMember = publisherMemberMap.get(work.getClassId() + ":" + work.getPublisherId());
                    if (publisherMember != null) {
                        response.setPublisherStudentName(publisherMember.getRealName());
                    }
                    
                    response.setClassId(work.getClassId());
                    
                    // 从缓存中获取班级名称
                    ClassInfo classInfo = classMap.get(work.getClassId());
                    response.setClassName(classInfo != null ? classInfo.getClassName() : null);
                    
                    response.setDeadline(work.getDeadline());
                    response.setTotalScore(work.getTotalScore());
                    response.setPublishTime(work.getPublishTime());
                    response.setStatus(calculateWorkStatus(work)); // 动态计算状态
                    response.setIsOverdue(work.getDeadline() != null && now.isAfter(work.getDeadline()));
                    response.setIsPinned(work.getIsPinned());
                    response.setCreateTime(work.getCreateTime());
                    response.setUpdateTime(work.getUpdateTime());
                    response.setSubmittedCount(submittedCountMap.getOrDefault(work.getId(), 0));
                    
                    // 从缓存中获取附件列表
                    response.setAttachments(attachmentMap.getOrDefault(work.getId(), new ArrayList<>()));
                    
                    return response;
                })
                .collect(Collectors.toList());

        // 构建分页结果
        Page<WorkResponse> page = new Page<>(pageNum, pageSize, pagedResult.getTotal());
        page.setRecords(responses);
        return page;
    }
    
    /**
     * 动态计算作业状态
     * @param workInfo 作业信息
     * @return 0-未发布，1-已发布，2-已结束
     */
    private Integer calculateWorkStatus(WorkInfo workInfo) {
        LocalDateTime now = LocalDateTime.now();
        
        // 如果当前时间在发布时间之前，状态为 0（未发布）
        if (workInfo.getPublishTime() != null && now.isBefore(workInfo.getPublishTime())) {
            return 0;
        }
        
        // 如果当前时间在截止时间之后，状态为 2（已结束）
        if (workInfo.getDeadline() != null && now.isAfter(workInfo.getDeadline())) {
            return 2;
        }
        
        // 否则状态为 1（已发布）
        return 1;
    }
    
    /**
     * 按「班级 + 发布人」批量获取发布人的学校内身份，key 为 classId:userId
     */
    private Map<String, SchoolMember> loadPublisherMembers(List<WorkInfo> works) {
        Set<Integer> classIds = works.stream().map(WorkInfo::getClassId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> publisherIds = works.stream().map(WorkInfo::getPublisherId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (classIds.isEmpty() || publisherIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 姓名与学工号是学校内身份，同一用户在不同学校可有不同身份，故先取班级所属学校
        QueryWrapper<ClassInfo> classQuery = new QueryWrapper<>();
        classQuery.in("id", classIds).select("id", "school_id");
        Map<Integer, Integer> classSchoolMap = classInfoMapper.selectList(classQuery).stream()
                .filter(classInfo -> classInfo.getSchoolId() != null)
                .collect(Collectors.toMap(ClassInfo::getId, ClassInfo::getSchoolId, (a, b) -> a));

        Map<Integer, Set<Integer>> schoolToPublishers = new HashMap<>();
        for (WorkInfo work : works) {
            Integer schoolId = classSchoolMap.get(work.getClassId());
            if (schoolId == null || work.getPublisherId() == null) {
                continue;
            }
            schoolToPublishers.computeIfAbsent(schoolId, key -> new HashSet<>()).add(work.getPublisherId());
        }

        Map<Integer, Map<Integer, SchoolMember>> membersBySchool = new HashMap<>();
        for (Map.Entry<Integer, Set<Integer>> entry : schoolToPublishers.entrySet()) {
            membersBySchool.put(entry.getKey(),
                    schoolService.getMembersByUserIds(entry.getKey(), entry.getValue()));
        }

        Map<String, SchoolMember> result = new HashMap<>();
        for (WorkInfo work : works) {
            Integer schoolId = classSchoolMap.get(work.getClassId());
            if (schoolId == null || work.getPublisherId() == null) {
                continue;
            }
            Map<Integer, SchoolMember> members = membersBySchool.get(schoolId);
            SchoolMember member = members == null ? null : members.get(work.getPublisherId());
            if (member != null) {
                result.put(work.getClassId() + ":" + work.getPublisherId(), member);
            }
        }
        return result;
    }

    /**
     * 填充发布人信息：学校成员用校内姓名，非成员（如管理员）退回用户名
     */
    private void fillPublisherInfo(WorkResponse response, Integer classId, Integer publisherId) {
        if (publisherId == null) {
            return;
        }
        User publisher = userMapper.selectById(publisherId);
        response.setPublisherName(publisher != null ? publisher.getUsername() : null);

        if (classId == null) {
            return;
        }
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null || classInfo.getSchoolId() == null) {
            return;
        }
        SchoolMember member = schoolService.getMember(classInfo.getSchoolId(), publisherId);
        if (member != null) {
            response.setPublisherStudentName(member.getRealName());
        }
    }

    /**
     * 将WorkInfo转换为WorkResponse
     * @param workInfo 作业信息
     * @return 作业响应对象
     */
    private WorkResponse convertToWorkResponse(WorkInfo workInfo) {
        WorkResponse response = new WorkResponse();
        response.setId(workInfo.getId());
        response.setTitle(workInfo.getTitle());
        response.setDescription(workInfo.getDescription());
        response.setPublisherId(workInfo.getPublisherId());
        fillPublisherInfo(response, workInfo.getClassId(), workInfo.getPublisherId());
        response.setClassId(workInfo.getClassId());
        response.setDeadline(workInfo.getDeadline());
        response.setTotalScore(workInfo.getTotalScore());
        response.setPublishTime(workInfo.getPublishTime());
        response.setStatus(calculateWorkStatus(workInfo));
        response.setIsOverdue(workInfo.getDeadline() != null && LocalDateTime.now().isAfter(workInfo.getDeadline()));
        response.setIsPinned(workInfo.getIsPinned());
        response.setCreateTime(workInfo.getCreateTime());
        response.setUpdateTime(workInfo.getUpdateTime());
        // 列表/详情都带上「是否含题目」标记；题目明细在需要时单独填充，避免列表 N+1
        response.setHasQuestions(Boolean.TRUE.equals(workInfo.getHasQuestions()));

        // 类型与考试配置
        response.setWorkType(workInfo.getWorkType() == null ? WorkType.HOMEWORK : workInfo.getWorkType());
        response.setExamDurationMinutes(workInfo.getExamDurationMinutes());
        response.setAntiCheatEnabled(Boolean.TRUE.equals(workInfo.getAntiCheatEnabled()));
        response.setAntiCheatFont(Boolean.TRUE.equals(workInfo.getAntiCheatFont()));
        response.setAntiCheatFullscreen(Boolean.TRUE.equals(workInfo.getAntiCheatFullscreen()));
        response.setAntiCheatNoCopy(Boolean.TRUE.equals(workInfo.getAntiCheatNoCopy()));
        response.setAntiCheatDetectLeave(Boolean.TRUE.equals(workInfo.getAntiCheatDetectLeave()));
        response.setAntiCheatMaxViolations(workInfo.getAntiCheatMaxViolations());
        response.setShuffleQuestions(Boolean.TRUE.equals(workInfo.getShuffleQuestions()));

        // 填充附件列表
        response.setAttachments(getWorkAttachments(workInfo.getId()));

        return response;
    }

    /**
     * 应用「类型 + 考试配置」到实体。
     * <p>
     * workType 为空时保持原值（默认 homework）；examConfig 为 null 时不改动反作弊字段。
     * 关闭反作弊时，所有反作弊子项一律置 false，避免残留脏配置。
     *
     * @param workInfo   目标作业/考试实体
     * @param workType   类型（可空）
     * @param examConfig 考试配置（可空）
     */
    private void applyWorkTypeAndExamConfig(WorkInfo workInfo, String workType, ExamConfigDto examConfig) {
        if (workType != null) {
            workInfo.setWorkType(WorkType.isValid(workType) ? workType : WorkType.HOMEWORK);
        } else if (workInfo.getWorkType() == null) {
            workInfo.setWorkType(WorkType.HOMEWORK);
        }

        if (examConfig == null) {
            return;
        }
        workInfo.setExamDurationMinutes(examConfig.getDurationMinutes());
        boolean enabled = Boolean.TRUE.equals(examConfig.getEnabled());
        workInfo.setAntiCheatEnabled(enabled);
        workInfo.setAntiCheatFont(enabled && Boolean.TRUE.equals(examConfig.getFontScramble()));
        workInfo.setAntiCheatFullscreen(enabled && Boolean.TRUE.equals(examConfig.getForceFullscreen()));
        workInfo.setAntiCheatNoCopy(enabled && Boolean.TRUE.equals(examConfig.getNoCopy()));
        workInfo.setAntiCheatDetectLeave(enabled && Boolean.TRUE.equals(examConfig.getDetectLeave()));
        workInfo.setAntiCheatMaxViolations(enabled ? examConfig.getMaxViolations() : null);
        workInfo.setShuffleQuestions(Boolean.TRUE.equals(examConfig.getShuffleQuestions()));
    }

    /**
     * 为教师侧响应填充题目明细（含参考答案）
     */
    private void fillQuestions(WorkResponse response, Integer workId) {
        response.setQuestions(workQuestionService.listForTeacher(workId));
    }
    
    /**
     * 保存作业附件（直接上传的文件）
     */
    private void saveWorkAttachmentsDirectly(Integer userId, Integer workId, List<MultipartFile> files) {
        if (CollUtil.isEmpty(files)) {
            return;
        }

        // 预检查：收集文件的扩展名和大小，在落盘前先校验扩展名和大小
        List<MultipartFile> validFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.contains("..")) {
                throw new BusinessException(BusinessErrorCode.INVALID_FILE_PATH, "非法的文件名", null);
            }
            // 1. 落盘前先校验扩展名白名单（最常见的非法类型）
            FileUploadValidator.validateFileExtension(originalFilename);
            // 2. 落盘前先校验文件大小
            FileUploadValidator.validateFileSize(file.getSize());
            validFiles.add(file);
        }

        if (validFiles.isEmpty()) {
            return;
        }

        // 确保上传目录存在（相对路径，基于运行目录）
        Path uploadPath = Paths.get(UPLOAD_DIR).normalize();
        try {
            Files.createDirectories(uploadPath);
        } catch (Exception e) {
            log.error("Failed to create upload directory: {}", uploadPath, e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, "无法创建上传目录", null);
        }

        // 文件需逐个落盘（IO 无法合并），但数据库记录收集后一次性批量插入，避免逐条 insert
        List<WorkAttachment> pending = new ArrayList<>();

        for (MultipartFile file : validFiles) {
            Path savedFilePath = null;
            try {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) {
                    continue;
                }

                // 生成安全的文件名（业务ID-用户ID-时间戳）
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String safeFileName = workId + "_" + userId + "_" + timestamp + extension;

                savedFilePath = uploadPath.resolve(safeFileName);

                // 3. 保存文件（相对路径，基于运行目录）
                Files.copy(file.getInputStream(), savedFilePath);

                // 4. 获取落盘后的实际文件信息
                long fileSize = Files.size(savedFilePath);
                String fileType = FileUploadValidator.detectFileType(savedFilePath.toString());

                // 5. 执行完整的安全检查（含魔数、路径、存在性、MIME 等）
                // 注：落盘前的大小/扩展名已在前面检查过，这里是深度校验
                FileUploadValidator.performFullSecurityCheck(savedFilePath.toString(), fileSize);

                // 6. 全部校验通过后，暂存待插入的数据库记录（稍后批量插入）
                WorkAttachment attachment = new WorkAttachment();
                attachment.setWorkId(workId);
                attachment.setFileName(originalFilename);
                attachment.setFilePath(savedFilePath.toString());
                attachment.setFileSize(fileSize);
                attachment.setFileType(fileType);
                attachment.setUploadTime(LocalDateTime.now());
                pending.add(attachment);

            } catch (BusinessException e) {
                // 任意校验失败：如果文件已经落盘，立刻物理删除后再抛异常
                if (savedFilePath != null) {
                    try {
                        if (Files.exists(savedFilePath)) {
                            Files.delete(savedFilePath);
                        }
                    } catch (Exception delEx) {
                        log.warn("Failed to rollback invalid attachment file: {}", savedFilePath, delEx);
                    }
                }
                throw e;
            } catch (Exception e) {
                // IO 或其他异常：同样清理已落盘的文件
                if (savedFilePath != null) {
                    try {
                        if (Files.exists(savedFilePath)) {
                            Files.delete(savedFilePath);
                        }
                    } catch (Exception delEx) {
                        log.warn("Failed to rollback attachment file on error: {}", savedFilePath, delEx);
                    }
                }
                log.error("Failed to save work attachment", e);
                throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED,
                        "文件上传失败，请稍后重试", null);
            }
        }

        // 7. 一次批量插入全部附件记录
        if (!pending.isEmpty()) {
            workAttachmentMapper.insert(pending);
        }
    }
    

    /**
     * 获取作业附件列表
     */
    private List<WorkResponse.AttachmentInfo> getWorkAttachments(Integer workId) {
        QueryWrapper<WorkAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("work_id", workId);
        List<WorkAttachment> attachments = workAttachmentMapper.selectList(queryWrapper);
        
        return attachments.stream()
                .map(attachment -> new WorkResponse.AttachmentInfo(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getFilePath(),
                        attachment.getFileSize(),
                        attachment.getFileType(),
                        attachment.getUploadTime()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 处理附件更新（增量更新）
     * @param workId 作业ID
     * @param removedAttachmentIds 要删除的附件ID列表
     * @param newAttachments 新增的附件文件列表
     */
    private void handleAttachmentUpdates(Integer workId, List<Integer> removedAttachmentIds, List<MultipartFile> newAttachments) {
        // 1. 删除指定的附件：一次查询取出这些附件，避免逐个 selectById
        if (CollUtil.isNotEmpty(removedAttachmentIds)) {
            QueryWrapper<WorkAttachment> removedQuery = new QueryWrapper<>();
            removedQuery.in("id", removedAttachmentIds).eq("work_id", workId);
            List<WorkAttachment> removed = workAttachmentMapper.selectList(removedQuery);

            for (WorkAttachment attachment : removed) {
                // 物理删除文件；删除失败时抛出异常，回滚数据库记录删除，保证磁盘与数据库一致
                Path filePath = Paths.get(attachment.getFilePath());
                if (Files.exists(filePath)) {
                    try {
                        Files.delete(filePath);
                    } catch (Exception e) {
                        log.error("Failed to delete attachment file: {}", attachment.getFilePath(), e);
                        throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED,
                                "附件文件删除失败：" + attachment.getFilePath(), null);
                    }
                }
            }
            // 一次 SQL 删除这些数据库记录
            if (!removed.isEmpty()) {
                QueryWrapper<WorkAttachment> deleteQuery = new QueryWrapper<>();
                deleteQuery.in("id", removed.stream().map(WorkAttachment::getId).toList());
                workAttachmentMapper.delete(deleteQuery);
            }
        }
        
        // 2. 添加新附件
        if (CollUtil.isNotEmpty(newAttachments)) {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser != null) {
                saveWorkAttachmentsDirectly(currentUser.getId(), workId, newAttachments);
            }
        }
    }
    
    /**
     * 级联删除作业及其所有关联数据（软删除）
     * @param workId 作业ID
     */
    private void cascadeDeleteWork(Integer workId) {
        // 1. 查询该作业下未删除的提交记录（仅取主键）
        QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.eq("work_id", workId)
                      .eq("is_deleted", false)
                      .select("id");
        List<Integer> submissionIds = workSubmissionMapper.selectList(submissionQuery).stream()
                .map(WorkSubmission::getId)
                .toList();

        if (!submissionIds.isEmpty()) {
            // 2. 一次 SQL 软删除这些提交的全部附件（不再逐条 updateById）
            QueryWrapper<WorkSubmissionAttachment> attQuery = new QueryWrapper<>();
            attQuery.in("submission_id", submissionIds).eq("is_deleted", false);
            WorkSubmissionAttachment attachmentUpdate = new WorkSubmissionAttachment();
            attachmentUpdate.setIsDeleted(true);
            workSubmissionAttachmentMapper.update(attachmentUpdate, attQuery);

            // 3. 一次 SQL 软删除这些提交记录
            QueryWrapper<WorkSubmission> submissionUpdateQuery = new QueryWrapper<>();
            submissionUpdateQuery.in("id", submissionIds);
            WorkSubmission submissionUpdate = new WorkSubmission();
            submissionUpdate.setIsDeleted(true);
            workSubmissionMapper.update(submissionUpdate, submissionUpdateQuery);
        }
        
        // 4. 删除作业本身的附件：先物理删文件，再删记录
        QueryWrapper<WorkAttachment> workAttQuery = new QueryWrapper<>();
        workAttQuery.eq("work_id", workId);
        List<WorkAttachment> workAttachments = workAttachmentMapper.selectList(workAttQuery);
        
        for (WorkAttachment attachment : workAttachments) {
            try {
                Path filePath = Paths.get(attachment.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (Exception e) {
                log.warn("Failed to delete work attachment file: {}", attachment.getFilePath(), e);
            }
        }
        workAttachmentMapper.delete(workAttQuery);
        
        // 5. 清理考试会话与违规记录（考试才有；非考试时无记录，为无害的空删除）
        examViolationMapper.delete(new QueryWrapper<ExamViolation>().eq("work_id", workId));
        examSessionMapper.delete(new QueryWrapper<ExamSession>().eq("work_id", workId));

        // 6. 最后删除作业/考试本身
        workMapper.deleteById(workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkResponse pinWork(Integer workId, Boolean isPinned) {
        // 1. 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 2. 查询作业
        WorkInfo workInfo = workMapper.selectById(workId);
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 3. 验证权限（只有班级老师可以置顶作业）
        if (!classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以置顶作业", null);
        }

        // 4. 更新置顶状态
        workInfo.setIsPinned(isPinned);
        workInfo.setUpdateTime(LocalDateTime.now());
        
        int updated = workMapper.updateById(workInfo);
        if (updated <= 0) {
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, "更新作业置顶状态失败", null);
        }

        // 转换为WorkResponse
        return convertToWorkResponse(workInfo);
    }
}
