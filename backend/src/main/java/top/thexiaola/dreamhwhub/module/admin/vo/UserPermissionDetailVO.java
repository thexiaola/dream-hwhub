package top.thexiaola.dreamhwhub.module.admin.vo;

import lombok.Data;
import top.thexiaola.dreamhwhub.module.permission.vo.PermissionGroupVO;

import java.util.List;
import java.util.Set;

/**
 * 用户权限明细 VO
 */
@Data
public class UserPermissionDetailVO {

    // 用户编号
    private Integer userId;

    // 用户名
    private String username;

    // 是否平台管理员（OP）
    private Boolean isOp;

    // 所属权限组
    private List<PermissionGroupVO> groups;

    // 被直接授予的权限节点
    private Set<String> directNodes;

    // 最终生效的权限节点（OP 为全部节点）
    private Set<String> permissions;
}
