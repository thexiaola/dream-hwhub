package top.thexiaola.dreamhwhub.module.login.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.config.GlobalExceptionHandler;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.security.SensitiveOperationSettingsService;
import top.thexiaola.dreamhwhub.support.security.dto.UpdateSensitiveOperationsRequest;
import top.thexiaola.dreamhwhub.support.security.vo.SensitiveOperationSettingVO;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 敏感操作验证设置控制器
 * <p>
 * 用户可逐个设置「哪些危险操作需要二次验证」。列表只返回对当前用户**可用**的操作
 * （该用户当前确实能执行的操作），不可用的操作既不展示开关，也不允许配置。
 */
@Slf4j
@RestController
@RequestMapping("/api/users/sensitive-operations")
@RequiredArgsConstructor
public class SensitiveOperationSettingsController {

    private final SensitiveOperationSettingsService settingsService;

    /**
     * 查询当前用户可见的敏感操作及其验证开关
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SensitiveOperationSettingVO>>> listSettings() {
        try {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "用户未登录"));
            }
            List<SensitiveOperationSettingVO> settings = settingsService.listSettings(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(settings));
        } catch (BusinessException e) {
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }

    /**
     * 更新当前用户对各敏感操作的验证开关
     */
    @PutMapping
    public ResponseEntity<ApiResponse<List<SensitiveOperationSettingVO>>> updateSettings(
            @RequestBody UpdateSensitiveOperationsRequest request) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User currentUser = UserUtils.getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(401).body(ApiResponse.error(401, "用户未登录"));
            }
            settingsService.updateSettings(currentUser.getId(), request);
            log.info("User ({}) updated sensitive-operation verification settings",
                    LogUtil.getUserInfoString(ip, currentUser));
            return ResponseEntity.ok(ApiResponse.success(
                    settingsService.listSettings(currentUser.getId()), "操作验证设置已更新"));
        } catch (BusinessException e) {
            log.warn("User failed to update sensitive-operation verification settings: {}", e.getMessage());
            return GlobalExceptionHandler.buildBusinessErrorResponse(e);
        }
    }
}
