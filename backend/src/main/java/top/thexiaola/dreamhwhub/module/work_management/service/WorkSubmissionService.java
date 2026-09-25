package top.thexiaola.dreamhwhub.module.work_management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import top.thexiaola.dreamhwhub.module.work_management.dto.*;
import top.thexiaola.dreamhwhub.module.work_management.vo.UnsubmittedStudentResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionSubmitResponse;

import java.util.List;

/**
 * 作业提交服务接口
 */
public interface WorkSubmissionService {

    /**
     * 提交作业
     *
     * @param request 提交作业请求
     * @return 提交的作业（不包含批改信息）
     */
    WorkSubmissionSubmitResponse submitWork(SubmitWorkRequest request);

    /**
     * 更新提交的作业
     *
     * @param submissionId 提交 ID
     * @param submissionContent 提交内容
     * @param attachments 新增的附件文件列表
     * @param removedAttachmentIds 要删除的附件ID列表
     * @param answers 逐题作答（含题目的作业使用；整体替换，客观题重新自动评判）
     * @return 更新后的提交（不包含批改信息）
     */
    WorkSubmissionSubmitResponse updateSubmission(Integer submissionId, String submissionContent, 
                                                   List<org.springframework.web.multipart.MultipartFile> attachments,
                                                   List<Integer> removedAttachmentIds,
                                                   List<AnswerItem> answers);

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
    WorkSubmissionResponse getSubmissionById(Integer submissionId);

    /**
     * 查询学生的提交列表
     *
     * @param studentNo 学生学号
     * @param workId 作业 ID（可选）
     * @return 提交列表
     */
    List<WorkSubmissionResponse> getStudentSubmissions(Integer workId);

    /**
     * 查询某次作业的所有提交（分页）
     *
     * @param workId 作业 ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 提交分页结果
     */
    Page<WorkSubmissionResponse> getWorkSubmissions(Integer workId, Integer pageNum, Integer pageSize);

    /**
     * 批改作业
     *
     * @param request 批改作业请求
     * @return 批改后的提交
     */
    WorkSubmissionResponse gradeWork(GradeWorkRequest request);

    /**
     * 逐题评分（教师专用）
     * <p>
     * 用于主观题/附加题手动评分，也支持对客观题手动改判覆盖自动分。
     * 评分后按题目得分在数据库侧汇总，重算该次提交总分。
     *
     * @param request 逐题评分请求
     * @return 评分后的提交（含逐题作答明细）
     */
    WorkSubmissionResponse gradeAnswers(GradeAnswersRequest request);

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
     * @return 未交学生列表（仅含用户名/邮箱/姓名/学号等展示信息，不含密码等敏感字段）
     */
    List<UnsubmittedStudentResponse> getUnsubmittedStudents(Integer workId);

    /**
     * 批量下载作业附件（打包成ZIP）
     *
     * @param request 批量下载请求
     * @param response HTTP响应对象
     */
    void batchDownloadAttachments(BatchDownloadAttachmentsRequest request, HttpServletResponse response);
}
