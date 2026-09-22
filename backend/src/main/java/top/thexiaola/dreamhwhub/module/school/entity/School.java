package top.thexiaola.dreamhwhub.module.school.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
