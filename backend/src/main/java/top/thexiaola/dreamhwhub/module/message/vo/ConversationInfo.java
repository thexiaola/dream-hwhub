package top.thexiaola.dreamhwhub.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私信会话摘要 VO（会话列表项：按对方用户聚合，取最近一条）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationInfo {

    /**
     * 所属学校 ID
     */
    private Integer schoolId;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 对方用户 ID
     */
    private Integer peerId;

    /**
     * 对方用户名
     */
    private String peerUsername;

    /**
     * 对方头像
     */
    private String peerAvatar;

    /**
     * 对方在该校的姓名
     */
    private String peerRealName;

    /**
     * 对方在该校的学工号
     */
    private String peerStaffNo;

    /**
     * 对方在该校的角色名称
     */
    private String peerRole;

    /**
     * 是否好友
     */
    private Boolean friend;

    /**
     * 最近一条消息内容
     */
    private String lastContent;

    /**
     * 最近一条消息时间
     */
    private LocalDateTime lastTime;

    /**
     * 我未读的条数
     */
    private Long unreadCount;
}
