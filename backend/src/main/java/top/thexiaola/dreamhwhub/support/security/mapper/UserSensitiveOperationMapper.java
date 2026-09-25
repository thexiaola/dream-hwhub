package top.thexiaola.dreamhwhub.support.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.thexiaola.dreamhwhub.support.security.entity.UserSensitiveOperation;

/**
 * 用户级敏感操作验证设置 Mapper 接口
 */
@Mapper
public interface UserSensitiveOperationMapper extends BaseMapper<UserSensitiveOperation> {
}
