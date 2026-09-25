package top.thexiaola.dreamhwhub.module.work_management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkResponse;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业实体类
 */
@Data
@TableName("work_info")
public class WorkInfo implements Serializable {

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
     * 发布人 ID
     */
    @TableField("publisher_id")
    private Integer publisherId;

    /**
     * 所属班级 ID
     */
    @TableField("class_id")
    private Integer classId;

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
     * 是否允许逾期提交：true-允许，false-不允许
     */
    @TableField("allow_late_submit")
    private Boolean allowLateSubmit = true;

    /**
     * 是否置顶：true-置顶，false-不置顶
     */
    @TableField("is_pinned")
    private Boolean isPinned = false;

    /**
     * 是否含结构化题目：true-含题目（学生逐题作答），false-仅文本作业
     */
    @TableField("has_questions")
    private Boolean hasQuestions = false;

    /**
     * 类型：homework-作业，exam-考试
     */
    @TableField("work_type")
    private String workType = "homework";

    /**
     * 考试时长（分钟）；从学生开考时刻计时，为空则仅以截止时间为准
     */
    @TableField("exam_duration_minutes")
    private Integer examDurationMinutes;

    /**
     * 是否开启反作弊
     */
    @TableField("anti_cheat_enabled")
    private Boolean antiCheatEnabled = false;

    /**
     * 反作弊-字体映射：题干文字以打乱字体渲染，复制得到乱码
     */
    @TableField("anti_cheat_font")
    private Boolean antiCheatFont = false;

    /**
     * 反作弊-强制全屏：离开全屏即记违规
     */
    @TableField("anti_cheat_fullscreen")
    private Boolean antiCheatFullscreen = false;

    /**
     * 反作弊-禁止复制粘贴
     */
    @TableField("anti_cheat_no_copy")
    private Boolean antiCheatNoCopy = false;

    /**
     * 反作弊-切屏/失焦检测
     */
    @TableField("anti_cheat_detect_leave")
    private Boolean antiCheatDetectLeave = false;

    /**
     * 违规次数上限：达到后自动交卷；为空表示不限制
     */
    @TableField("anti_cheat_max_violations")
    private Integer antiCheatMaxViolations;

    /**
     * 题目乱序：true-为学生随机打乱题序
     */
    @TableField("shuffle_questions")
    private Boolean shuffleQuestions = false;

    /**
     * 发布时间
     */
    @TableField("publish_time")
    private LocalDateTime publishTime;

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

    /**
     * 附件列表（非数据库字段，仅用于接口返回）
     */
    @TableField(exist = false)
    private List<WorkResponse.AttachmentInfo> attachments;
}
