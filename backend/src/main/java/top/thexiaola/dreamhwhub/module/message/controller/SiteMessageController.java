package top.thexiaola.dreamhwhub.module.message.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.message.dto.SiteMessageQueryRequest;
import top.thexiaola.dreamhwhub.module.message.service.SiteMessageService;
import top.thexiaola.dreamhwhub.module.message.vo.SiteMessageResponse;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 站内信控制器（当前用户个人消息）
 * <p>
 * 站内信按学校隔离，且仅接收人本人可读，因此不做权限节点控制。
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class SiteMessageController {

    private final SiteMessageService siteMessageService;

    /**
     * 分页查询我的站内信（支持学校 / 类型 / 已读 / 关键字筛选）
     */
    @GetMapping
    public ApiResponse<Page<SiteMessageResponse>> listMyMessages(
            @Valid @ModelAttribute SiteMessageQueryRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        Page<SiteMessageResponse> messages = siteMessageService.listMyMessages(request);
        log.info("User {} queried {} site messages, schoolId={}, type={}, isRead={}, keyword={}",
                LogUtil.getUserInfo(currentUser), messages.getTotal(),
                request.getSchoolId(), request.getType(), request.getIsRead(), request.getKeyword());
        return ApiResponse.success(messages);
    }

    /**
     * 我的各学校未读站内信数量（用于侧边栏角标）
     */
    @GetMapping("/unread-count")
    public ApiResponse<List<SiteMessageService.UnreadCount>> countUnreadBySchool() {
        User currentUser = UserUtils.getCurrentUser();
        List<SiteMessageService.UnreadCount> counts = siteMessageService.countUnreadBySchool();
        log.info("User {} queried unread messages, {} schools", LogUtil.getUserInfo(currentUser), counts.size());
        return ApiResponse.success(counts);
    }

    /**
     * 将某条站内信标记为已读
     */
    @PutMapping("/{messageId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable(value = "messageId") Integer messageId) {
        User currentUser = UserUtils.getCurrentUser();
        siteMessageService.markAsRead(messageId);
        log.info("User {} marked message {} as read", LogUtil.getUserInfo(currentUser), messageId);
        return ApiResponse.success(null, "已标记为已读");
    }

    /**
     * 将我的站内信全部标记为已读（可按学校限定）
     */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(
            @RequestParam(value = "schoolId", required = false) Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        siteMessageService.markAllAsRead(schoolId);
        log.info("User {} marked all messages as read, schoolId={}", LogUtil.getUserInfo(currentUser), schoolId);
        return ApiResponse.success(null, "已全部标记为已读");
    }
}
