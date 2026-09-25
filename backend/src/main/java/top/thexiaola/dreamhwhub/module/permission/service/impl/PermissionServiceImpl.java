package top.thexiaola.dreamhwhub.module.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionRegistry;
import top.thexiaola.dreamhwhub.module.permission.entity.PermissionGroup;
import top.thexiaola.dreamhwhub.module.permission.entity.PermissionGroupNode;
import top.thexiaola.dreamhwhub.module.permission.entity.UserPermissionGroup;
import top.thexiaola.dreamhwhub.module.permission.entity.UserPermissionNode;
import top.thexiaola.dreamhwhub.module.permission.mapper.PermissionGroupMapper;
import top.thexiaola.dreamhwhub.module.permission.mapper.PermissionGroupNodeMapper;
import top.thexiaola.dreamhwhub.module.permission.mapper.UserPermissionGroupMapper;
import top.thexiaola.dreamhwhub.module.permission.mapper.UserPermissionNodeMapper;
import top.thexiaola.dreamhwhub.module.permission.service.PermissionService;
import top.thexiaola.dreamhwhub.module.permission.vo.PermissionGroupVO;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private static final java.util.regex.Pattern GROUP_CODE_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{1,63}$");

    private final PermissionGroupMapper permissionGroupMapper;
    private final PermissionGroupNodeMapper permissionGroupNodeMapper;
    private final UserPermissionGroupMapper userPermissionGroupMapper;
    private final UserPermissionNodeMapper userPermissionNodeMapper;
    private final UserMapper userMapper;

    @Override
    public boolean isOp(Integer userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectById(userId);
        return user != null && Boolean.TRUE.equals(user.getIsOp());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setOp(Integer userId, boolean isOp) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "用户ID无效", null);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }

        // 平台管理员身份只能由平台管理员授予或取消，且不能作用于自己
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "无法确认当前操作者身份", null);
        }
        if (currentUser.getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能修改自己的平台管理员身份", null);
        }
        if (!isOp(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    "只有平台管理员可以授予或取消平台管理员身份", null);
        }

        User update = new User();
        update.setId(userId);
        update.setIsOp(isOp);
        userMapper.updateById(update);
        log.info("User {} OP flag set to {}", userId, isOp);
    }

    @Override
    public Set<String> getPermissionNodes(Integer userId) {
        if (userId == null) {
            return Set.of();
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Set.of();
        }
        // 平台管理员（OP）拥有全部权限节点
        if (Boolean.TRUE.equals(user.getIsOp())) {
            return PermissionRegistry.allNodes();
        }

        Set<String> nodes = new LinkedHashSet<>();

        // 用户被直接授予的权限节点
        QueryWrapper<UserPermissionNode> directQuery = new QueryWrapper<>();
        directQuery.eq("user_id", userId);
        for (UserPermissionNode directNode : userPermissionNodeMapper.selectList(directQuery)) {
            nodes.add(directNode.getNode());
        }

        // 用户所属权限组携带的权限节点
        QueryWrapper<UserPermissionGroup> groupQuery = new QueryWrapper<>();
        groupQuery.eq("user_id", userId);
        List<Integer> groupIds = userPermissionGroupMapper.selectList(groupQuery).stream()
                .map(UserPermissionGroup::getGroupId)
                .toList();
        if (!groupIds.isEmpty()) {
            QueryWrapper<PermissionGroupNode> nodeQuery = new QueryWrapper<>();
            nodeQuery.in("group_id", groupIds);
            for (PermissionGroupNode groupNode : permissionGroupNodeMapper.selectList(nodeQuery)) {
                nodes.add(groupNode.getNode());
            }
        }

        return nodes;
    }

    @Override
    public boolean hasPermission(Integer userId, String node) {
        if (userId == null || node == null || node.isBlank()) {
            return false;
        }
        return getPermissionNodes(userId).contains(node);
    }

    @Override
    public boolean hasAnyPermission(Integer userId, Collection<String> nodes) {
        if (userId == null || nodes == null || nodes.isEmpty()) {
            return false;
        }
        Set<String> owned = getPermissionNodes(userId);
        return nodes.stream().anyMatch(owned::contains);
    }

    @Override
    public List<PermissionGroup> listGroups() {
        QueryWrapper<PermissionGroup> query = new QueryWrapper<>();
        query.orderByAsc("id");
        return permissionGroupMapper.selectList(query);
    }

    @Override
    public List<PermissionGroupVO> listGroupVOs() {
        return toGroupVOs(listGroups());
    }

    @Override
    public List<PermissionGroupVO> getUserGroupVOs(Integer userId) {
        return toGroupVOs(getUserGroups(userId));
    }

    /**
     * 批量把权限组实体转为 VO：一次查询取回所有组的节点、一次分组聚合取回各组用户数，
     * 避免「每个组各查一次节点 + 各数一次用户」的 N+1。
     */
    private List<PermissionGroupVO> toGroupVOs(List<PermissionGroup> groups) {
        if (groups.isEmpty()) {
            return List.of();
        }
        List<Integer> groupIds = groups.stream().map(PermissionGroup::getId).toList();

        // 一次取回所有组的节点，按组归并（仅取本批组的行，不额外多读）
        QueryWrapper<PermissionGroupNode> nodeQuery = new QueryWrapper<>();
        nodeQuery.in("group_id", groupIds);
        Map<Integer, Set<String>> nodesByGroup = new HashMap<>();
        for (PermissionGroupNode node : permissionGroupNodeMapper.selectList(nodeQuery)) {
            nodesByGroup.computeIfAbsent(node.getGroupId(), k -> new LinkedHashSet<>()).add(node.getNode());
        }

        // 各组用户数在数据库内聚合：GROUP BY group_id + COUNT(*)
        Map<Integer, Long> countByGroup = new HashMap<>();
        QueryWrapper<UserPermissionGroup> countQuery = new QueryWrapper<>();
        countQuery.in("group_id", groupIds)
                .select("group_id", "COUNT(*) AS cnt")
                .groupBy("group_id");
        for (Map<String, Object> row : userPermissionGroupMapper.selectMaps(countQuery)) {
            Object groupId = row.get("group_id");
            Object cnt = row.get("cnt");
            if (groupId instanceof Number gid && cnt instanceof Number count) {
                countByGroup.put(gid.intValue(), count.longValue());
            }
        }

        return groups.stream()
                .map(group -> toGroupVO(group,
                        nodesByGroup.getOrDefault(group.getId(), Set.of()),
                        countByGroup.getOrDefault(group.getId(), 0L)))
                .toList();
    }

    @Override
    public PermissionGroup getGroup(Integer groupId) {
        if (groupId == null) {
            return null;
        }
        return permissionGroupMapper.selectById(groupId);
    }

    @Override
    public PermissionGroupVO getGroupVO(Integer groupId) {
        PermissionGroup group = getGroup(groupId);
        if (group == null) {
            return null;
        }
        return toGroupVO(group, getGroupNodes(groupId), countGroupUsers(groupId));
    }

    /** 权限组实体 + 已取好的节点与用户数 → VO */
    private PermissionGroupVO toGroupVO(PermissionGroup group, Set<String> nodes, long userCount) {
        PermissionGroupVO vo = new PermissionGroupVO();
        vo.setId(group.getId());
        vo.setCode(group.getCode());
        vo.setName(group.getName());
        vo.setDescription(group.getDescription());
        vo.setIsDefault(Boolean.TRUE.equals(group.getIsDefault()));
        vo.setCreateTime(group.getCreateTime());
        vo.setNodes(nodes);
        vo.setUserCount(userCount);
        return vo;
    }

    /** 单个权限组的用户数（数据库内 COUNT） */
    private long countGroupUsers(Integer groupId) {
        QueryWrapper<UserPermissionGroup> countQuery = new QueryWrapper<>();
        countQuery.eq("group_id", groupId);
        return userPermissionGroupMapper.selectCount(countQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionGroup createGroup(String code, String name, String description, Boolean isDefault) {
        if (code == null || !GROUP_CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "权限组标识只能由字母、数字、下划线、连字符组成，且以字母开头（2-64位）", null);
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "权限组名称不能为空", null);
        }

        QueryWrapper<PermissionGroup> existsQuery = new QueryWrapper<>();
        existsQuery.eq("code", code);
        if (permissionGroupMapper.selectCount(existsQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "权限组标识已存在", null);
        }

        PermissionGroup group = new PermissionGroup();
        group.setCode(code);
        group.setName(name);
        group.setDescription(description);
        group.setIsDefault(Boolean.TRUE.equals(isDefault));
        permissionGroupMapper.insert(group);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionGroup updateGroup(Integer groupId, String name, String description, Boolean isDefault) {
        PermissionGroup group = requireGroup(groupId);
        if (name != null && !name.isBlank()) {
            group.setName(name);
        }
        group.setDescription(description);
        if (isDefault != null) {
            // 设为默认组会让后续所有新用户自动继承该组节点，因此需要校验其在操作者权限范围内
            if (Boolean.TRUE.equals(isDefault)) {
                ensureGrantable(getGroupNodes(groupId));
            }
            group.setIsDefault(isDefault);
        }
        permissionGroupMapper.updateById(group);
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Integer groupId) {
        requireGroup(groupId);

        QueryWrapper<PermissionGroupNode> nodeQuery = new QueryWrapper<>();
        nodeQuery.eq("group_id", groupId);
        permissionGroupNodeMapper.delete(nodeQuery);

        QueryWrapper<UserPermissionGroup> userQuery = new QueryWrapper<>();
        userQuery.eq("group_id", groupId);
        userPermissionGroupMapper.delete(userQuery);

        permissionGroupMapper.deleteById(groupId);
        log.info("Permission group {} deleted", groupId);
    }

    @Override
    public Set<String> getGroupNodes(Integer groupId) {
        if (groupId == null) {
            return Set.of();
        }
        QueryWrapper<PermissionGroupNode> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        Set<String> nodes = new LinkedHashSet<>();
        for (PermissionGroupNode node : permissionGroupNodeMapper.selectList(query)) {
            nodes.add(node.getNode());
        }
        return nodes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setGroupNodes(Integer groupId, Collection<String> nodes) {
        requireGroup(groupId);
        List<String> normalized = normalizeNodes(nodes);
        ensureGrantable(normalized);

        QueryWrapper<PermissionGroupNode> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("group_id", groupId);
        permissionGroupNodeMapper.delete(deleteQuery);

        // 批量插入，避免逐条 insert（N 次数据库往返）
        if (!normalized.isEmpty()) {
            List<PermissionGroupNode> entities = normalized.stream().map(node -> {
                PermissionGroupNode entity = new PermissionGroupNode();
                entity.setGroupId(groupId);
                entity.setNode(node);
                return entity;
            }).toList();
            permissionGroupNodeMapper.insert(entities);
        }
    }

    @Override
    public List<Integer> getUserGroupIds(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        QueryWrapper<UserPermissionGroup> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        return userPermissionGroupMapper.selectList(query).stream()
                .map(UserPermissionGroup::getGroupId)
                .distinct()
                .toList();
    }

    @Override
    public List<PermissionGroup> getUserGroups(Integer userId) {
        List<Integer> groupIds = getUserGroupIds(userId);
        if (groupIds.isEmpty()) {
            return List.of();
        }
        List<PermissionGroup> groups = permissionGroupMapper.selectByIds(groupIds);
        return groups == null ? List.of() : groups;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserGroups(Integer userId, Collection<Integer> groupIds) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "用户ID无效", null);
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }

        Set<Integer> targetGroupIds = groupIds == null ? Set.of() : new LinkedHashSet<>(groupIds);

        // 不能修改自己的权限组，避免把自己加入高权限组实现提权
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser != null && currentUser.getId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "不能修改自己的权限组", null);
        }

        // 目标组合计携带的节点必须都在操作者已有权限范围内
        Set<String> grantedNodes = new LinkedHashSet<>();
        for (Integer groupId : targetGroupIds) {
            requireGroup(groupId);
            grantedNodes.addAll(getGroupNodes(groupId));
        }
        ensureGrantable(grantedNodes);

        QueryWrapper<UserPermissionGroup> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("user_id", userId);
        userPermissionGroupMapper.delete(deleteQuery);

        // 批量插入，避免逐条 insert
        if (!targetGroupIds.isEmpty()) {
            List<UserPermissionGroup> entities = targetGroupIds.stream().map(groupId -> {
                UserPermissionGroup entity = new UserPermissionGroup();
                entity.setUserId(userId);
                entity.setGroupId(groupId);
                return entity;
            }).toList();
            userPermissionGroupMapper.insert(entities);
        }
    }

    @Override
    public Set<String> getUserDirectNodes(Integer userId) {
        if (userId == null) {
            return Set.of();
        }
        QueryWrapper<UserPermissionNode> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        Set<String> nodes = new LinkedHashSet<>();
        for (UserPermissionNode node : userPermissionNodeMapper.selectList(query)) {
            nodes.add(node.getNode());
        }
        return nodes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserDirectNodes(Integer userId, Collection<String> nodes) {
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "用户ID无效", null);
        }
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }
        List<String> normalized = normalizeNodes(nodes);
        ensureGrantable(normalized);

        QueryWrapper<UserPermissionNode> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("user_id", userId);
        userPermissionNodeMapper.delete(deleteQuery);

        // 批量插入，避免逐条 insert
        if (!normalized.isEmpty()) {
            List<UserPermissionNode> entities = normalized.stream().map(node -> {
                UserPermissionNode entity = new UserPermissionNode();
                entity.setUserId(userId);
                entity.setNode(node);
                return entity;
            }).toList();
            userPermissionNodeMapper.insert(entities);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyDefaultGroups(Integer userId) {
        if (userId == null) {
            return;
        }
        QueryWrapper<PermissionGroup> query = new QueryWrapper<>();
        query.apply("is_default = 1");
        List<PermissionGroup> defaultGroups = permissionGroupMapper.selectList(query);
        if (defaultGroups.isEmpty()) {
            return;
        }
        // 批量插入，避免逐条 insert
        List<UserPermissionGroup> entities = defaultGroups.stream().map(group -> {
            UserPermissionGroup entity = new UserPermissionGroup();
            entity.setUserId(userId);
            entity.setGroupId(group.getId());
            return entity;
        }).toList();
        userPermissionGroupMapper.insert(entities);
        log.info("Applied {} default permission group(s) to user {}", defaultGroups.size(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean promoteToOpIfFirstUser(Integer userId) {
        if (userId == null) {
            return false;
        }
        // 仅在系统还没有其他用户时把首个用户作为平台管理员引导，避免抢先注册者接管平台
        QueryWrapper<User> otherUserQuery = new QueryWrapper<>();
        otherUserQuery.ne("id", userId);
        if (userMapper.selectCount(otherUserQuery) > 0) {
            return false;
        }

        User user = userMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getIsOp())) {
            return false;
        }

        User update = new User();
        update.setId(userId);
        update.setIsOp(true);
        userMapper.updateById(update);
        log.warn("First user {} ({}) promoted to platform administrator for bootstrap",
                userId, user.getUsername());
        return true;
    }

    /**
     * 校验权限组是否存在
     */
    private PermissionGroup requireGroup(Integer groupId) {
        PermissionGroup group = groupId == null ? null : permissionGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "权限组不存在", null);
        }
        return group;
    }

    /**
     * 防止越权提权：只能授予自己已拥有的权限节点
     * 无当前请求上下文时视为不可信调用，一律拒绝
     */
    private void ensureGrantable(Collection<String> nodes) {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    "无法确认当前操作者身份，拒绝授予权限节点", null);
        }
        if (nodes == null || nodes.isEmpty() || isOp(currentUser.getId())) {
            return;
        }
        Set<String> owned = getPermissionNodes(currentUser.getId());
        List<String> notOwned = nodes.stream().filter(node -> !owned.contains(node)).toList();
        if (!notOwned.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    "不能授予自己未拥有的权限节点：" + String.join(", ", notOwned), null);
        }
    }

    /**
     * 去重并校验权限节点均为已注册节点
     */
    private List<String> normalizeNodes(Collection<String> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String node : nodes) {
            if (node == null || node.isBlank()) {
                continue;
            }
            String trimmed = node.trim();
            if (!PermissionRegistry.exists(trimmed)) {
                throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                        "存在无效的权限节点：" + trimmed, null);
            }
            unique.add(trimmed);
        }
        return List.copyOf(unique);
    }
}
