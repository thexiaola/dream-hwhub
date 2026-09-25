package top.thexiaola.dreamhwhub.module.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.thexiaola.dreamhwhub.module.message.entity.UserFriend;

/**
 * 好友关系 Mapper 接口
 */
@Mapper
public interface UserFriendMapper extends BaseMapper<UserFriend> {
}
