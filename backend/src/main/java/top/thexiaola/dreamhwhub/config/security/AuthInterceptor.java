package top.thexiaola.dreamhwhub.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.support.jwt.JwtUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT认证拦截器：校验Token并回库确认账号状态，将当前用户写入请求属性
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    // 不需要登录即可访问的公开接口
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/api/users/login",
            "/api/users/register",
            "/api/users/getregcode",
            "/api/users/retrieve/sendcode",
            "/api/users/retrieve/resetpassword"
    ));

    public AuthInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler)
            throws Exception {

        String requestURI = request.getRequestURI();

        // 检查是否是公开接口
        if (isPublicPath(requestURI)) {
            logger.debug("Public path, skipping authentication: {}", requestURI);
            return true;
        }

        // 从请求头中获取JWT Token
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            logger.warn("Missing Authorization header for URI: {}", requestURI);
            return writeUnauthorized(response, "未提供认证Token");
        }

        // 移除Bearer前缀(如果有)
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            logger.warn("Invalid or expired token for URI: {}", requestURI);
            return writeUnauthorized(response, "Token无效或已过期");
        }

        try {
            // Token 是无状态的，仅凭签名无法得知账号是否仍然可用，
            // 因此回库确认账号存在且未被封禁，并把库中的完整用户作为当前用户
            User tokenUser = jwtUtil.getUserFromToken(token);
            User currentUser = tokenUser.getId() == null ? null : userMapper.selectById(tokenUser.getId());

            if (currentUser == null) {
                logger.warn("Token belongs to a non-existent user (ID: {}), URI: {}", tokenUser.getId(), requestURI);
                return writeUnauthorized(response, "账号不存在或已被删除");
            }
            if (Boolean.TRUE.equals(currentUser.getIsBanned())) {
                logger.warn("Banned user (ID: {}) attempted to access URI: {}", currentUser.getId(), requestURI);
                return writeUnauthorized(response, "账号已被封禁");
            }

            // 将用户信息存储到request属性中，供后续使用
            request.setAttribute("currentUser", currentUser);

            logger.debug("Authenticated user: {} (ID: {}) for URI: {}",
                    currentUser.getUsername(), currentUser.getId(), requestURI);
            return true;
        } catch (Exception e) {
            logger.error("Failed to parse token for URI: {}", requestURI, e);
            return writeUnauthorized(response, "Token解析失败");
        }
    }

    /**
     * 检查路径是否为公开接口（精确匹配，避免前缀误放行）
     */
    private boolean isPublicPath(String uri) {
        return PUBLIC_PATHS.contains(uri);
    }

    /**
     * 输出401未认证响应
     */
    private boolean writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
