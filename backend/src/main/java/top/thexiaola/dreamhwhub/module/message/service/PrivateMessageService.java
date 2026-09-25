package top.thexiaola.dreamhwhub.module.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.thexiaola.dreamhwhub.module.message.vo.ConversationInfo;
import top.thexiaola.dreamhwhub.module.message.vo.PrivateMessageInfo;

import java.util.List;

/**
 * 私信服务接口
 * <p>
 * 私信按学校隔离：会话双方须在同一学校；非好友也可互发，但受陌生私信配额约束。
 */
public interface PrivateMessageService {

    /**
     * 我的会话列表（某学校，按最近消息倒序）
     *
     * @param schoolId 学校 ID
     * @return 会话列表
     */
    List<ConversationInfo> listConversations(Integer schoolId);

    /**
     * 某会话的消息分页（与指定用户的往来私信，按时间倒序）
     *
     * @param schoolId 学校 ID
     * @param peerId   对方用户 ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 消息分页
     */
    Page<PrivateMessageInfo> listMessages(Integer schoolId, Integer peerId, Integer pageNum, Integer pageSize);

    /**
     * 发送私信（非好友时校验陌生私信配额）
     *
     * @param schoolId   学校 ID
     * @param receiverId 接收人 ID
     * @param content    消息内容
     * @return 发送后的消息
     */
    PrivateMessageInfo send(Integer schoolId, Integer receiverId, String content);

    /**
     * 将某会话中对方发给我的消息标记为已读
     *
     * @param schoolId 学校 ID
     * @param peerId   对方用户 ID
     */
    void markConversationRead(Integer schoolId, Integer peerId);

    /**
     * 我的各学校未读私信数量
     *
     * @return 学校 ID 到未读数
     */
    List<UnreadCount> countUnreadBySchool();

    /**
     * 某学校下的未读数
     */
    record UnreadCount(Integer schoolId, long count) {
    }
}
