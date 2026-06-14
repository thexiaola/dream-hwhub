package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级信息实体类
 */
@Data
@TableName("class_info")
public class ClassInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_name")
    private String className;

    @TableField("description")
    private String description;

    @TableField("owner_id")
    private Integer ownerId;

    @TableField("invite_code")
    private String inviteCode;  // 班级邀请码（25位随机码）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
