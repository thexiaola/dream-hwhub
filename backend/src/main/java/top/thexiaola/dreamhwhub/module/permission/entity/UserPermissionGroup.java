package top.thexiaola.dreamhwhub.module.permission.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-权限组关联实体类
 * 对应数据库表：user_permission_group
 */
@Data
@TableName("user_permission_group")
public class UserPermissionGroup {

    // 关联ID
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 用户ID
    @TableField("user_id")
    private Integer userId;

    // 权限组ID
    @TableField("group_id")
    private Integer groupId;

    // 创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
