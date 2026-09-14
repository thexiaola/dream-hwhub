package top.thexiaola.dreamhwhub.module.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限组实体类
 * 对应数据库表：permission_group
 */
@Data
@TableName("permission_group")
public class PermissionGroup {

    // 权限组ID
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 权限组标识（唯一）
    @TableField("code")
    private String code;

    // 权限组名称
    @TableField("name")
    private String name;

    // 权限组描述
    @TableField("description")
    private String description;

    // 是否为新用户默认加入：0-否，1-是
    @TableField("is_default")
    private Boolean isDefault = false;

    // 创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 更新时间
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
