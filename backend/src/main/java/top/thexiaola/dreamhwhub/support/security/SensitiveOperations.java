package top.thexiaola.dreamhwhub.support.security;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感操作（危险操作）注册表
 * <p>
 * 每个受二次验证保护的操作都有一个**稳定标识**（key，形如 {@code class.dissolve}），
 * 前端、注解与用户设置均以 key 对齐；名称与说明集中在此维护，避免各处硬编码。
 * <p>
 * 用户可在「危险操作验证」中逐个操作开启/关闭二次验证；某个操作是否对该用户可见，
 * 取决于该用户当前是否真的能执行该操作（权限节点或成员身份），由
 * {@code SensitiveOperationSettingsService} 判定。
 */
public final class SensitiveOperations {

    private SensitiveOperations() {
    }

    // ===== 班级 =====
    /** 解散班级 */
    public static final String CLASS_DISSOLVE = "class.dissolve";
    /** 踢出班级成员 */
    public static final String CLASS_KICK_MEMBER = "class.kick_member";
    /** 退出班级 */
    public static final String CLASS_LEAVE = "class.leave";
    /** 转让班级 */
    public static final String CLASS_TRANSFER = "class.transfer";

    // ===== 学校 =====
    /** 退出学校 */
    public static final String SCHOOL_LEAVE = "school.leave";
    /** 解散学校 */
    public static final String SCHOOL_DISSOLVE = "school.dissolve";
    /** 指派/取消学校管理员 */
    public static final String SCHOOL_ASSIGN_ADMIN = "school.assign_admin";

    // ===== 权限 =====
    /** 删除权限组 */
    public static final String PERMISSION_GROUP_DELETE = "permission.group.delete";
    /** 设置权限组节点 */
    public static final String PERMISSION_GROUP_SET_NODES = "permission.group.set_nodes";
    /** 分配权限组 */
    public static final String PERMISSION_GROUP_ASSIGN = "permission.group.assign";
    /** 分配权限节点 */
    public static final String PERMISSION_USER_ASSIGN = "permission.user.assign";

    // ===== 用户 =====
    /** 删除用户 */
    public static final String USER_DELETE = "user.delete";
    /** 封禁/解封用户 */
    public static final String USER_BAN = "user.ban";
    /** 设置平台管理员身份 */
    public static final String USER_SET_OP = "user.set_op";

    // ===== 作业 =====
    /** 撤回/删除作业提交 */
    public static final String SUBMISSION_WITHDRAW = "submission.withdraw";

    /**
     * 单个敏感操作的元信息
     *
     * @param key         稳定标识
     * @param name        展示名称
     * @param description 说明文案
     */
    public record Operation(String key, String name, String description) {
    }

    /** 全部敏感操作（顺序即前端展示顺序） */
    public static final List<Operation> ALL = List.of(
            new Operation(CLASS_DISSOLVE, "解散班级", "解散你创建或有权管理的班级，数据不可恢复"),
            new Operation(CLASS_KICK_MEMBER, "踢出班级成员", "将成员移出你管理的班级并清理其作业提交"),
            new Operation(CLASS_LEAVE, "退出班级", "退出你已加入的班级并清理你在该班的作业提交"),
            new Operation(CLASS_TRANSFER, "转让班级", "把你创建的班级转让给其他成员，转让后你将成为课代表"),
            new Operation(SCHOOL_LEAVE, "退出学校", "退出你已加入的学校并移出该校所有班级"),
            new Operation(SCHOOL_DISSOLVE, "解散学校", "解散学校并解除其全部成员关系与申请数据"),
            new Operation(SCHOOL_ASSIGN_ADMIN, "指派学校管理员", "指派或取消学校的学校管理员身份"),
            new Operation(PERMISSION_GROUP_DELETE, "删除权限组", "删除权限组并解除其节点与用户绑定"),
            new Operation(PERMISSION_GROUP_SET_NODES, "设置权限组节点", "修改权限组包含的权限节点"),
            new Operation(PERMISSION_GROUP_ASSIGN, "分配权限组", "为用户分配或取消权限组"),
            new Operation(PERMISSION_USER_ASSIGN, "分配权限节点", "为用户单独授予或取消权限节点"),
            new Operation(USER_DELETE, "删除用户", "删除用户账号及其关联数据"),
            new Operation(USER_BAN, "封禁/解封用户", "封禁或解封用户账号"),
            new Operation(USER_SET_OP, "设置平台管理员身份", "授予或取消用户的平台管理员（OP）身份"),
            new Operation(SUBMISSION_WITHDRAW, "撤回提交", "撤回或删除作业提交及其附件"));

    private static final Map<String, Operation> BY_KEY = buildIndex();

    private static Map<String, Operation> buildIndex() {
        Map<String, Operation> map = new LinkedHashMap<>();
        for (Operation op : ALL) {
            map.put(op.key(), op);
        }
        return Map.copyOf(map);
    }

    /**
     * 按 key 获取操作元信息
     *
     * @param key 操作标识
     * @return 操作元信息，未登记时返回 null
     */
    public static Operation byKey(String key) {
        return key == null ? null : BY_KEY.get(key);
    }

    /**
     * 判断操作标识是否已登记
     *
     * @param key 操作标识
     * @return true-已登记
     */
    public static boolean exists(String key) {
        return key != null && BY_KEY.containsKey(key);
    }

    /**
     * 取操作的展示名称，未登记时回退为 key 本身
     *
     * @param key 操作标识
     * @return 展示名称
     */
    public static String nameOf(String key) {
        Operation op = byKey(key);
        return op == null ? (key == null ? "" : key) : op.name();
    }
}
