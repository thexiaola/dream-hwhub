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

    @TableField("creator_id")
    private Integer creatorId;

    @TableField("class_code")
    private String classCode;

    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
