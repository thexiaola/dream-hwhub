package top.thexiaola.dreamhwhub.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.support.jwt.JwtUtil;

/**
 * JWT认证拦截器，从Token中提取用户信息
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler)
            throws Exception {

        String requestURI = request.getRequestURI();

        // 从请求头中获取JWT Token
        String token = request.getHeader("Authorization");
        
        if (token == null || token.isEmpty()) {
            // 返回401未认证状态
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            // 返回错误信息
            response.getWriter().write("{\"code\":401,\"message\":\"未提供认证Token\",\"data\":null}");
            logger.warn("Missing Authorization header for URI: {}", requestURI);
            return false;
        }
        
        // 移除Bearer前缀(如果有)
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\",\"data\":null}");
            logger.warn("Invalid or expired token for URI: {}", requestURI);
            return false;
        }
        
        // 从Token中提取用户信息
        try {
            Integer userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            
            // 创建简化的User对象并存储到request属性中
            User user = new User();
            user.setId(userId);
            user.setUsername(username);
            
            // 将用户信息存储到request属性中，供后续使用
            request.setAttribute("currentUser", user);
            
            logger.debug("Authenticated user: {} (ID: {}) for URI: {}", username, userId, requestURI);
            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token解析失败\",\"data\":null}");
            logger.error("Failed to parse token for URI: {}", requestURI, e);
            return false;
        }
    }
}