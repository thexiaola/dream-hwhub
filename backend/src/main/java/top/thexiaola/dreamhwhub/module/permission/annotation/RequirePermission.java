package top.thexiaola.dreamhwhub.module.permission.annotation;

import java.lang.annotation.*;

/**
 * 声明接口所需的权限节点
 * <p>
 * 可标注在控制器类或方法上：满足 {@link #value()} 中任意一个权限节点即可访问；
 * 平台管理员（OP）拥有全部权限节点，因此总是放行。
 * 节点的定义统一由 {@code PermissionNodes} / {@code PermissionRegistry} 提供。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 所需权限节点，满足任意一个即可
     *
     * @return 权限节点数组
     */
    String[] value();
}
