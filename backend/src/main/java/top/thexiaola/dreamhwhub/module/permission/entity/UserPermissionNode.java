package top.thexiaola.dreamhwhub.module.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户直接权限节点实体类
 * 对应数据库表：user_permission_node
 */
@Data
@TableName("user_permission_node")
public class UserPermissionNode {

    // 关联ID
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 用户ID
    @TableField("user_id")
    private Integer userId;

    // 权限节点，如 user:add
    @TableField("node")
    private String node;

    // 创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
