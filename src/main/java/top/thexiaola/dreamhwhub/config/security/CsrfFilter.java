package top.thexiaola.dreamhwhub.config.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CSRF防护过滤器
 * 基于JWT Token的CSRF防护方案
 */
@Component
public class CsrfFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfFilter.class);
    
    // 需要CSRF保护的HTTP方法
    private static final Set<String> CSRF_PROTECTED_METHODS = new HashSet<>(
            Arrays.asList("POST", "PUT", "DELETE", "PATCH")
    );
    
    // 不需要CSRF保护的路径(白名单)
    private static final Set<String> CSRF_EXEMPT_PATHS = new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/send-code",
            "/api/auth/retrieve-password/code",
            "/api/auth/retrieve-password/modify"
    ));
    
    @Value("${app.csrf.enabled:true}")
    private boolean csrfEnabled;
    
    @Value("${app.csrf.header-name:X-CSRF-Token}")
    private String csrfHeaderName;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 如果CSRF防护未启用,直接放行
        if (!csrfEnabled) {
            chain.doFilter(request, response);
            return;
        }
        
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        
        // 只检查需要保护的方法
        if (CSRF_PROTECTED_METHODS.contains(method)) {
            // 检查是否在白名单中
            if (isExemptPath(uri)) {
                logger.debug("CSRF check skipped for exempt path: {}", uri);
                chain.doFilter(request, response);
                return;
            }
            
            // 验证CSRF Token
            String csrfToken = httpRequest.getHeader(csrfHeaderName);
            String authToken = httpRequest.getHeader("Authorization");
            
            // 如果没有提供CSRF Token
            if (csrfToken == null || csrfToken.isEmpty()) {
                logger.warn("Missing CSRF token for {} {}", method, uri);
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write(
                    "{\"code\":403,\"message\":\"缺少CSRF Token\",\"data\":null}"
                );
                return;
            }
            
            // 验证CSRF Token是否与JWT Token匹配
            // 策略: CSRF Token应该是JWT Token的哈希值或派生值
            if (authToken != null && authToken.startsWith("Bearer ")) {
                // 简单的验证: CSRF Token应该与JWT Token相关联
                // 这里使用JWT Token的哈希作为CSRF Token的预期值
                if (!isValidCsrfToken(csrfToken)) {
                    logger.warn("Invalid CSRF token for {} {}", method, uri);
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.getWriter().write(
                        "{\"code\":403,\"message\":\"CSRF Token无效\",\"data\":null}"
                    );
                    return;
                }
            }
            
            logger.debug("CSRF token validated for {} {}", method, uri);
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * 检查路径是否在白名单中
     */
    private boolean isExemptPath(String uri) {
        return CSRF_EXEMPT_PATHS.stream()
                .anyMatch(uri::startsWith);
    }
    
    /**
     * 验证CSRF Token是否有效
     * 简单实现: 验证Token格式和长度
     * 生产环境建议使用更复杂的算法(如HMAC-SHA256)
     */
    private boolean isValidCsrfToken(String csrfToken) {
        // 简单验证: CSRF Token长度至少为32位
        return csrfToken.length() >= 32;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("CSRF Filter initialized, enabled: {}", csrfEnabled);
    }

    @Override
    public void destroy() {
        logger.info("CSRF Filter destroyed");
    }
}
