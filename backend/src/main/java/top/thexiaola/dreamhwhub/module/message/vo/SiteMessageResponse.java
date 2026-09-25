package top.thexiaola.dreamhwhub.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内信响应 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SiteMessageResponse {

    /**
     * 消息 ID
     */
    private Integer id;

    /**
     * 所属学校 ID
     */
    private Integer schoolId;

    /**
     * 所属学校名称
     */
    private String schoolName;

    /**
     * 消息类型，如 work_published
     */
    private String type;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 关联班级 ID
     */
    private Integer classId;

    /**
     * 关联班级名称
     */
    private String className;

    /**
     * 关联作业 ID
     */
    private Integer workId;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
