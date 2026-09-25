package top.thexiaola.dreamhwhub.module.work_management.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.config.GlobalExceptionHandler;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.work_management.dto.ReportViolationRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.SaveExamDraftRequest;
import top.thexiaola.dreamhwhub.module.work_management.service.ExamSessionService;
import top.thexiaola.dreamhwhub.module.work_management.vo.ExamEnterResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.ExamViolationVO;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 考试控制器
 * <p>
 * 负责学生进入考试（下发题目/限时/反作弊配置/打乱字体）、违规上报与草稿保存，
 * 以及教师查看违规明细。
 */
@Slf4j
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamSessionService examSessionService;

    /**
     * 进入考试：创建或恢复会话，返回题目、限时与反作弊配置
     */
    @PostMapping("/{workId}/enter")
    public ResponseEntity<ApiResponse<ExamEnterResponse>> enterExam(@PathVariable("workId") Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            ExamEnterResponse response = examSessionService.enterExam(workId);
            log.info("User ({}) entered exam {}", LogUtil.getUserInfoString(ip, currentUser), workId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            log.warn("User enter exam failed: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 上报一次违规；返回是否已达到违规上限（达到即应自动交卷）
     */
    @PostMapping("/{workId}/violations")
    public ResponseEntity<ApiResponse<Boolean>> reportViolation(
            @PathVariable("workId") Integer workId,
            @Valid @RequestBody ReportViolationRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            boolean reached = examSessionService.reportViolation(workId, request);
            log.info("User ({}) reported exam violation {} on {} (reachedLimit={})",
                    LogUtil.getUserInfoString(ip, currentUser), request.getType(), workId, reached);
            return ResponseEntity.ok(ApiResponse.success(reached));
        } catch (BusinessException e) {
            log.warn("Report exam violation failed: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 保存答题草稿（刷新/断线后可恢复）
     */
    @PutMapping("/{workId}/draft")
    public ResponseEntity<ApiResponse<Void>> saveDraft(
            @PathVariable("workId") Integer workId,
            @Valid @RequestBody SaveExamDraftRequest request) {
        try {
            examSessionService.saveDraft(workId, request.getDraft());
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 查询某场考试的违规记录（教师侧）
     */
    @GetMapping("/{workId}/violations")
    public ResponseEntity<ApiResponse<List<ExamViolationVO>>> listViolations(
            @PathVariable("workId") Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            List<ExamViolationVO> violations = examSessionService.listViolations(workId);
            log.info("User ({}) queried {} exam violations for work {}",
                    LogUtil.getUserInfoString(ip, currentUser), violations.size(), workId);
            return ResponseEntity.ok(ApiResponse.success(violations));
        } catch (BusinessException e) {
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 下载反作弊打乱字体（WOFF2）。
     * <p>
     * 字体本身不含任何明文，仅把「显示码点」映射到真实字形；配合服务端打乱后的文本使用。
     * 需登录（AuthInterceptor 拦截 /api/**），GET 不受 CSRF 限制。
     */
    @GetMapping("/font/{seed}.woff2")
    public ResponseEntity<Resource> downloadFont(@PathVariable("seed") Integer seed) {
        String path = "exam-font/pool/" + seed + ".woff2";
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("font/woff2"))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }
}
