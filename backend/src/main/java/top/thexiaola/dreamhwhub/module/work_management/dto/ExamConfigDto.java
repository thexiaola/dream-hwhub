package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 考试反作弊与限时设置（创建/更新考试时随作业请求一起提交）
 */
@Data
public class ExamConfigDto {

    /**
     * 考试时长（分钟）；为空则仅以截止时间为准
     */
    @Min(value = 1, message = "考试时长至少 1 分钟")
    @Max(value = 600, message = "考试时长不能超过 600 分钟")
    private Integer durationMinutes;

    /**
     * 是否开启反作弊（关闭时其余反作弊项一律不生效）
     */
    private Boolean enabled = false;

    /** 反作弊-字体映射：题干以打乱字体渲染，复制得到乱码 */
    private Boolean fontScramble = false;

    /** 反作弊-强制全屏：离开全屏即记违规 */
    private Boolean forceFullscreen = false;

    /** 反作弊-禁止复制粘贴 */
    private Boolean noCopy = false;

    /** 反作弊-切屏/失焦检测 */
    private Boolean detectLeave = false;

    /**
     * 违规次数上限：达到后自动交卷；为空表示不限制
     */
    @Min(value = 1, message = "违规上限至少 1 次")
    @Max(value = 99, message = "违规上限不能超过 99 次")
    private Integer maxViolations;

    /** 题目乱序：为学生随机打乱题序 */
    private Boolean shuffleQuestions = false;
}
