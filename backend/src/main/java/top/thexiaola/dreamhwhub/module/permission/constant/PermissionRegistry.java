package top.thexiaola.dreamhwhub.module.permission.constant;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限节点注册表：代码内置的权限节点清单与分组树
 * <p>
 * 数据库仅保存「权限组 → 节点」「用户 → 组 / 节点」的绑定关系，
 * 节点本身由本类集中定义，从而避免出现拼写错误或无效的节点。
 * 新增权限节点时只需在 {@link PermissionNodes} 登记常量并在此加入分组。
 */
public final class PermissionRegistry {

    private PermissionRegistry() {
    }

    /**
     * 单个权限节点
     */
    public static final class Node {

        private final String node;
        private final String name;
        private final String description;

        public Node(String node, String name, String description) {
            this.node = node;
            this.name = name;
            this.description = description;
        }

        public String getNode() {
            return node;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 权限节点分组（用于前端渲染分组勾选树）
     */
    public static final class Group {

        private final String key;
        private final String name;
        private final List<Node> nodes;

        public Group(String key, String name, List<Node> nodes) {
            this.key = key;
            this.name = name;
            this.nodes = nodes;
        }

        public String getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public List<Node> getNodes() {
            return nodes;
        }
    }

    /** 全部权限节点分组 */
    public static final List<Group> GROUPS = List.of(
            new Group("user", "用户管理", List.of(
                    new Node(PermissionNodes.USER_VIEW, "查看用户", "查看用户列表与详情"),
                    new Node(PermissionNodes.USER_ADD, "添加用户", "在后台新增用户"),
                    new Node(PermissionNodes.USER_EDIT, "编辑用户", "修改用户资料"),
                    new Node(PermissionNodes.USER_DELETE, "删除用户", "删除用户账号及其数据"),
                    new Node(PermissionNodes.USER_BAN, "封禁用户", "封禁或解封用户"),
                    new Node(PermissionNodes.USER_SET_OP, "设置平台管理员", "授予或取消用户的平台管理员(OP)"))),
            new Group("permission", "权限管理", List.of(
                    new Node(PermissionNodes.PERMISSION_VIEW, "查看权限", "查看权限节点与权限组"),
                    new Node(PermissionNodes.PERMISSION_GROUP_ADD, "新增权限组", "创建权限组"),
                    new Node(PermissionNodes.PERMISSION_GROUP_EDIT, "编辑权限组", "修改权限组信息与所含节点"),
                    new Node(PermissionNodes.PERMISSION_GROUP_DELETE, "删除权限组", "删除权限组"),
                    new Node(PermissionNodes.PERMISSION_GROUP_ASSIGN, "分配权限组", "为用户分配权限组"),
                    new Node(PermissionNodes.PERMISSION_USER_ASSIGN, "分配用户节点", "为用户单独授予权限节点"))),
            new Group("class", "班级管理", List.of(
                    new Node(PermissionNodes.CLASS_VIEW_ALL, "查看全部班级", "查看平台上所有班级"),
                    new Node(PermissionNodes.CLASS_UPDATE, "管理任意班级", "修改任意班级信息"),
                    new Node(PermissionNodes.CLASS_DISSOLVE, "解散任意班级", "解散非自己创建的班级"),
                    new Node(PermissionNodes.CLASS_MEMBER_KICK, "踢出班级成员", "从任意班级踢出成员"),
                    new Node(PermissionNodes.CLASS_APPROVE_JOIN, "审批加入申请", "审批任意班级的加入申请"),
                    new Node(PermissionNodes.CLASS_TEACHER_ADD, "添加班级老师", "为任意班级添加老师"))));

    /** 全部权限节点 */
    private static final Set<String> ALL_NODES = buildAllNodes();

    private static Set<String> buildAllNodes() {
        Set<String> nodes = new LinkedHashSet<>();
        for (Group group : GROUPS) {
            for (Node node : group.getNodes()) {
                nodes.add(node.getNode());
            }
        }
        return Set.copyOf(nodes);
    }

    /**
     * 获取全部权限节点
     *
     * @return 不可变的权限节点集合
     */
    public static Set<String> allNodes() {
        return ALL_NODES;
    }

    /**
     * 判断权限节点是否已注册
     *
     * @param node 权限节点
     * @return true-已注册
     */
    public static boolean exists(String node) {
        return node != null && ALL_NODES.contains(node);
    }

    /**
     * 获取某个分组下的全部权限节点
     *
     * @param groupKey 分组标识，如 class
     * @return 该分组下的权限节点集合，分组不存在时返回空集合
     */
    public static Set<String> groupNodes(String groupKey) {
        Set<String> nodes = new LinkedHashSet<>();
        for (Group group : GROUPS) {
            if (group.getKey().equals(groupKey)) {
                for (Node node : group.getNodes()) {
                    nodes.add(node.getNode());
                }
            }
        }
        return Set.copyOf(nodes);
    }
}
