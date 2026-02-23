package top.thexiaola.dreamhwhub.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.util.UserUtils;

import java.util.Set;

/**
 * 认证拦截器，用于检查用户是否已登录
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    
    // 公开接口路径，无需认证
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/users/register",
        "/api/users/login", 
        "/api/users/getregcode",
        "/api/users/check/userno",
        "/api/users/check/username",
        "/api/users/check/email",
        "/actuator/health",
        "/favicon.ico"
    );
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) 
            throws Exception {
        
        String requestURI = request.getRequestURI();
        
        // 检查是否为公开路径
        if (isPublicPath(requestURI)) {
            return true;
        }
        
        // 检查Session中是否存在用户信息
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            logger.warn("Unauthorized access to {}: session not found or user not logged in", requestURI);
            
            // 返回401未认证状态
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            
            // 返回统一样式的错误信息
            String jsonResponse = """
                {
                    "code": 401,
                    "msg": "用户未登录，请先登录",
                    "data": null
                }
                """;
            response.getWriter().write(jsonResponse);
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
    
    /**
     * 判断请求路径是否为公开路径
     * @param requestURI 请求URI
     * @return 是公开路径返回true，否则返回false
     */
    private boolean isPublicPath(String requestURI) {
        // 精确匹配
        if (PUBLIC_PATHS.contains(requestURI)) {
            return true;
        }
        
        // 模糊匹配API检查接口
        return requestURI.startsWith("/api/users/check/");
    }
}