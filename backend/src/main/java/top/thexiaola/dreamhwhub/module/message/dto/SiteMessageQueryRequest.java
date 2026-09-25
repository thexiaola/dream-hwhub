package top.thexiaola.dreamhwhub.module.message.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 站内信分页查询请求
 * <p>
 * 筛选条件全部下推到数据库执行：学校、类型、已读状态、关键字。
 */
@Data
public class SiteMessageQueryRequest {

    /**
     * 页码（从 1 开始）
     */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 10;

    /**
     * 学校 ID（按学校隔离），为空时返回全部学校的站内信
     */
    private Integer schoolId;

    /**
     * 消息类型筛选，如 work_published；为空时不限
     */
    private String type;

    /**
     * 已读状态筛选：true-仅已读，false-仅未读，null-不限
     */
    private Boolean isRead;

    /**
     * 关键字（匹配标题 / 内容 / 班级名），为空时不限
     */
    private String keyword;
}
