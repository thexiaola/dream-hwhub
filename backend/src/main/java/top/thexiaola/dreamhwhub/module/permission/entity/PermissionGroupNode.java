package top.thexiaola.dreamhwhub.module.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限组-权限节点关联实体类
 * 对应数据库表：permission_group_node
 */
@Data
@TableName("permission_group_node")
public class PermissionGroupNode {

    // 关联ID
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 权限组ID
    @TableField("group_id")
    private Integer groupId;

    // 权限节点，如 user:add
    @TableField("node")
    private String node;

    // 创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
