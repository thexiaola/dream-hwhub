package top.thexiaola.dreamhwhub.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.thexiaola.dreamhwhub.module.message.entity.PrivateMessage;

import java.util.List;
import java.util.Map;

/**
 * 私信 Mapper 接口
 */
@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    /**
     * 会话聚合：按「对方用户」分组，取每组最近一条消息 ID 与我的未读数。
     * 分组与计数全部在数据库完成，避免取回全部消息再在内存里聚合。
     *
     * @param schoolId 学校 ID
     * @param me       当前用户 ID
     * @return 每行含 peer_id / last_id / unread
     */
    @Select("SELECT IF(sender_id = #{me}, receiver_id, sender_id) AS peer_id, "
            + "MAX(id) AS last_id, "
            + "SUM(CASE WHEN receiver_id = #{me} AND is_read = 0 THEN 1 ELSE 0 END) AS unread "
            + "FROM private_message "
            + "WHERE school_id = #{schoolId} AND (sender_id = #{me} OR receiver_id = #{me}) "
            + "GROUP BY peer_id "
            + "ORDER BY last_id DESC")
    List<Map<String, Object>> selectConversationRows(@Param("schoolId") Integer schoolId,
            @Param("me") Integer me);

    /**
     * 我的各学校未读私信数量（数据库分组统计）
     *
     * @param me 当前用户 ID
     * @return 每行含 school_id / cnt
     */
    @Select("SELECT school_id, COUNT(*) AS cnt FROM private_message "
            + "WHERE receiver_id = #{me} AND is_read = 0 "
            + "GROUP BY school_id")
    List<Map<String, Object>> countUnreadBySchool(@Param("me") Integer me);
}
