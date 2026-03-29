package top.thexiaola.dreamhwhub.module.work_management.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.dto.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.work_management.domain.Work;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.WorkResponse;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.util.LogUtil;

import java.util.List;

/**
 * 作业管理控制器
 */
@RestController
@RequestMapping("/api/works")
public class WorkController {
    private static final Logger log = LoggerFactory.getLogger(WorkController.class);
    private final WorkService workService;

    public WorkController(WorkService workService) {
        this.workService = workService;
    }

    /**
     * 创建作业
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Work>> createWork(@Valid @RequestBody CreateWorkRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            Work work = workService.createWork(request);
            log.info("User ({}) created work: {}", ip, work.getTitle());
            return ResponseEntity.ok(ApiResponse.success(work));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to create work: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 更新作业
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<Work>> updateWork(@Valid @RequestBody UpdateWorkRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            Work work = workService.updateWork(request);
            log.info("User ({}) updated work: {}", ip, work.getTitle());
            return ResponseEntity.ok(ApiResponse.success(work));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to update work: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 删除作业
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteWork(@RequestParam Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            workService.deleteWork(workId);
            log.info("User ({}) deleted work, id: {}", ip, workId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to delete work: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询作业详情
     */
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Work>> getWorkDetail(@RequestParam Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            Work work = workService.getWorkById(workId);
            log.info("User ({}) queried work detail, id: {}", ip, workId);
            return ResponseEntity.ok(ApiResponse.success(work));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to query work detail: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询作业列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<WorkResponse>>> getWorkList(
            @RequestParam(required = false) String teacherNo,
            @RequestParam(required = false) Integer status) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            List<WorkResponse> works = workService.getWorkList(teacherNo, status);
            log.info("User ({}) queried work list, size: {}", ip, works.size());
            return ResponseEntity.ok(ApiResponse.success(works));
        } catch (BusinessException e) {
            log.warn("User ({}) failed to query work list: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}
