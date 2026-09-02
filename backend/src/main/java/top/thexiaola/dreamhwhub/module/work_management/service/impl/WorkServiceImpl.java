package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
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
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.entity.*;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;
import top.thexiaola.dreamhwhub.support.session.UserUtils;
import top.thexiaola.dreamhwhub.support.validation.FileUploadValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ClassService classService;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkInfo createWork(CreateWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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

        workMapper.insert(workInfo);
        
        // 保存附件（直接上传的文件）
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            saveWorkAttachmentsDirectly(currentUser.getId(), workInfo.getId(), request.getAttachments());
        }
        
        return workInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkInfo updateWork(UpdateWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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

        workMapper.updateById(workInfo);
        
        // 处理附件更新（直接上传）
        handleAttachmentUpdates(workInfo.getId(), request.getRemovedAttachmentIds(), request.getAttachments());
        
        return workInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWork(Integer workId) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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
    public WorkInfo getWorkById(Integer workId) {
        WorkInfo workInfo = workMapper.selectById(workId);
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查权限：未发布的作业只有老师可以查看
        User currentUser = UserUtils.getCurrentUser();
        Integer status = calculateWorkStatus(workInfo);
        if (status == 0) { // 0-未发布
            if (currentUser == null || !classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "该作业尚未发布，无法查看", null);
            }
        }

        // 填充附件列表
        workInfo.setAttachments(getWorkAttachments(workId));

        return workInfo;
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
                    
                    // 从缓存中获取发布人用户名
                    User publisher = userMap.get(work.getPublisherId());
                    response.setPublisherName(publisher != null ? publisher.getUsername() : null);
                    
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

                // 6. 全部校验通过后，才持久化到数据库
                WorkAttachment attachment = new WorkAttachment();
                attachment.setWorkId(workId);
                attachment.setFileName(originalFilename);
                attachment.setFilePath(savedFilePath.toString());
                attachment.setFileSize(fileSize);
                attachment.setFileType(fileType);
                attachment.setUploadTime(LocalDateTime.now());
                workAttachmentMapper.insert(attachment);


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
                        "文件上传失败：" + e.getMessage(), null);
            }
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
        // 1. 删除指定的附件
        if (CollUtil.isNotEmpty(removedAttachmentIds)) {
            for (Integer attachmentId : removedAttachmentIds) {
                WorkAttachment attachment = workAttachmentMapper.selectById(attachmentId);
                if (attachment != null && attachment.getWorkId().equals(workId)) {
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
                    // 删除数据库记录
                    workAttachmentMapper.deleteById(attachmentId);
                }
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
        // 1. 查询该作业的所有提交记录
        QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.eq("work_id", workId)
                      .eq("is_deleted", false);
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(submissionQuery);
        
        // 2. 软删除每个提交的附件记录
        for (WorkSubmission submission : submissions) {
            QueryWrapper<WorkSubmissionAttachment> attQuery = new QueryWrapper<>();
            attQuery.eq("submission_id", submission.getId())
                   .eq("is_deleted", false);
            List<WorkSubmissionAttachment> attachments = workSubmissionAttachmentMapper.selectList(attQuery);
            
            // 软删除附件记录
            for (WorkSubmissionAttachment attachment : attachments) {
                attachment.setIsDeleted(true);
                workSubmissionAttachmentMapper.updateById(attachment);
            }
            
            // 软删除提交记录
            submission.setIsDeleted(true);
            workSubmissionMapper.updateById(submission);
        }
        
        // 3. 软删除作业本身的附件记录
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
        
        // 4. 最后删除作业本身
        workMapper.deleteById(workId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkInfo pinWork(Integer workId, Boolean isPinned) {
        // 1. 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

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

        return workInfo;
    }
}
