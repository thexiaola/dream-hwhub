package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 进入考试响应 VO
 * <p>
 * 返回考试作答所需的一切：题目（按需乱序、按需字体映射打乱后的文本）、
 * 限时与剩余时间、反作弊配置，以及用于加载打乱字体的种子。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamEnterResponse {

    private Integer workId;
    private String title;
    private String description;
    private Integer totalScore;
    private Integer status;

    /** 考试时长（分钟）；为空表示不限时 */
    private Integer durationMinutes;
    /** 开考时刻 */
    private LocalDateTime startTime;
    /** 限时结束时刻（startTime + durationMinutes）；为空表示不限时 */
    private LocalDateTime endTime;
    /** 剩余秒数；为空表示不限时 */
    private Long remainingSeconds;
    /** 是否已交卷 */
    private Boolean submitted;

    /** 反作弊是否开启 */
    private Boolean antiCheatEnabled;
    /** 反作弊-字体映射 */
    private Boolean antiCheatFont;
    /** 反作弊-强制全屏 */
    private Boolean antiCheatFullscreen;
    /** 反作弊-禁止复制粘贴 */
    private Boolean antiCheatNoCopy;
    /** 反作弊-切屏/失焦检测 */
    private Boolean antiCheatDetectLeave;
    /** 违规次数上限 */
    private Integer antiCheatMaxViolations;
    /** 已累计违规次数 */
    private Integer violationCount;

    /** 字体映射种子；非空时前端应加载 /api/exams/{workId}/font/{seed}.woff2 并按需加字体样式 */
    private Integer fontSeed;

    /** 题目列表（学生侧，不含参考答案；文本已按需打乱） */
    private List<WorkQuestionStudentVO> questions;

    /** 已保存的答题草稿（JSON 字符串，未保存为空） */
    private String draftAnswers;
}
