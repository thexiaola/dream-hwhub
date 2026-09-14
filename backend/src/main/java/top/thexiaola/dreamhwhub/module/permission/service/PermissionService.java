package top.thexiaola.dreamhwhub.module.permission.service;

import top.thexiaola.dreamhwhub.module.permission.entity.PermissionGroup;
import top.thexiaola.dreamhwhub.module.permission.vo.PermissionGroupVO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 权限服务接口
 * <p>
 * 提供 LuckPerms 风格的两级授权模型：
 * 用户 → 权限组 → 权限节点，并支持直接给用户授予权限节点；
 * 平台管理员（OP）拥有全部权限节点。
 */
public interface PermissionService {

    /**
     * 判断用户是否为平台管理员（OP）
     *
     * @param userId 用户 ID
     * @return true-是平台管理员
     */
    boolean isOp(Integer userId);

    /**
     * 设置或取消用户的平台管理员（OP）身份
     *
     * @param userId 用户 ID
     * @param isOp   true-授予，false-取消
     */
    void setOp(Integer userId, boolean isOp);

    /**
     * 计算用户拥有的全部权限节点（OP 返回全部已注册节点）
     *
     * @param userId 用户 ID
     * @return 权限节点集合
     */
    Set<String> getPermissionNodes(Integer userId);

    /**
     * 判断用户是否拥有指定权限节点
     *
     * @param userId 用户 ID
     * @param node   权限节点
     * @return true-拥有
     */
    boolean hasPermission(Integer userId, String node);

    /**
     * 判断用户是否拥有其中任意一个权限节点
     *
     * @param userId 用户 ID
     * @param nodes  权限节点集合
     * @return true-拥有至少一个
     */
    boolean hasAnyPermission(Integer userId, Collection<String> nodes);

    /**
     * 查询全部权限组
     *
     * @return 权限组列表
     */
    List<PermissionGroup> listGroups();

    /**
     * 查询全部权限组（含所含节点与组内用户数，供管理端直接返回）
     *
     * @return 权限组 VO 列表
     */
    List<PermissionGroupVO> listGroupVOs();

    /**
     * 查询用户所属的权限组（含所含节点）
     *
     * @param userId 用户 ID
     * @return 权限组 VO 列表
     */
    List<PermissionGroupVO> getUserGroupVOs(Integer userId);

    /**
     * 查询权限组
     *
     * @param groupId 权限组 ID
     * @return 权限组，不存在返回 null
     */
    PermissionGroup getGroup(Integer groupId);

    /**
     * 查询权限组 VO（含所含节点与组内用户数）
     *
     * @param groupId 权限组 ID
     * @return 权限组 VO，不存在返回 null
     */
    PermissionGroupVO getGroupVO(Integer groupId);

    /**
     * 创建权限组
     *
     * @param code        权限组标识（唯一）
     * @param name        权限组名称
     * @param description 权限组描述
     * @param isDefault   是否为新用户默认加入
     * @return 创建后的权限组
     */
    PermissionGroup createGroup(String code, String name, String description, Boolean isDefault);

    /**
     * 更新权限组（标识与所含节点不在此处修改）
     *
     * @param groupId     权限组 ID
     * @param name        权限组名称
     * @param description 权限组描述
     * @param isDefault   是否为新用户默认加入
     * @return 更新后的权限组
     */
    PermissionGroup updateGroup(Integer groupId, String name, String description, Boolean isDefault);

    /**
     * 删除权限组及其节点绑定、用户绑定
     *
     * @param groupId 权限组 ID
     */
    void deleteGroup(Integer groupId);

    /**
     * 查询权限组拥有的权限节点
     *
     * @param groupId 权限组 ID
     * @return 权限节点集合
     */
    Set<String> getGroupNodes(Integer groupId);

    /**
     * 覆盖设置权限组拥有的权限节点
     *
     * @param groupId 权限组 ID
     * @param nodes   权限节点集合（未注册的节点会被拒绝）
     */
    void setGroupNodes(Integer groupId, Collection<String> nodes);

    /**
     * 查询用户所属的权限组 ID 列表
     *
     * @param userId 用户 ID
     * @return 权限组 ID 列表
     */
    List<Integer> getUserGroupIds(Integer userId);

    /**
     * 查询用户所属的权限组
     *
     * @param userId 用户 ID
     * @return 权限组列表
     */
    List<PermissionGroup> getUserGroups(Integer userId);

    /**
     * 覆盖设置用户所属的权限组
     *
     * @param userId   用户 ID
     * @param groupIds 权限组 ID 集合
     */
    void setUserGroups(Integer userId, Collection<Integer> groupIds);

    /**
     * 查询用户被直接授予的权限节点
     *
     * @param userId 用户 ID
     * @return 权限节点集合
     */
    Set<String> getUserDirectNodes(Integer userId);

    /**
     * 覆盖设置用户被直接授予的权限节点
     *
     * @param userId 用户 ID
     * @param nodes  权限节点集合（未注册的节点会被拒绝）
     */
    void setUserDirectNodes(Integer userId, Collection<String> nodes);

    /**
     * 将默认权限组应用到新注册用户
     *
     * @param userId 用户 ID
     */
    void applyDefaultGroups(Integer userId);

    /**
     * 部署引导：系统中还没有其他用户时，把首个注册用户提升为平台管理员（OP）
     *
     * @param userId 用户 ID
     * @return true-已提升为 OP
     */
    boolean promoteToOpIfFirstUser(Integer userId);
}
