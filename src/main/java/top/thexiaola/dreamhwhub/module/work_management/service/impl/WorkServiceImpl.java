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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作业服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkServiceImpl implements WorkService {

    // 文件存储根目录
    private static final String UPLOAD_DIR = "uploads/works/";

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

        // 处理发布时间
        LocalDateTime publishTime;
        if (request.getPublishTime() == null) {
            // 如果不填时间，当作即时发布
            publishTime = LocalDateTime.now();
        } else {
            // 如果填了以前的时间，拒绝发布
            if (request.getPublishTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "发布时间不能是过去的时间", null);
            }
            publishTime = request.getPublishTime();
        }

        // 校验截止时间：不能设置为过去的时间
        if (request.getDeadline() != null && request.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "截止时间不能是过去的时间", null);
        }

        // 创建作业
        WorkInfo workInfo = new WorkInfo();
        workInfo.setTitle(request.getTitle());
        workInfo.setDescription(request.getDescription());
        workInfo.setClassId(request.getClassId());
        workInfo.setDeadline(request.getDeadline());
        workInfo.setTotalScore(request.getTotalScore());
        workInfo.setAllowLateSubmit(request.getAllowLateSubmit() != null ? request.getAllowLateSubmit() : true);
        workInfo.setPublishTime(publishTime);
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

        // 校验截止时间修改
        if (request.getDeadline() != null) {
            LocalDateTime newDeadline = request.getDeadline();
            LocalDateTime oldDeadline = workInfo.getDeadline();
            
            // 如果新截止时间是过去的时间
            if (newDeadline.isBefore(now)) {
                // 情况1：与之前的截止时间相同——忽略截止时间的修改，继续处理其他字段
                if (oldDeadline != null && newDeadline.isEqual(oldDeadline)) {
                    // 不修改 deadline 字段，保持原值
                    log.info("User {} attempted to set deadline to past time, but it's the same as current deadline, ignoring deadline update", currentUser.getId());
                } else {
                    // 情况2：与之前的截止时间不同——报错
                    throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "截止时间不能修改为过去的时间", null);
                }
            } else {
                // 新截止时间是未来时间，允许修改
                workInfo.setDeadline(newDeadline);
            }
        } else {
            // 如果请求中 deadline 为 null，表示要清除截止时间（设置为永久有效）
            workInfo.setDeadline(null);
        }

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
        
        log.info("User {} deleted work {}, status was: {}", currentUser.getId(), workId, currentStatus);
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
        
        return workInfo;
    }

    @Override
    public Page<WorkResponse> getWorkList(String publisherUserNo, Integer status, Integer pageNum, Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        
        // 构建查询条件
        QueryWrapper<WorkInfo> queryWrapper = new QueryWrapper<>();
        
        // 按状态筛选
        if (status != null && status == 0) {
            // 未发布：只有班级老师可以看到自己管理的班级的未发布作业
            if (currentUser == null) {
                return new Page<>(pageNum, pageSize, 0);
            }
            
            List<Integer> teacherClassIds = classService.getTeacherClassIds(currentUser.getId());
            if (teacherClassIds.isEmpty()) {
                return new Page<>(pageNum, pageSize, 0);
            }
            
            queryWrapper.gt("publish_time", now)
                       .in("class_id", teacherClassIds);
        } else if (status != null && status == 1) {
            // 已发布：发布时间 <= 当前时间 AND (截止时间为空 OR 截止时间 > 当前时间)
            queryWrapper.le("publish_time", now)
                       .and(wrapper -> wrapper.isNull("deadline").or().gt("deadline", now));
        } else if (status != null && status == 2) {
            // 已结束：截止时间不为空 AND 截止时间 <= 当前时间
            queryWrapper.isNotNull("deadline")
                       .le("deadline", now);
        } else {
            // status=null: 返回所有可见作业（已发布 + 已结束 + 用户有权限的未发布）
            if (currentUser != null) {
                List<Integer> teacherClassIds = classService.getTeacherClassIds(currentUser.getId());
                
                if (!teacherClassIds.isEmpty()) {
                    // 用户是某些班级的老师：可以看到这些班级的未发布作业 + 所有已发布和已结束作业
                    queryWrapper.and(wrapper -> wrapper
                        // 未发布作业（仅限老师管理的班级）
                        .gt("publish_time", now)
                        .in("class_id", teacherClassIds)
                        .or()
                        // 已发布作业
                        .le("publish_time", now)
                        .and(w -> w.isNull("deadline").or().gt("deadline", now))
                        .or()
                        // 已结束作业
                        .isNotNull("deadline")
                        .le("deadline", now)
                    );
                } else {
                    // 用户不是任何班级的老师：只能看到已发布和已结束作业
                    queryWrapper.and(wrapper -> wrapper
                        // 已发布作业
                        .le("publish_time", now)
                        .and(w -> w.isNull("deadline").or().gt("deadline", now))
                        .or()
                        // 已结束作业
                        .isNotNull("deadline")
                        .le("deadline", now)
                    );
                }
            } else {
                // 未登录用户：只能看到已发布和已结束作业
                queryWrapper.and(wrapper -> wrapper
                    // 已发布作业
                    .le("publish_time", now)
                    .and(w -> w.isNull("deadline").or().gt("deadline", now))
                    .or()
                    // 已结束作业
                    .isNotNull("deadline")
                    .le("deadline", now)
                );
            }
        }
        
        // 按发布人筛选
        if (publisherUserNo != null && !publisherUserNo.isEmpty()) {
            Integer publisherId = getUserIdByUserNo(publisherUserNo);
            if (publisherId == null) {
                return new Page<>(pageNum, pageSize, 0);
            }
            queryWrapper.eq("publisher_id", publisherId);
        }
        
        // 排序：置顶的作业在前，然后按创建时间倒序
        queryWrapper.orderByDesc("is_pinned")
                   .orderByDesc("create_time");
        
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
     * 通过用户号获取用户 ID
     */
    private Integer getUserIdByUserNo(String userNo) {
        if (StrUtil.isBlank(userNo)) {
            return null;
        }
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", userNo);
        User user = userMapper.selectOne(queryWrapper);
        
        return user != null ? user.getId() : null;
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
        
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            
            try {
                // 1. 获取原始文件名
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || originalFilename.contains("..")) {
                    throw new BusinessException(BusinessErrorCode.INVALID_FILE_PATH, "非法的文件名", null);
                }
                
                // 2. 生成安全的文件名（业务ID-用户ID-时间戳）
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String safeFileName = workId + "_" + userId + "_" + timestamp + extension;
                
                // 3. 确保上传目录存在
                Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
                Files.createDirectories(uploadPath);
                
                // 4. 保存文件
                Path filePath = uploadPath.resolve(safeFileName);
                Files.copy(file.getInputStream(), filePath);
                
                // 5. 获取文件信息
                long fileSize = Files.size(filePath);
                String fileType = FileUploadValidator.detectFileType(filePath.toString());
                
                // 6. 执行完整的安全检查
                FileUploadValidator.performFullSecurityCheck(filePath.toString(), fileSize);
                
                // 7. 保存到数据库
                WorkAttachment attachment = new WorkAttachment();
                attachment.setWorkId(workId);
                attachment.setFileName(originalFilename);
                attachment.setFilePath(filePath.toString());
                attachment.setFileSize(fileSize);
                attachment.setFileType(fileType);
                attachment.setUploadTime(LocalDateTime.now());
                workAttachmentMapper.insert(attachment);
                
                log.info("Saved work attachment directly: {}, size: {}, type: {}", 
                        originalFilename, fileSize, fileType);
                        
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
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
                    // 物理删除文件
                    try {
                        Path filePath = Paths.get(attachment.getFilePath());
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                            log.info("Deleted attachment file: {}", attachment.getFilePath());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to delete attachment file: {}", attachment.getFilePath(), e);
                    }
                    // 删除数据库记录
                    workAttachmentMapper.deleteById(attachmentId);
                    log.info("Deleted attachment record: id={}", attachmentId);
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
                log.info("Soft deleted submission attachment record: id={}", attachment.getId());
            }
            
            // 软删除提交记录
            submission.setIsDeleted(true);
            workSubmissionMapper.updateById(submission);
        }
        log.info("Soft deleted {} submission records for work {}", submissions.size(), workId);
        
        // 3. 软删除作业本身的附件记录
        QueryWrapper<WorkAttachment> workAttQuery = new QueryWrapper<>();
        workAttQuery.eq("work_id", workId);
        List<WorkAttachment> workAttachments = workAttachmentMapper.selectList(workAttQuery);
        
        for (WorkAttachment attachment : workAttachments) {
            try {
                Path filePath = Paths.get(attachment.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted work attachment file: {}", attachment.getFilePath());
                }
            } catch (Exception e) {
                log.warn("Failed to delete work attachment file: {}", attachment.getFilePath(), e);
            }
        }
        workAttachmentMapper.delete(workAttQuery);
        
        // 4. 最后删除作业本身
        workMapper.deleteById(workId);
        log.info("Deleted work {} and soft deleted all related data", workId);
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

        String action = isPinned ? "pinned" : "unpinned";
        log.info("User {} {} work {} successfully", currentUser.getId(), action, workId);
        
        return workInfo;
    }
}
