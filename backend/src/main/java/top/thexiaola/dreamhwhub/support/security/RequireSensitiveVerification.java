package top.thexiaola.dreamhwhub.support.security;

import java.lang.annotation.*;

/**
 * 声明接口需要「敏感操作身份二次验证」
 * <p>
 * 适用于解散班级、退出学校、踢出成员，以及平台管理员的高危/权限类操作。
 * 标注后，调用方须通过请求头提供操作者本人的身份凭据之一：
 * <ul>
 *   <li>登录密码：{@code X-Verify-Method: password} + {@code X-Verify-Password: &lt;登录密码&gt;}</li>
 *   <li>邮箱验证码：{@code X-Verify-Method: email_code} + {@code X-Verify-Code: &lt;验证码&gt;}</li>
 * </ul>
 * 由 {@link SensitiveVerificationInterceptor} 统一校验，须在认证拦截器之后执行。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireSensitiveVerification {

    /**
     * 用于前端提示的操作名称（如「解散班级」），留空则由校验器给出通用提示。
     * <p>
     * 若未显式提供 {@link #key()}，则以此名称回退作为操作标识。
     *
     * @return 操作名称
     */
    String value() default "";

    /**
     * 稳定的操作标识（见 {@link SensitiveOperations}，如 {@code class.dissolve}）。
     * <p>
     * 用户可逐个操作开关二次验证；拦截器以该标识判断该操作是否被用户关闭而跳过验证。
     * 留空时回退到 {@link #value()}。
     *
     * @return 操作标识
     */
    String key() default "";
}
