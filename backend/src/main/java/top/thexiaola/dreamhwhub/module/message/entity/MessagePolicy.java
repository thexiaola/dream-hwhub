package top.thexiaola.dreamhwhub.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 全站私信策略实体类（单行，id 恒为 1）
 * <p>
 * 平台管理员可修改全站默认：向陌生用户发送私信的条数上限与重置小时数。
 * 学校可在 school 表上覆盖这两项（为空表示继承此处）。
 */
@Data
@TableName("message_policy")
public class MessagePolicy {

    /** 全站策略固定主键 */
    public static final int GLOBAL_ID = 1;

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    @TableField("stranger_limit")
    private Integer strangerLimit;  // 向陌生用户发送私信的条数上限

    @TableField("reset_hours")
    private Integer resetHours;  // 配额重置小时数

    @TableField("update_time")
    private LocalDateTime updateTime;  // 更新时间
}
