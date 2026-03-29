package top.thexiaola.dreamhwhub.module.work_management.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作业实体类
 */
@Data
@TableName("work")
public class Work implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 作业 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 作业标题
     */
    @TableField("title")
    private String title;

    /**
     * 作业描述
     */
    @TableField("description")
    private String description;

    /**
     * 发布教师工号
     */
    @TableField("teacher_no")
    private String teacherNo;

    /**
     * 发布教师姓名
     */
    @TableField("teacher_name")
    private String teacherName;

    /**
     * 截止时间
     */
    @TableField("deadline")
    private LocalDateTime deadline;

    /**
     * 作业总分
     */
    @TableField("total_score")
    private Integer totalScore = 100;

    /**
     * 作业状态：0-未发布，1-已发布，2-已结束
     */
    @TableField("status")
    private Integer status = 1;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
