package top.thexiaola.dreamhwhub.module.school.constant;

/**
 * 学校成员角色常量
 * 角色数值越大权限越高：学校管理员 &gt; 老师 &gt; 学生
 */
public final class SchoolMemberRole {

    private SchoolMemberRole() {
    }

    /** 学生：可加入班级、提交作业 */
    public static final int STUDENT = 0;

    /** 老师：可创建并管理班级 */
    public static final int TEACHER = 1;

    /** 学校管理员：可审核加入学校的申请、设置免审核、管理成员与身份 */
    public static final int ADMIN = 2;

    /**
     * 获取角色名称
     *
     * @param role 角色代码
     * @return 角色名称（学校管理员/老师/学生）
     */
    public static String nameOf(Integer role) {
        if (role == null) {
            return "学生";
        }
        return switch (role) {
            case ADMIN -> "学校管理员";
            case TEACHER -> "老师";
            default -> "学生";
        };
    }
}
