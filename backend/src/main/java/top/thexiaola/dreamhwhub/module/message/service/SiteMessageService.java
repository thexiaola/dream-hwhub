package top.thexiaola.dreamhwhub.module.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.thexiaola.dreamhwhub.module.message.dto.SiteMessageQueryRequest;
import top.thexiaola.dreamhwhub.module.message.vo.SiteMessageResponse;

import java.util.List;

/**
 * 站内信服务接口
 */
public interface SiteMessageService {

    /**
     * 分页查询当前用户的站内信（按学校隔离，支持类型/已读/关键字筛选）
     *
     * @param request 查询请求
     * @return 站内信分页结果
     */
    Page<SiteMessageResponse> listMyMessages(SiteMessageQueryRequest request);

    /**
     * 当前用户各学校的未读站内信数量
     *
     * @return 学校 ID 到未读数（仅包含有未读的学校）
     */
    List<UnreadCount> countUnreadBySchool();

    /**
     * 将某条站内信标记为已读（仅限本人消息）
     *
     * @param messageId 消息 ID
     */
    void markAsRead(Integer messageId);

    /**
     * 将当前用户在某学校下的全部站内信标记为已读
     *
     * @param schoolId 学校 ID，为空时标记全部学校的消息
     */
    void markAllAsRead(Integer schoolId);

    /**
     * 班级布置了新作业时，向该班全部学生（不含发布者本人）发站内信。
     * 由作业发布流程调用；按班级所属学校隔离。
     *
     * @param classId    班级 ID
     * @param className  班级名称
     * @param workId     作业 ID
     * @param workTitle  作业标题
     * @param publisherId 发布者用户 ID（不给自己发）
     */
    void notifyWorkPublished(Integer classId, String className, Integer workId,
            String workTitle, Integer publisherId);

    /**
     * 某学校下的未读数
     */
    record UnreadCount(Integer schoolId, long count) {
    }
}
