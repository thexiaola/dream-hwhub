package top.thexiaola.dreamhwhub.module.work_management.entity;

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
 * 考试违规记录实体
 * <p>
 * 每次反作弊触发（退出全屏、切屏/失焦、复制粘贴等）记录一条，供老师审计。
 */
@Data
@TableName("exam_violation")
public class ExamViolation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 考试会话 ID */
    @TableField("session_id")
    private Integer sessionId;

    /** 考试 ID */
    @TableField("work_id")
    private Integer workId;

    /** 学生 ID */
    @TableField("student_id")
    private Integer studentId;

    /** 违规类型：fullscreen_exit / visibility_hidden / window_blur / copy / cut / paste */
    @TableField("type")
    private String type;

    /** 补充说明（可选） */
    @TableField("detail")
    private String detail;

    @TableField(value = "occur_time", fill = FieldFill.INSERT)
    private LocalDateTime occurTime;
}
