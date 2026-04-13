package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.work_management.domain.*;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.mapper.*;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.service.FileUploadService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;
import top.thexiaola.dreamhwhub.support.session.UserUtils;
import top.thexiaola.dreamhwhub.support.validation.FileUploadValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业服务实现类
 */
@Service
public class WorkServiceImpl implements WorkService {
    private static final Logger log = LoggerFactory.getLogger(WorkServiceImpl.class);

    private final WorkMapper workMapper;
    private final WorkAttachmentMapper workAttachmentMapper;
    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper;
    private final ClassService classService;
    private final UserMapper userMapper;
    private final FileUploadService fileUploadService;
    private final TempFileUploadMapper tempFileUploadMapper;

    public WorkServiceImpl(WorkMapper workMapper, WorkAttachmentMapper workAttachmentMapper, 
                          WorkSubmissionMapper workSubmissionMapper, WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper,
                          ClassService classService, UserMapper userMapper, FileUploadService fileUploadService, TempFileUploadMapper tempFileUploadMapper) {
        this.workMapper = workMapper;
        this.workAttachmentMapper = workAttachmentMapper;
        this.workSubmissionMapper = workSubmissionMapper;
        this.workSubmissionAttachmentMapper = workSubmissionAttachmentMapper;
        this.classService = classService;
        this.userMapper = userMapper;
        this.fileUploadService = fileUploadService;
        this.tempFileUploadMapper = tempFileUploadMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkInfo createWork(CreateWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限（只有班级老师可以发布作业）
        if (!classService.isTeacher(Integer.parseInt(request.getClassId()), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以发布作业", null);
        }

        // 创建作业
        WorkInfo workInfo = new WorkInfo();
        workInfo.setTitle(request.getTitle());
        workInfo.setDescription(request.getDescription());
        workInfo.setClassId(Integer.parseInt(request.getClassId()));
        workInfo.setDeadline(request.getDeadline());
        workInfo.setTotalScore(request.getTotalScore());
        workInfo.setAllowLateSubmit(request.getAllowLateSubmit() != null ? request.getAllowLateSubmit() : true);
        workInfo.setPublishTime(request.getPublishTime());
        workInfo.setCreateTime(LocalDateTime.now());
        workInfo.setUpdateTime(LocalDateTime.now());

        workMapper.insert(workInfo);
        
        // 保存附件（使用文件ID）
        if (request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty()) {
            saveWorkAttachmentsWithValidation(workInfo.getId(), request.getAttachmentIds(), currentUser.getId());
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
        WorkInfo workInfo = workMapper.selectById(Integer.parseInt(request.getId()));
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查权限（只有班级老师可以修改作业）
        if (!classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以修改作业", null);
        }

        // 只能修改自己发布的作业
        if (!workInfo.getPublisherId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能修改自己发布的作业", null);
        }

        // 再次检查是否是班级老师
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
            submissionQuery.eq("work_id", workInfo.getId());
            long submissionCount = workSubmissionMapper.selectCount(submissionQuery);
            if (submissionCount > 0) {
                throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, 
                        "已有学生提交作业，无法修改总分", null);
            }
        }

        // 更新作业
        workInfo.setTitle(request.getTitle());
        workInfo.setDescription(request.getDescription());
        workInfo.setDeadline(request.getDeadline());
        workInfo.setTotalScore(request.getTotalScore());
        if (request.getAllowLateSubmit() != null) {
            workInfo.setAllowLateSubmit(request.getAllowLateSubmit());
        }
        if (request.getPublishTime() != null) {
            workInfo.setPublishTime(request.getPublishTime());
        }
        workInfo.setUpdateTime(LocalDateTime.now());

        workMapper.updateById(workInfo);
        
        // 处理附件更新（使用文件ID）
        handleAttachmentUpdates(workInfo.getId(), request.getRemovedAttachmentIds(), request.getAttachmentIds());
        
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

        // 只能删除自己发布的作业
        if (!workInfo.getPublisherId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能删除自己发布的作业", null);
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
        // 默认分页参数
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;  // 限制最大每页数量

        User currentUser = UserUtils.getCurrentUser();
        QueryWrapper<WorkInfo> queryWrapper = new QueryWrapper<>();
        
        if (publisherUserNo != null && !publisherUserNo.isEmpty()) {
            Integer publisherId = getUserIdByUserNo(publisherUserNo);
            if (publisherId != null) {
                queryWrapper.eq("publisher_id", publisherId);
            } else {
                // 如果用户不存在，返回空分页
                return new Page<>(pageNum, pageSize, 0);
            }
        }
        
        queryWrapper.orderByDesc("create_time");
        
        // 使用MyBatisPlus分页
        Page<WorkInfo> workPage = new Page<>(pageNum, pageSize);
        Page<WorkInfo> pagedResult = workMapper.selectPage(workPage, queryWrapper);
        LocalDateTime now = LocalDateTime.now();
        
        List<WorkResponse> responses = pagedResult.getRecords().stream()
                .filter(work -> {
                    // 过滤未发布作业：学生看不到
                    Integer workStatus = calculateWorkStatus(work);
                    if (workStatus == 0) { // 0-未发布
                        return currentUser != null && classService.isTeacher(work.getClassId(), currentUser.getId());
                    }
                    return true;
                })
                .map(work -> {
                    WorkResponse response = new WorkResponse();
                    response.setId(work.getId());
                    response.setTitle(work.getTitle());
                    response.setDescription(work.getDescription());
                    response.setPublisherId(work.getPublisherId());
                    response.setDeadline(work.getDeadline());
                    response.setTotalScore(work.getTotalScore());
                    response.setPublishTime(work.getPublishTime());
                    response.setStatus(calculateWorkStatus(work)); // 动态计算状态
                    response.setIsOverdue(work.getDeadline() != null && now.isAfter(work.getDeadline()));
                    response.setCreateTime(work.getCreateTime());
                    response.setUpdateTime(work.getUpdateTime());
                    
                    // 加载附件列表
                    List<WorkResponse.AttachmentInfo> attachments = getWorkAttachments(work.getId());
                    response.setAttachments(attachments);
                    
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
        if (userNo == null || userNo.isEmpty()) {
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
     * 保存作业附件（通过文件ID，验证归属权）
     */
    private void saveWorkAttachmentsWithValidation(Integer workId, List<Integer> attachmentIds, Integer userId) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        
        for (Integer fileId : attachmentIds) {
            // 1. 验证文件归属权并标记为已使用
            fileUploadService.validateAndMarkAsUsed(fileId, userId);
            
            // 2. 查询临时文件信息
            TempFileUpload tempFile = tempFileUploadMapper.selectById(fileId);
            if (tempFile == null) {
                throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, "文件不存在", null);
            }
            
            // 3. 复制到正式作业附件表
            WorkAttachment attachment = new WorkAttachment();
            attachment.setWorkId(workId);
            attachment.setFileName(tempFile.getFileName());
            attachment.setFilePath(tempFile.getFilePath());
            attachment.setFileSize(tempFile.getFileSize());
            attachment.setFileType(tempFile.getFileType());
            attachment.setUploadTime(LocalDateTime.now());
            workAttachmentMapper.insert(attachment);
            
            log.info("Saved work attachment from temp file: id={}, name={}", fileId, tempFile.getFileName());
        }
    }
    
    /**
     * 保存作业附件（旧方法，保留兼容）
     */
    private void saveWorkAttachments(Integer workId, List<String> attachmentPaths) {
        if (attachmentPaths == null || attachmentPaths.isEmpty()) {
            return;
        }
        
        for (String filePath : attachmentPaths) {
            try {
                // 1. 获取文件信息
                Path path = Paths.get(filePath);
                long fileSize = Files.size(path);
                String fileName = path.getFileName().toString();
                
                // 2. 执行完整的安全检查
                FileUploadValidator.performFullSecurityCheck(filePath, fileSize);
                
                // 3. 获取文件类型
                String fileType = FileUploadValidator.detectFileType(filePath);
                
                // 4. 保存到数据库
                WorkAttachment attachment = new WorkAttachment();
                attachment.setWorkId(workId);
                attachment.setFileName(fileName);
                attachment.setFilePath(filePath);
                attachment.setFileSize(fileSize);
                attachment.setFileType(fileType);
                attachment.setUploadTime(LocalDateTime.now());
                workAttachmentMapper.insert(attachment);
                
                log.info("Saved work attachment: {}, size: {}, type: {}", 
                        fileName, fileSize, fileType);
                        
            } catch (BusinessException e) {
                log.error("File security check failed: {}", filePath, e);
                throw e;
            } catch (Exception e) {
                log.error("Failed to save work attachment: {}", filePath, e);
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
     * @param newAttachmentIds 新增的附件ID列表
     */
    private void handleAttachmentUpdates(Integer workId, List<Integer> removedAttachmentIds, List<Integer> newAttachmentIds) {
        // 1. 删除指定的附件
        if (removedAttachmentIds != null && !removedAttachmentIds.isEmpty()) {
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
        if (newAttachmentIds != null && !newAttachmentIds.isEmpty()) {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser != null) {
                saveWorkAttachmentsWithValidation(workId, newAttachmentIds, currentUser.getId());
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
}
