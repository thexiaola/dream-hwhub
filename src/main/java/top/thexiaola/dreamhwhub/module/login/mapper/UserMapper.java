package top.thexiaola.dreamhwhub.module.login.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.thexiaola.dreamhwhub.module.login.domain.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}