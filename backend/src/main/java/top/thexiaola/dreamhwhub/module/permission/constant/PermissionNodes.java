package top.thexiaola.dreamhwhub.module.permission.constant;

/**
 * 权限节点常量定义（LuckPerms 风格的权限节点）
 * <p>
 * 节点命名规范：{@code 模块:资源:动作}，例如 {@code user:add}、{@code permission:group:assign}。
 * 每个后台操作对应一个权限节点，统一在此登记，禁止在其他位置硬编码节点字符串。
 */
public final class PermissionNodes {

    private PermissionNodes() {
    }

    // ===== 用户管理 =====
    public static final String USER_VIEW = "user:view";
    public static final String USER_ADD = "user:add";
    public static final String USER_EDIT = "user:edit";
    public static final String USER_DELETE = "user:delete";
    public static final String USER_BAN = "user:ban";
    public static final String USER_SET_OP = "user:setop";

    // ===== 权限管理 =====
    public static final String PERMISSION_VIEW = "permission:view";
    public static final String PERMISSION_GROUP_ADD = "permission:group:add";
    public static final String PERMISSION_GROUP_EDIT = "permission:group:edit";
    public static final String PERMISSION_GROUP_DELETE = "permission:group:delete";
    public static final String PERMISSION_GROUP_ASSIGN = "permission:group:assign";
    public static final String PERMISSION_USER_ASSIGN = "permission:user:assign";

    // ===== 班级管理 =====
    public static final String CLASS_VIEW_ALL = "class:view_all";
    /** 在任意学校下创建班级（不受“必须是该校老师”限制） */
    public static final String CLASS_CREATE = "class:create";
    public static final String CLASS_DISSOLVE = "class:dissolve";
    public static final String CLASS_UPDATE = "class:update";
    public static final String CLASS_MEMBER_KICK = "class:member:kick";
    public static final String CLASS_APPROVE_JOIN = "class:approve_join";
    public static final String CLASS_TEACHER_ADD = "class:teacher:add";

    // ===== 学校管理（平台管理员侧） =====
    /** 查看平台上所有学校 */
    public static final String SCHOOL_VIEW_ALL = "school:view_all";
    /** 创建学校 */
    public static final String SCHOOL_CREATE = "school:create";
    /** 管理任意学校（修改信息、设置加入是否需要审核） */
    public static final String SCHOOL_UPDATE = "school:update";
    /** 解散任意学校 */
    public static final String SCHOOL_DISSOLVE = "school:dissolve";
    /** 指派或取消学校管理员 */
    public static final String SCHOOL_ADMIN_ASSIGN = "school:admin:assign";
}
