package top.thexiaola.dreamhwhub.support.security.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户级敏感操作验证设置实体
 * <p>
 * 仅存储用户**显式关闭**二次验证的操作（key 见 {@code SensitiveOperations}）。
 * 采用「只存关闭项」的模型：默认所有可用操作都要求二次验证；用户关闭某操作即插入一行，
 * 重新开启即删除该行。用户失去某操作权限时删除其行，即自动「重置为默认（启用验证）」。
 * 对应数据库表：user_sensitive_operation
 */
@Data
@TableName("user_sensitive_operation")
public class UserSensitiveOperation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 记录编号
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 用户编号
    @TableField("user_id")
    private Integer userId;

    // 已关闭二次验证的操作标识（如 class.dissolve）
    @TableField("operation_key")
    private String operationKey;

    // 创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
