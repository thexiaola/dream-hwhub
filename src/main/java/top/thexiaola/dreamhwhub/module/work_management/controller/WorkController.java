package top.thexiaola.dreamhwhub.module.work_management.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkInfo;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

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
    public ResponseEntity<ApiResponse<WorkInfo>> createWork(@Valid @RequestBody CreateWorkRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            
            WorkInfo workInfo = workService.createWork(request);
            log.info("User ({}) created work: {}", userInfo, workInfo.getTitle());
            return ResponseEntity.ok(ApiResponse.success(workInfo));
        } catch (BusinessException e) {
            log.warn("User create work failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 更新作业
     */
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<WorkInfo>> updateWork(@Valid @RequestBody UpdateWorkRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            
            WorkInfo workInfo = workService.updateWork(request);
            log.info("User ({}) updated work: {}", userInfo, workInfo.getTitle());
            return ResponseEntity.ok(ApiResponse.success(workInfo));
        } catch (BusinessException e) {
            log.warn("User update work failed: {}", e.getMessage());
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
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            
            workService.deleteWork(workId);
            log.info("User ({}) deleted work, id: {}", userInfo, workId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (BusinessException e) {
            log.warn("User delete work failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询作业详情
     */
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<WorkInfo>> getWorkDetail(@RequestParam Integer workId) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            
            WorkInfo workInfo = workService.getWorkById(workId);
            log.info("User ({}) queried work detail, id: {}", userInfo, workId);
            return ResponseEntity.ok(ApiResponse.success(workInfo));
        } catch (BusinessException e) {
            log.warn("User query work detail failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 查询作业列表
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkResponse>>> getWorkList(
            @RequestParam(required = false) String publisherUserNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, currentUser);
            
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<WorkResponse> works = workService.getWorkList(publisherUserNo, status, pageNum, pageSize);
            log.info("User ({}) queried work list, total: {}", userInfo, works.getTotal());
            return ResponseEntity.ok(ApiResponse.success(works));
        } catch (BusinessException e) {
            log.warn("User query work list failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}
