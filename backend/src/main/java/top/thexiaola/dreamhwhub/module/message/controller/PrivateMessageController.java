package top.thexiaola.dreamhwhub.module.message.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.message.dto.SendPrivateMessageRequest;
import top.thexiaola.dreamhwhub.module.message.service.PrivateMessageService;
import top.thexiaola.dreamhwhub.module.message.vo.ConversationInfo;
import top.thexiaola.dreamhwhub.module.message.vo.PrivateMessageInfo;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 私信控制器
 * <p>
 * 私信按学校隔离：会话双方须在同一学校；非好友也可互发，但受陌生私信配额约束。
 */
@Slf4j
@RestController
@RequestMapping("/api/private-messages")
@RequiredArgsConstructor
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;

    /**
     * 我的会话列表（某学校，按最近消息倒序）
     */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationInfo>> listConversations(
            @RequestParam(value = "schoolId") Integer schoolId) {
        User currentUser = UserUtils.getCurrentUser();
        List<ConversationInfo> conversations = privateMessageService.listConversations(schoolId);
        log.info("User {} queried {} conversations in school {}",
                LogUtil.getUserInfo(currentUser), conversations.size(), schoolId);
        return ApiResponse.success(conversations);
    }

    /**
     * 某会话的消息分页（与指定用户的往来私信）
     */
    @GetMapping
    public ApiResponse<Page<PrivateMessageInfo>> listMessages(
            @RequestParam(value = "schoolId") Integer schoolId,
            @RequestParam(value = "peerId") Integer peerId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        Page<PrivateMessageInfo> messages =
                privateMessageService.listMessages(schoolId, peerId, pageNum, pageSize);
        log.info("User {} queried {} messages with peer {} in school {}",
                LogUtil.getUserInfo(currentUser), messages.getTotal(), peerId, schoolId);
        return ApiResponse.success(messages);
    }

    /**
     * 发送私信（非好友时校验陌生私信配额）
     */
    @PostMapping
    public ApiResponse<PrivateMessageInfo> send(@Valid @RequestBody SendPrivateMessageRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        PrivateMessageInfo message = privateMessageService.send(
                request.getSchoolId(), request.getReceiverId(), request.getContent());
        log.info("User {} sent private message {} to user {} in school {}",
                LogUtil.getUserInfo(currentUser), message.getId(), request.getReceiverId(), request.getSchoolId());
        return ApiResponse.success(message, "已发送");
    }

    /**
     * 将某会话中对方发给我的消息标记为已读
     */
    @PutMapping("/read")
    public ApiResponse<Void> markConversationRead(
            @RequestParam(value = "schoolId") Integer schoolId,
            @RequestParam(value = "peerId") Integer peerId) {
        User currentUser = UserUtils.getCurrentUser();
        privateMessageService.markConversationRead(schoolId, peerId);
        log.info("User {} marked conversation with peer {} as read in school {}",
                LogUtil.getUserInfo(currentUser), peerId, schoolId);
        return ApiResponse.success(null, "已标记为已读");
    }

    /**
     * 我的各学校未读私信数量（用于角标）
     */
    @GetMapping("/unread-count")
    public ApiResponse<List<PrivateMessageService.UnreadCount>> countUnreadBySchool() {
        User currentUser = UserUtils.getCurrentUser();
        List<PrivateMessageService.UnreadCount> counts = privateMessageService.countUnreadBySchool();
        log.info("User {} queried unread private messages, {} schools",
                LogUtil.getUserInfo(currentUser), counts.size());
        return ApiResponse.success(counts);
    }
}
