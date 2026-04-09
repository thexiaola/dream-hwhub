package top.thexiaola.dreamhwhub.module.work_management.service;

import top.thexiaola.dreamhwhub.module.work_management.domain.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.dto.GradeWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.SubmitWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionResponse;

import java.util.List;

/**
 * 作业提交服务接口
 */
public interface WorkSubmissionService {

    /**
     * 提交作业
     *
     * @param request 提交作业请求
     * @return 提交的作业
     */
    WorkSubmission submitWork(SubmitWorkRequest request);

    /**
     * 更新提交的作业
     *
     * @param submissionId 提交 ID
     * @param submissionContent 提交内容
     * @return 更新后的提交
     */
    WorkSubmission updateSubmission(Integer submissionId, String submissionContent);

    /**
     * 删除提交的作业
     *
     * @param submissionId 提交 ID
     */
    void deleteSubmission(Integer submissionId);

    /**
     * 根据 ID 查询提交
     *
     * @param submissionId 提交 ID
     * @return 提交信息
     */
    WorkSubmission getSubmissionById(Integer submissionId);

    /**
     * 查询学生的提交列表
     *
     * @param studentNo 学生学号
     * @param workId 作业 ID（可选）
     * @return 提交列表
     */
    List<WorkSubmissionResponse> getStudentSubmissions(String studentNo, Integer workId);

    /**
     * 查询某次作业的所有提交
     *
     * @param workId 作业 ID
     * @return 提交列表
     */
    List<WorkSubmissionResponse> getWorkSubmissions(Integer workId);

    /**
     * 批改作业
     *
     * @param request 批改作业请求
     * @return 批改后的提交
     */
    WorkSubmission gradeWork(GradeWorkRequest request);

    /**
     * 查询某次作业的已交名单
     *
     * @param workId 作业 ID
     * @return 已交学生列表
     */
    List<WorkSubmissionResponse> getSubmittedStudents(Integer workId);

    /**
     * 查询某次作业的未交名单
     *
     * @param workId 作业 ID
     * @return 未交学生列表（仅包含学生基本信息）
     */
    List<top.thexiaola.dreamhwhub.module.login.domain.User> getUnsubmittedStudents(Integer workId);
}
