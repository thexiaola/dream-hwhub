package top.thexiaola.dreamhwhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.thexiaola.dreamhwhub.domain.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}