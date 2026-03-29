package top.thexiaola.dreamhwhub.module.work_management.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级成员实体类
 */
@Data
@TableName("class_member")
public class ClassMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("class_id")
    private Integer classId;

    @TableField("user_id")
    private Integer userId;

    @TableField("role")
    private Boolean isTeacher;  // true=老师，false=学生

    @TableField("join_time")
    private LocalDateTime joinTime;

    @TableField("invite_by")
    private Integer inviteBy;
}
