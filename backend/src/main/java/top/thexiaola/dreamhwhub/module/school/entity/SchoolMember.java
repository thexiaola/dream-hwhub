package top.thexiaola.dreamhwhub.module.school.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校成员实体类
 * 姓名与学工号属于学校内身份，用户加入学校时填写，同一学校内学工号唯一
 */
@Data
@TableName("school_member")
public class SchoolMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("school_id")
    private Integer schoolId;

    @TableField("user_id")
    private Integer userId;

    @TableField("role")
    private Integer role;  // 2-学校管理员，1-老师，0-学生

    @TableField("staff_no")
    private String staffNo;  // 学工号，同一学校内唯一

    @TableField("real_name")
    private String realName;  // 姓名

    @TableField("join_time")
    private LocalDateTime joinTime;
}
