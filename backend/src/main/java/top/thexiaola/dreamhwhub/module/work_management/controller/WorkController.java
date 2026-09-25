package top.thexiaola.dreamhwhub.module.work_management.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.config.GlobalExceptionHandler;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.work_management.dto.*;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 作业管理控制器
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/works")
@RequiredArgsConstructor
public class WorkController {
    private final WorkService workService;

    /**
     * 创建作业
     */
    @PostMapping(value = "", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<WorkResponse>> createWork(
            @Validated CreateWorkRequest request,
            @RequestParam(value = "questionsJson", required = false) String questionsJson,
            @RequestParam(value = "examConfigJson", required = false) String examConfigJson) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            request.setQuestions(parseQuestions(questionsJson));
            request.setExamConfig(parseExamConfig(examConfigJson));
            request.validate();

            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);

            WorkResponse workInfo = workService.createWork(request);
            log.info("User ({}) created work: {}", userInfo, workInfo.getTitle());
            return ResponseEntity.ok(ApiResponse.success(workInfo));
        } catch (BusinessException e) {
            log.warn("User create work failed: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        } catch (Exception e) {
            log.error("User create work error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求处理失败，请稍后重试"));
        }
    }

    /**
     * 更新作业
     */
    @PutMapping(value = "/{workId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<WorkResponse>> updateWork(
            @PathVariable(value = "workId") Integer workId,
            @Validated UpdateWorkRequest request,
            @RequestParam(value = "questionsJson", required = false) String questionsJson,
            @RequestParam(value = "questionsProvided", required = false) Boolean questionsProvided,
            @RequestParam(value = "examConfigJson", required = false) String examConfigJson,
            @RequestParam(value = "examConfigProvided", required = false) Boolean examConfigProvided) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            // 仅当请求显式携带题目字段时才整体替换（空数组=清除题目）
            if (Boolean.TRUE.equals(questionsProvided)) {
                request.setQuestions(parseQuestions(questionsJson));
            }
            // 仅当请求显式携带考试配置时才覆盖
            if (Boolean.TRUE.equals(examConfigProvided)) {
                request.setExamConfig(parseExamConfig(examConfigJson));
            }
            request.validate();

            request.setId(workId);

            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);

            WorkResponse workInfo = workService.updateWork(request);
            log.info("User ({}) updated work: {}", userInfo, workInfo.getTitle());
            return ResponseEntity.ok(ApiResponse.success(workInfo));
        } catch (BusinessException e) {
            log.warn("User update work failed: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        } catch (Exception e) {
            log.error("User update work error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求处理失败，请稍后重试"));
        }
    }

    /**
     * 解析 multipart 表单携带的考试配置 JSON（可为空，表示非考试或沿用默认）
     */
    private ExamConfigDto parseExamConfig(String examConfigJson) {
        if (examConfigJson == null || examConfigJson.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.toBean(examConfigJson, ExamConfigDto.class);
        } catch (Exception e) {
            throw new BusinessException(
                    top.thexiaola.dreamhwhub.enums.BusinessErrorCode.PARAMETER_ERROR,
                    "考试配置数据格式不正确", null);
        }
    }

    /**
     * 解析 multipart 表单携带的题目 JSON 字符串为题目列表。
     *
     * @param questionsJson 题目 JSON（可为空，表示纯文本作业）
     * @return 题目列表；JSON 为空白时返回空列表
     */
    private List<QuestionItem> parseQuestions(String questionsJson) {
        if (questionsJson == null || questionsJson.isBlank()) {
            return List.of();
        }
        try {
            return JSONUtil.toList(JSONUtil.parseArray(questionsJson), QuestionItem.class);
        } catch (Exception e) {
            throw new BusinessException(
                    top.thexiaola.dreamhwhub.enums.BusinessErrorCode.PARAMETER_ERROR,
                    "题目数据格式不正确", null);
        }
    }

    /**
     * 删除作业
     */
    @DeleteMapping(value = "/{workId}")
    public ResponseEntity<ApiResponse<Void>> deleteWork(@PathVariable(value = "workId") Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);

            workService.deleteWork(workId);
            log.info("User ({}) deleted work, id: {}", userInfo, workId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            log.warn("User delete work failed: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 查询作业详情
     */
    @GetMapping(value = "/{workId}")
    public ResponseEntity<ApiResponse<WorkResponse>> getWorkDetail(@PathVariable(value = "workId") Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);

            WorkResponse workInfo = workService.getWorkById(workId);
            log.info("User ({}) queried work detail, id: {}", userInfo, workId);
            return ResponseEntity.ok(ApiResponse.success(workInfo));
        } catch (BusinessException e) {
            log.warn("User query work detail failed: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 查询作业列表
     */
    @GetMapping(value = "")
    public ResponseEntity<ApiResponse<Page<WorkResponse>>> getWorkList(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "classId", required = false) Integer classId,
            @Validated @ModelAttribute(value = "pageRequest") PageRequest pageRequest) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            Page<WorkResponse> works = workService.getWorkList(status, classId, pageRequest.getPageNum(),
                    pageRequest.getPageSize());
            log.info("User ({}) queried work list, total: {}", userInfo, works.getTotal());
            return ResponseEntity.ok(ApiResponse.success(works));
        } catch (BusinessException e) {
            log.warn("User query work list failed: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 置顶/取消置顶作业
     */
    @PatchMapping(value = "/{workId}/pin")
    public ResponseEntity<ApiResponse<WorkResponse>> pinWork(
            @PathVariable(value = "workId") Integer workId,
            @Validated @RequestBody PinWorkRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            request.setWorkId(workId);

            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);

            WorkResponse workInfo = workService.pinWork(request.getWorkId(), request.getIsPinned());
            String action = request.getIsPinned() ? "pinned" : "unpinned";
            log.info("User ({}) {} work, id: {}", userInfo, action, request.getWorkId());
            return ResponseEntity.ok(ApiResponse.success(workInfo));
        } catch (BusinessException e) {
            log.warn("User pin work failed: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }
}
