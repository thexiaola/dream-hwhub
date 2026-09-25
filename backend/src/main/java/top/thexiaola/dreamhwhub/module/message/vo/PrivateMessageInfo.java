package top.thexiaola.dreamhwhub.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私信消息 VO（某会话内的单条消息）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrivateMessageInfo {

    /**
     * 消息 ID
     */
    private Integer id;

    /**
     * 所属学校 ID
     */
    private Integer schoolId;

    /**
     * 发送人 ID
     */
    private Integer senderId;

    /**
     * 接收人 ID
     */
    private Integer receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 是否已读（对我而言）
     */
    private Boolean isRead;

    /**
     * 发送时间
     */
    private LocalDateTime createTime;

    /**
     * 是否为我自己发送的（前端据此决定气泡方向）
     */
    private Boolean mine;
}
