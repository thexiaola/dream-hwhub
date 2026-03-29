package top.thexiaola.dreamhwhub.module.work_management.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.dto.GradeWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.SubmitWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.WorkSubmissionResponse;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkSubmissionService;
import top.thexiaola.dreamhwhub.util.LogUtil;
import top.thexiaola.dreamhwhub.util.UserUtils;

import java.util.List;

/**
 * 作业提交控制器
 */
@RestController
@RequestMapping("/api/submissions")
public class WorkSubmissionController {
    private static final Logger log = LoggerFactory.getLogger(WorkSubmissionController.class);
    private final WorkSubmissionService workSubmissionService;

    public WorkSubmissionController(WorkSubmissionService workSubmissionService) {
        this.workSubmissionService = workSubmissionService;
    }

    /**
     * 提交作业
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<WorkSubmission>> submitWork(@Valid @RequestBody SubmitWorkRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = UserUtils.getCurrentUser();
            WorkSubmission submission = workSubmissionService.submitWork(request);
            log.info("User ({}) submitted work, id: {}", ip, submission.getId());
            return ResponseEntity.ok(ApiResponse.success(submission));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to submit work: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 更新提交的作业
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<WorkSubmission>> updateSubmission(
            @RequestParam Integer submissionId,
            @RequestParam String submissionContent) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            WorkSubmission submission = workSubmissionService.updateSubmission(submissionId, submissionContent);
            log.info("User ({}) updated submission, id: {}", ip, submission.getId());
            return ResponseEntity.ok(ApiResponse.success(submission));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to update submission: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 删除提交的作业
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteSubmission(@RequestParam Integer submissionId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            workSubmissionService.deleteSubmission(submissionId);
            log.info("User ({}) deleted submission, id: {}", ip, submissionId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to delete submission: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询提交详情
     */
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<WorkSubmission>> getSubmissionDetail(@RequestParam Integer submissionId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            WorkSubmission submission = workSubmissionService.getSubmissionById(submissionId);
            log.info("User ({}) queried submission detail, id: {}", ip, submissionId);
            return ResponseEntity.ok(ApiResponse.success(submission));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to query submission detail: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询学生的提交列表
     */
    @GetMapping("/student/list")
    public ResponseEntity<ApiResponse<List<WorkSubmissionResponse>>> getStudentSubmissions(
            @RequestParam(required = false) Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = UserUtils.getCurrentUser();
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "用户未登录"));
            }
            List<WorkSubmissionResponse> submissions = workSubmissionService.getStudentSubmissions(user.getUserNo(), workId);
            log.info("User ({}) queried student submissions, size: {}", ip, submissions.size());
            return ResponseEntity.ok(ApiResponse.success(submissions));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to query student submissions: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询某次作业的所有提交（教师专用）
     */
    @GetMapping("/work/list")
    public ResponseEntity<ApiResponse<List<WorkSubmissionResponse>>> getWorkSubmissions(@RequestParam Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            List<WorkSubmissionResponse> submissions = workSubmissionService.getWorkSubmissions(workId);
            log.info("User ({}) queried work submissions, workId: {}, size: {}", ip, workId, submissions.size());
            return ResponseEntity.ok(ApiResponse.success(submissions));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to query work submissions: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 批改作业（教师专用）
     */
    @PutMapping("/grade")
    public ResponseEntity<ApiResponse<WorkSubmission>> gradeWork(@Valid @RequestBody GradeWorkRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            WorkSubmission submission = workSubmissionService.gradeWork(request);
            log.info("User ({}) graded submission, id: {}, score: {}", ip, submission.getId(), submission.getScore());
            return ResponseEntity.ok(ApiResponse.success(submission));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to grade submission: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}
