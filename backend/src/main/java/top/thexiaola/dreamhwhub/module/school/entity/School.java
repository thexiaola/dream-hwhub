package top.thexiaola.dreamhwhub.module.school.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校实体类
 * 学校由平台管理员（OP）创建，学校管理员由平台管理员指派
 */
@Data
@TableName("school")
public class School {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("school_name")
    private String schoolName;

    @TableField("description")
    private String description;

    @TableField("allow_join_without_approval")
    private Boolean allowJoinWithoutApproval;  // 加入学校是否免审核：true-填入学工号与姓名后直接加入

    /**
     * 班级接管是否自动同意：
     * true-其他老师申请接管「原老师已失去教师身份」的班级时自动通过；
     * false-需由学校管理员审核（默认 true，即系统默认自动同意）
     */
    @TableField("auto_approve_class_takeover")
    private Boolean autoApproveClassTakeover;

    /**
     * 本校向陌生用户发送私信的条数上限；null 表示继承全站默认
     */
    @TableField("stranger_message_limit")
    private Integer strangerMessageLimit;

    /**
     * 本校陌生私信配额重置小时数；null 表示继承全站默认
     */
    @TableField("stranger_message_reset_hours")
    private Integer strangerMessageResetHours;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
