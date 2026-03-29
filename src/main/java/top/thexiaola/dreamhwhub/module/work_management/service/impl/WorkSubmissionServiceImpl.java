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
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkSubmissionAttachment;
import top.thexiaola.dreamhwhub.module.work_management.dto.GradeWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.SubmitWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.WorkSubmissionResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkSubmissionService;
import top.thexiaola.dreamhwhub.util.FileUploadValidator;
import top.thexiaola.dreamhwhub.util.UserUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业提交服务实现类
 */
@Service
public class WorkSubmissionServiceImpl implements WorkSubmissionService {
    private static final Logger log = LoggerFactory.getLogger(WorkSubmissionServiceImpl.class);

    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkMapper workMapper;
    private final WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper;
    private final UserMapper userMapper;

    public WorkSubmissionServiceImpl(WorkSubmissionMapper workSubmissionMapper, WorkMapper workMapper, WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper, UserMapper userMapper) {
        this.workSubmissionMapper = workSubmissionMapper;
        this.workMapper = workMapper;
        this.workSubmissionAttachmentMapper = workSubmissionAttachmentMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkSubmission submitWork(SubmitWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限（学生才能提交作业）
        if (currentUser.getPermission() >= 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "教师不能提交作业", null);
        }

        // 查询作业
        Work work = workMapper.selectById(request.getWorkId());
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查作业状态
        if (work.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, "作业未发布或已结束", null);
        }

        // 检查是否已提交过
        QueryWrapper<WorkSubmission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("work_id", request.getWorkId())
                .eq("submitter_id", currentUser.getId());
        WorkSubmission existingSubmission = workSubmissionMapper.selectOne(queryWrapper);
        
        if (existingSubmission != null) {
            throw new BusinessException(BusinessErrorCode.WORK_ALREADY_SUBMITTED, "您已经提交过该作业", null);
        }

        // 创建提交记录
        WorkSubmission submission = new WorkSubmission();
        submission.setWorkId(request.getWorkId());
        submission.setSubmitterId(currentUser.getId());
        submission.setSubmissionContent(request.getSubmissionContent());
        submission.setStatus(1); // 已提交
        submission.setIsOverdue(work.getDeadline() != null && LocalDateTime.now().isAfter(work.getDeadline()));
        submission.setSubmitTime(LocalDateTime.now());
        submission.setCreateTime(LocalDateTime.now());
        submission.setUpdateTime(LocalDateTime.now());

        workSubmissionMapper.insert(submission);
        
        // 保存附件
        if (request.getAttachmentPaths() != null && !request.getAttachmentPaths().isEmpty()) {
            saveSubmissionAttachments(submission.getId(), request.getAttachmentPaths());
        }
        
        return submission;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkSubmission updateSubmission(Integer submissionId, String submissionContent) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 查询提交记录
        WorkSubmission submission = workSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }

        // 只能修改自己的提交
        if (!submission.getSubmitterId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能修改自己的提交", null);
        }

        // 如果已经被批改，不能修改
        if (submission.getStatus() == 2) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_ALREADY_GRADED, "作业已被批改，不能修改", null);
        }

        // 更新提交内容
        submission.setSubmissionContent(submissionContent);
        submission.setUpdateTime(LocalDateTime.now());

        workSubmissionMapper.updateById(submission);
        return submission;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubmission(Integer submissionId) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 查询提交记录
        WorkSubmission submission = workSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }

        // 如果是学生，只能删除自己的提交
        if (currentUser.getPermission() < 2) {
            if (!submission.getSubmitterId().equals(currentUser.getId())) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能删除自己的提交", null);
            }
        }

        workSubmissionMapper.deleteById(submissionId);
    }

    @Override
    public WorkSubmission getSubmissionById(Integer submissionId) {
        WorkSubmission submission = workSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }
        return submission;
    }

    @Override
    public List<WorkSubmissionResponse> getStudentSubmissions(String studentNo, Integer workId) {
        if (studentNo == null || studentNo.isEmpty()) {
            return List.of();
        }
        
        // 根据学号查询用户 ID
        Integer studentId = getUserIdByUserNo(studentNo);
        if (studentId == null) {
            // 如果学生不存在，返回空列表
            return List.of();
        }
        
        QueryWrapper<WorkSubmission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("submitter_id", studentId);
        
        if (workId != null) {
            queryWrapper.eq("work_id", workId);
        }
        
        queryWrapper.orderByDesc("submit_time");
        
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(queryWrapper);
        
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkSubmissionResponse> getWorkSubmissions(Integer workId) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限（只有教师可以查看所有提交）
        if (currentUser.getPermission() < 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有教师可以查看所有提交", null);
        }

        QueryWrapper<WorkSubmission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("work_id", workId).orderByDesc("submit_time");
        
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(queryWrapper);
        
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkSubmission gradeWork(GradeWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限（只有教师可以批改作业）
        if (currentUser.getPermission() < 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有教师可以批改作业", null);
        }

        // 查询提交记录
        WorkSubmission submission = workSubmissionMapper.selectById(request.getSubmissionId());
        if (submission == null) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }

        // 查询作业信息
        Work work = workMapper.selectById(submission.getWorkId());
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查分数范围
        if (request.getScore().compareTo(new java.math.BigDecimal(work.getTotalScore())) > 0) {
            throw new BusinessException(BusinessErrorCode.SCORE_OUT_OF_RANGE, "分数超过作业总分", null);
        }

        // 批改作业
        submission.setScore(request.getScore());
        submission.setComment(request.getComment());
        submission.setStatus(2); // 已批改
        submission.setGradeTime(LocalDateTime.now());
        submission.setGraderId(currentUser.getId());
        submission.setUpdateTime(LocalDateTime.now());

        workSubmissionMapper.updateById(submission);
        return submission;
    }

    /**
     * 转换为响应对象
     */
    private WorkSubmissionResponse convertToResponse(WorkSubmission submission) {
        Work work = workMapper.selectById(submission.getWorkId());
        WorkSubmissionResponse response = new WorkSubmissionResponse();
        response.setId(submission.getId());
        response.setWorkId(submission.getWorkId());
        response.setWorkTitle(work != null ? work.getTitle() : null);
        response.setSubmitterId(submission.getSubmitterId());
        response.setSubmissionContent(submission.getSubmissionContent());
        response.setScore(submission.getScore());
        response.setComment(submission.getComment());
        response.setSubmitTime(submission.getSubmitTime());
        response.setGradeTime(submission.getGradeTime());
        response.setGraderId(submission.getGraderId());
        response.setStatus(submission.getStatus());
        response.setIsOverdue(submission.getIsOverdue());
        response.setCreateTime(submission.getCreateTime());
        response.setUpdateTime(submission.getUpdateTime());
        
        // 加载附件列表
        List<WorkSubmissionResponse.AttachmentInfo> attachments = getSubmissionAttachments(submission.getId());
        response.setAttachments(attachments);
        
        return response;
    }
    
    /**
     * 保存提交附件
     */
    private void saveSubmissionAttachments(Integer submissionId, List<String> attachmentPaths) {
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
                WorkSubmissionAttachment attachment = new WorkSubmissionAttachment();
                attachment.setSubmissionId(submissionId);
                attachment.setFileName(fileName);
                attachment.setFilePath(filePath);
                attachment.setFileSize(fileSize);
                attachment.setFileType(fileType);
                attachment.setUploadTime(LocalDateTime.now());
                workSubmissionAttachmentMapper.insert(attachment);
                
                log.info("Saved submission attachment: {}, size: {}, type: {}", 
                        fileName, fileSize, fileType);
                        
            } catch (BusinessException e) {
                log.error("File security check failed: {}", filePath, e);
                throw e;
            } catch (Exception e) {
                log.error("Failed to save submission attachment: {}", filePath, e);
                throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, 
                        "文件上传失败：" + e.getMessage(), null);
            }
        }
    }
    
    /**
     * 获取提交附件列表
     */
    private List<WorkSubmissionResponse.AttachmentInfo> getSubmissionAttachments(Integer submissionId) {
        QueryWrapper<WorkSubmissionAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("submission_id", submissionId);
        List<WorkSubmissionAttachment> attachments = workSubmissionAttachmentMapper.selectList(queryWrapper);
        
        return attachments.stream()
                .map(attachment -> new WorkSubmissionResponse.AttachmentInfo(
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
     * 根据学号/工号查询用户 ID
     * @param userNo 学号或工号
     * @return 用户 ID，不存在返回 null
     */
    private Integer getUserIdByUserNo(String userNo) {
        // 验证参数
        if (userNo == null || userNo.trim().isEmpty()) {
            return null;
        }
        
        // 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", userNo);
        User user = userMapper.selectOne(queryWrapper);
        
        // 返回用户 ID
        return user != null ? user.getId() : null;
    }
}
