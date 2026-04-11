package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkAttachment;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkInfo;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.support.validation.FileUploadValidator;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

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
    private final ClassService classService;
    private final UserMapper userMapper;

    public WorkServiceImpl(WorkMapper workMapper, WorkAttachmentMapper workAttachmentMapper, ClassService classService, UserMapper userMapper) {
        this.workMapper = workMapper;
        this.workAttachmentMapper = workAttachmentMapper;
        this.classService = classService;
        this.userMapper = userMapper;
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
        workInfo.setPublishTime(request.getPublishTime());
        workInfo.setCreateTime(LocalDateTime.now());
        workInfo.setUpdateTime(LocalDateTime.now());

        workMapper.insert(workInfo);
        
        // 保存附件
        if (request.getAttachmentPaths() != null && !request.getAttachmentPaths().isEmpty()) {
            saveWorkAttachments(workInfo.getId(), request.getAttachmentPaths());
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

        // 更新作业
        workInfo.setTitle(request.getTitle());
        workInfo.setDescription(request.getDescription());
        workInfo.setDeadline(request.getDeadline());
        workInfo.setTotalScore(request.getTotalScore());
        if (request.getPublishTime() != null) {
            workInfo.setPublishTime(request.getPublishTime());
        }
        workInfo.setUpdateTime(LocalDateTime.now());

        workMapper.updateById(workInfo);
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

        workMapper.deleteById(workId);
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
    public List<WorkResponse> getWorkList(String publisherUserNo, Integer status) {
        User currentUser = UserUtils.getCurrentUser();
        QueryWrapper<WorkInfo> queryWrapper = new QueryWrapper<>();
        
        if (publisherUserNo != null && !publisherUserNo.isEmpty()) {
            Integer publisherId = getUserIdByUserNo(publisherUserNo);
            if (publisherId != null) {
                queryWrapper.eq("publisher_id", publisherId);
            } else {
                // 如果用户不存在，返回空列表
                return List.of();
            }
        }
        
        queryWrapper.orderByDesc("create_time");
        
        List<WorkInfo> workInfos = workMapper.selectList(queryWrapper);
        LocalDateTime now = LocalDateTime.now();
        
        return workInfos.stream()
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
     * 保存作业附件
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
}
