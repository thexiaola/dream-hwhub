package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.work_management.domain.Work;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.dto.GradeWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.SubmitWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.WorkSubmissionResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkSubmissionService;
import top.thexiaola.dreamhwhub.util.UserUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业提交服务实现类
 */
@Service
public class WorkSubmissionServiceImpl implements WorkSubmissionService {

    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkMapper workMapper;

    public WorkSubmissionServiceImpl(WorkSubmissionMapper workSubmissionMapper, WorkMapper workMapper) {
        this.workSubmissionMapper = workSubmissionMapper;
        this.workMapper = workMapper;
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
                .eq("student_no", currentUser.getUserNo());
        WorkSubmission existingSubmission = workSubmissionMapper.selectOne(queryWrapper);
        
        if (existingSubmission != null) {
            throw new BusinessException(BusinessErrorCode.WORK_ALREADY_SUBMITTED, "您已经提交过该作业", null);
        }

        // 创建提交记录
        WorkSubmission submission = new WorkSubmission();
        submission.setWorkId(request.getWorkId());
        submission.setStudentNo(currentUser.getUserNo());
        submission.setStudentName(currentUser.getIdName());
        submission.setSubmissionContent(request.getSubmissionContent());
        submission.setStatus(1); // 已提交
        submission.setIsOverdue(work.getDeadline() != null && LocalDateTime.now().isAfter(work.getDeadline()));
        submission.setSubmitTime(LocalDateTime.now());
        submission.setCreateTime(LocalDateTime.now());
        submission.setUpdateTime(LocalDateTime.now());

        workSubmissionMapper.insert(submission);
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
        if (!submission.getStudentNo().equals(currentUser.getUserNo())) {
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
            if (!submission.getStudentNo().equals(currentUser.getUserNo())) {
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
        QueryWrapper<WorkSubmission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("student_no", studentNo);
        
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
        submission.setGradeTeacherNo(currentUser.getUserNo());
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
        response.setStudentNo(submission.getStudentNo());
        response.setStudentName(submission.getStudentName());
        response.setSubmissionContent(submission.getSubmissionContent());
        response.setScore(submission.getScore());
        response.setComment(submission.getComment());
        response.setSubmitTime(submission.getSubmitTime());
        response.setGradeTime(submission.getGradeTime());
        response.setGradeTeacherNo(submission.getGradeTeacherNo());
        response.setStatus(submission.getStatus());
        response.setIsOverdue(submission.getIsOverdue());
        response.setCreateTime(submission.getCreateTime());
        response.setUpdateTime(submission.getUpdateTime());
        return response;
    }
}
