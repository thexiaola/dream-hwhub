package top.thexiaola.dreamhwhub.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.permission.annotation.RequirePermission;
import top.thexiaola.dreamhwhub.module.permission.service.PermissionService;

import java.util.Arrays;
import java.util.List;

/**
 * 权限节点校验拦截器
 * <p>
 * 读取 {@link RequirePermission} 注解声明的权限节点，校验当前登录用户是否拥有；
 * 平台管理员（OP）拥有全部权限节点，直接放行。
 * 依赖 {@link AuthInterceptor} 已将用户写入 request 属性，故须在其之后注册。
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(PermissionInterceptor.class);

    private final PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission annotation = resolveAnnotation(handlerMethod);
        if (annotation == null || annotation.value().length == 0) {
            // 管理后台接口必须显式声明权限节点，未声明视为配置缺失并拒绝
            logger.warn("Admin endpoint declared no @RequirePermission: {}", request.getRequestURI());
            return writeError(response, HttpServletResponse.SC_FORBIDDEN, 403, "接口未配置访问权限");
        }

        User currentUser = (User) request.getAttribute("currentUser");
        if (currentUser == null) {
            return writeError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "用户未登录");
        }

        List<String> required = Arrays.asList(annotation.value());
        Integer userId = currentUser.getId();
        if (permissionService.hasAnyPermission(userId, required)) {
            return true;
        }

        logger.warn("User {} denied access to {} (required permission: {})",
                userId, request.getRequestURI(), required);
        return writeError(response, HttpServletResponse.SC_FORBIDDEN, 403, "权限不足，缺少所需权限节点");
    }

    /**
     * 方法上的注解优先，其次取控制器类上的注解
     */
    private RequirePermission resolveAnnotation(HandlerMethod handlerMethod) {
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation != null) {
            return annotation;
        }
        return handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
    }

    /**
     * 输出统一的错误响应
     */
    private boolean writeError(HttpServletResponse response, int httpStatus, int code, String message)
            throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", code, message));
        return false;
    }
}
