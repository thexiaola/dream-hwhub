package top.thexiaola.dreamhwhub.support.mapper;

import org.mapstruct.Mapper;
import top.thexiaola.dreamhwhub.module.login.dto.UserResponse;
import top.thexiaola.dreamhwhub.module.login.entity.User;

/**
 * 用户对象映射器
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    
    /**
     * User 实体转换为 UserResponse DTO
     * @param user 用户实体
     * @return 用户响应 DTO
     */
    UserResponse toUserResponse(User user);
}
