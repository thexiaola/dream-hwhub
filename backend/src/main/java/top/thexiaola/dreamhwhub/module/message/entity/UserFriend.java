package top.thexiaola.dreamhwhub.module.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友关系实体类
 * <p>
 * 好友按学校隔离：同一用户在不同学校拥有各自的好友列表。
 * 建立好友需先发起申请（status=0），对方同意后成为好友（status=1）。
 */
@Data
@TableName("user_friend")
public class UserFriend {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("school_id")
    private Integer schoolId;  // 所属学校 ID（好友按学校隔离）

    @TableField("requester_id")
    private Integer requesterId;  // 发起人 ID

    @TableField("addressee_id")
    private Integer addresseeId;  // 接收人 ID

    @TableField("status")
    private Integer status;  // 0-待确认，1-已接受，2-已拒绝

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;  // 发起时间

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;  // 更新时间
}
