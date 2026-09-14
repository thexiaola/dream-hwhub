package top.thexiaola.dreamhwhub.module.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.thexiaola.dreamhwhub.module.permission.entity.PermissionGroupNode;

/**
 * 权限组-权限节点关联 Mapper 接口
 */
@Mapper
public interface PermissionGroupNodeMapper extends BaseMapper<PermissionGroupNode> {
}
