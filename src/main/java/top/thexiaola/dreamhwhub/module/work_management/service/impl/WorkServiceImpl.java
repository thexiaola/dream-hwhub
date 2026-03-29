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
import top.thexiaola.dreamhwhub.module.work_management.domain.Work;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkAttachment;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.WorkResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.util.FileUploadValidator;
import top.thexiaola.dreamhwhub.util.UserUtils;

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
    private final UserMapper userMapper;

    public WorkServiceImpl(WorkMapper workMapper, WorkAttachmentMapper workAttachmentMapper, UserMapper userMapper) {
        this.workMapper = workMapper;
        this.workAttachmentMapper = workAttachmentMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Work createWork(CreateWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限（只有教师可以发布作业）
        if (currentUser.getPermission() < 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有教师可以发布作业", null);
        }

        // 创建作业
        Work work = new Work();
        work.setTitle(request.getTitle());
        work.setDescription(request.getDescription());
        work.setPublisherId(currentUser.getId());
        work.setDeadline(request.getDeadline());
        work.setTotalScore(request.getTotalScore());
        work.setStatus(1); // 已发布
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());

        workMapper.insert(work);
        
        // 保存附件
        if (request.getAttachmentPaths() != null && !request.getAttachmentPaths().isEmpty()) {
            saveWorkAttachments(work.getId(), request.getAttachmentPaths());
        }
        
        return work;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Work updateWork(UpdateWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限
        if (currentUser.getPermission() < 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有教师可以修改作业", null);
        }

        // 查询作业
        Work work = workMapper.selectById(request.getId());
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 只能修改自己发布的作业
        if (!work.getPublisherId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能修改自己发布的作业", null);
        }

        // 更新作业
        work.setTitle(request.getTitle());
        work.setDescription(request.getDescription());
        work.setDeadline(request.getDeadline());
        work.setTotalScore(request.getTotalScore());
        work.setStatus(request.getStatus());
        work.setUpdateTime(LocalDateTime.now());

        workMapper.updateById(work);
        return work;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWork(Integer workId) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限
        if (currentUser.getPermission() < 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有教师可以删除作业", null);
        }

        // 查询作业
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 只能删除自己发布的作业
        if (!work.getPublisherId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能删除自己发布的作业", null);
        }

        workMapper.deleteById(workId);
    }

    @Override
    public Work getWorkById(Integer workId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }
        return work;
    }

    @Override
    public List<WorkResponse> getWorkList(String publisherUserNo, Integer status) {
        QueryWrapper<Work> queryWrapper = new QueryWrapper<>();
        
        if (publisherUserNo != null && !publisherUserNo.isEmpty()) {
            Integer publisherId = getUserIdByUserNo(publisherUserNo);
            if (publisherId != null) {
                queryWrapper.eq("publisher_id", publisherId);
            } else {
                // 如果用户不存在，返回空列表
                return List.of();
            }
        }
        
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        List<Work> works = workMapper.selectList(queryWrapper);
        LocalDateTime now = LocalDateTime.now();
        
        return works.stream()
                .map(work -> {
                    WorkResponse response = new WorkResponse();
                    response.setId(work.getId());
                    response.setTitle(work.getTitle());
                    response.setDescription(work.getDescription());
                    response.setPublisherId(work.getPublisherId());
                    response.setDeadline(work.getDeadline());
                    response.setTotalScore(work.getTotalScore());
                    response.setStatus(work.getStatus());
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
    
    /**
     * 根据工号获取用户 ID
     * @param userNo 学号或工号
     * @return 用户 ID，不存在返回 null
     */
    private Integer getUserIdByUserNo(String userNo) {
        // 验证参数有效性
        if (userNo == null || userNo.trim().isEmpty()) {
            return null;
        }
        
        // 构建查询条件并查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", userNo);
        User user = userMapper.selectOne(queryWrapper);
        
        return user != null ? user.getId() : null;
    }
}
