package top.thexiaola.dreamhwhub.module.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信实体类
 * <p>
 * 私信按学校隔离：会话双方须在同一学校。非好友也可互发（受陌生私信配额约束）。
 */
@Data
@TableName("private_message")
public class PrivateMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("school_id")
    private Integer schoolId;  // 所属学校 ID（私信按学校隔离）

    @TableField("sender_id")
    private Integer senderId;  // 发送人 ID

    @TableField("receiver_id")
    private Integer receiverId;  // 接收人 ID

    @TableField("content")
    private String content;  // 消息内容

    @TableField("is_read")
    private Boolean isRead;  // 是否已读

    @TableField("read_time")
    private LocalDateTime readTime;  // 阅读时间

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 发送时间
}
