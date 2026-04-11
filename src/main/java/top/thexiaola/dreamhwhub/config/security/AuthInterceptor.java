package top.thexiaola.dreamhwhub.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

/**
 * 认证拦截器，检查用户是否登录
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler)
            throws Exception {

        String requestURI = request.getRequestURI();

        // 检查 Session 中是否存在用户信息
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            // 返回401未认证状态
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            // 返回错误信息
            response.getWriter().write("{\"code\":401,\"message\":\"用户未登录，请先登录\",\"data\":null}");
            return false;
        }
        
        // 记录已认证用户访问
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser != null) {
            logger.debug("Authenticated user {} accessing {}", 
                UserUtils.getUserInfoString(currentUser), requestURI);
        }
        
        return true;
    }
}