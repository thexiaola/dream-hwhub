package top.thexiaola.dreamhwhub.module.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信实体类
 * <p>
 * 系统事件（如班级布置了新作业）产生的个人通知。
 * 按学校隔离：同一用户在不同学校看到各自的站内信。
 */
@Data
@TableName("site_message")
public class SiteMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("recipient_id")
    private Integer recipientId;  // 接收人 ID

    @TableField("school_id")
    private Integer schoolId;  // 所属学校 ID（按学校隔离）

    @TableField("type")
    private String type;  // 消息类型，如 work_published

    @TableField("title")
    private String title;  // 消息标题

    @TableField("content")
    private String content;  // 消息内容

    @TableField("class_id")
    private Integer classId;  // 关联班级 ID

    @TableField("class_name")
    private String className;  // 关联班级名称（冗余，便于展示）

    @TableField("work_id")
    private Integer workId;  // 关联作业 ID

    @TableField("is_read")
    private Boolean isRead;  // 是否已读

    @TableField("read_time")
    private LocalDateTime readTime;  // 阅读时间

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 创建时间
}
