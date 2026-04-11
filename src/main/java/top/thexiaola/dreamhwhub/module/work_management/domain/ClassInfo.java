package top.thexiaola.dreamhwhub.module.work_management.domain;

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
    private String inviteCode;  // 班级邀请码（6位随机码）

    @TableField("approval_status")
    private Integer approvalStatus;  // 0-待审核，1-已通过，2-已拒绝

    @TableField("admin_remark")
    private String adminRemark;  // 管理员审核备注

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
