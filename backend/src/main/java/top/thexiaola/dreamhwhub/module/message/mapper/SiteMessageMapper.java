package top.thexiaola.dreamhwhub.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.thexiaola.dreamhwhub.module.message.entity.SiteMessage;

import java.util.List;

/**
 * 站内信 Mapper 接口
 */
@Mapper
public interface SiteMessageMapper extends BaseMapper<SiteMessage> {

    /**
     * 批量插入站内信（单条 INSERT 多值，避免逐条插入）
     *
     * @param messages 待插入的消息列表
     * @return 影响行数
     */
    @Insert("<script>" +
            "INSERT INTO site_message " +
            "(recipient_id, school_id, type, title, content, class_id, class_name, work_id, is_read, create_time) VALUES " +
            "<foreach collection='messages' item='m' separator=','>" +
            "(#{m.recipientId}, #{m.schoolId}, #{m.type}, #{m.title}, #{m.content}, " +
            "#{m.classId}, #{m.className}, #{m.workId}, #{m.isRead}, #{m.createTime})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("messages") List<SiteMessage> messages);
}
