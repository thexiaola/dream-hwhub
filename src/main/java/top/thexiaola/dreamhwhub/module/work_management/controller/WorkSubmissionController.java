package top.thexiaola.dreamhwhub.module.work_management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.dto.GradeWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.SubmitWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkSubmissionService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionResponse;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

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
    @PostMapping(value = "/submit", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<WorkSubmission>> submitWork(
            @RequestParam("workId") String workId,
            @RequestParam(value = "submissionContent", required = false) String submissionContent,
            @RequestParam(value = "attachments", required = false) List<org.springframework.web.multipart.MultipartFile> attachments) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            // 参数验证
            if (workId == null || workId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "作业 ID 不能为空"));
            }
            if (!workId.matches("^[0-9]+$")) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "作业 ID 必须是数字"));
            }
            
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            // 构建请求对象
            SubmitWorkRequest request = new SubmitWorkRequest();
            request.setWorkId(workId);
            request.setSubmissionContent(submissionContent);
            request.setAttachments(attachments);
            
            WorkSubmission submission = workSubmissionService.submitWork(request);
            log.info("User ({}) submitted work, id: {}", userInfo, submission.getId());
            return ResponseEntity.ok(ApiResponse.success(submission));
        } catch (BusinessException e) {
            log.warn("User submit work failed: {}", e.getMessage());
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
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            WorkSubmission submission = workSubmissionService.updateSubmission(submissionId, submissionContent);
            log.info("User ({}) updated submission, id: {}", userInfo, submission.getId());
            return ResponseEntity.ok(ApiResponse.success(submission));
        } catch (BusinessException e) {
            log.warn("User update submission failed: {}", e.getMessage());
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
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            workSubmissionService.deleteSubmission(submissionId);
            log.info("User ({}) deleted submission, id: {}", userInfo, submissionId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            log.warn("User delete submission failed: {}", e.getMessage());
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
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            WorkSubmission submission = workSubmissionService.getSubmissionById(submissionId);
            log.info("User ({}) queried submission detail, id: {}", userInfo, submissionId);
            return ResponseEntity.ok(ApiResponse.success(submission));
        } catch (BusinessException e) {
            log.warn("User query submission detail failed: {}", e.getMessage());
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
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400, "用户未登录"));
            }
            List<WorkSubmissionResponse> submissions = workSubmissionService.getStudentSubmissions(user.getUserNo(), workId);
            log.info("User ({}) queried student submissions, size: {}", userInfo, submissions.size());
            return ResponseEntity.ok(ApiResponse.success(submissions));
        } catch (BusinessException e) {
            log.warn("User query student submissions failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询某次作业的所有提交（教师专用，分页）
     */
    @GetMapping("/work/list")
    public ResponseEntity<ApiResponse<Page<WorkSubmissionResponse>>> getWorkSubmissions(
            @RequestParam Integer workId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            Page<WorkSubmissionResponse> submissions = workSubmissionService.getWorkSubmissions(workId, pageNum, pageSize);
            log.info("User ({}) queried work submissions, workId: {}, total: {}", userInfo, workId, submissions.getTotal());
            return ResponseEntity.ok(ApiResponse.success(submissions));
        } catch (BusinessException e) {
            log.warn("User query work submissions failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询某次作业的已交名单（教师专用）
     */
    @GetMapping("/work/submitted")
    public ResponseEntity<ApiResponse<List<WorkSubmissionResponse>>> getSubmittedStudents(@RequestParam Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            List<WorkSubmissionResponse> submitted = workSubmissionService.getSubmittedStudents(workId);
            log.info("User ({}) queried submitted students, workId: {}, size: {}", userInfo, workId, submitted.size());
            return ResponseEntity.ok(ApiResponse.success(submitted));
        } catch (BusinessException e) {
            log.warn("User query submitted students failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询某次作业的未交名单（教师专用）
     */
    @GetMapping("/work/unsubmitted")
    public ResponseEntity<ApiResponse<List<User>>> getUnsubmittedStudents(@RequestParam Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            List<User> unsubmitted = workSubmissionService.getUnsubmittedStudents(workId);
            log.info("User ({}) queried unsubmitted students, workId: {}, size: {}", userInfo, workId, unsubmitted.size());
            return ResponseEntity.ok(ApiResponse.success(unsubmitted));
        } catch (BusinessException e) {
            log.warn("User query unsubmitted students failed: {}", e.getMessage());
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
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            WorkSubmission submission = workSubmissionService.gradeWork(request);
            log.info("User ({}) graded submission, id: {}, score: {}", userInfo, submission.getId(), submission.getScore());
            return ResponseEntity.ok(ApiResponse.success(submission));
        } catch (BusinessException e) {
            log.warn("User grade work failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}
