package top.thexiaola.dreamhwhub.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 陌生私信配额实体类
 * <p>
 * 记录「发送人 → 陌生接收人」在某学校内的已用条数与窗口起点。
 * 到达重置时间（窗口起点 + resetHours）后计数归零，可继续发送。
 */
@Data
@TableName("stranger_message_quota")
public class StrangerMessageQuota {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("school_id")
    private Integer schoolId;  // 所属学校 ID

    @TableField("sender_id")
    private Integer senderId;  // 发送人 ID

    @TableField("receiver_id")
    private Integer receiverId;  // 陌生接收人 ID

    @TableField("used_count")
    private Integer usedCount;  // 当前窗口内已发送条数

    @TableField("window_start")
    private LocalDateTime windowStart;  // 当前窗口起点

    @TableField("update_time")
    private LocalDateTime updateTime;  // 更新时间
}
