package top.thexiaola.dreamhwhub.module.work_management.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作业响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkResponse {

    /**
     * 作业 ID
     */
    private Integer id;

    /**
     * 作业标题
     */
    private String title;

    /**
     * 作业描述
     */
    private String description;

    /**
     * 发布人 ID
     */
    private Integer publisherId;

    /**
     * 发布人用户名（发布人不是班级成员时用于标识，如管理员）
     */
    private String publisherName;

    /**
     * 发布人在班级内的姓名（发布人不是班级成员时为空）
     */
    private String publisherStudentName;

    /**
     * 所属班级 ID
     */
    private Integer classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 作业总分
     */
    private Integer totalScore;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 作业状态：0-未发布，1-已发布，2-已结束（动态计算）
     */
    private Integer status;

    /**
     * 是否已逾期
     */
    private Boolean isOverdue;

    /**
     * 是否置顶
     */
    private Boolean isPinned;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 附件列表
     */
    private List<AttachmentInfo> attachments;

    /**
     * 已交人数（去重提交人，不含软删除）
     */
    private Integer submittedCount;

    /**
     * 是否含结构化题目
     */
    private Boolean hasQuestions;

    /**
     * 类型：homework-作业，exam-考试
     */
    private String workType;

    /**
     * 考试时长（分钟）
     */
    private Integer examDurationMinutes;

    /**
     * 是否开启反作弊
     */
    private Boolean antiCheatEnabled;

    /**
     * 反作弊-字体映射
     */
    private Boolean antiCheatFont;

    /**
     * 反作弊-强制全屏
     */
    private Boolean antiCheatFullscreen;

    /**
     * 反作弊-禁止复制粘贴
     */
    private Boolean antiCheatNoCopy;

    /**
     * 反作弊-切屏/失焦检测
     */
    private Boolean antiCheatDetectLeave;

    /**
     * 违规次数上限
     */
    private Integer antiCheatMaxViolations;

    /**
     * 题目乱序
     */
    private Boolean shuffleQuestions;

    /**
     * 题目列表（教师侧含参考答案；学生侧由学生接口单独获取，不在此返回）
     */
    private List<WorkQuestionVO> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private Integer id;
        private String fileName;
        private String filePath;
        private Long fileSize;
        private String fileType;
        private LocalDateTime uploadTime;
    }
}
