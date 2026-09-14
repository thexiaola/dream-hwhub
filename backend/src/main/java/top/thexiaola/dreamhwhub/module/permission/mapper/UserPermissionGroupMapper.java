package top.thexiaola.dreamhwhub.module.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.thexiaola.dreamhwhub.module.permission.entity.UserPermissionGroup;

/**
 * 用户-权限组关联 Mapper 接口
 */
@Mapper
public interface UserPermissionGroupMapper extends BaseMapper<UserPermissionGroup> {
}
