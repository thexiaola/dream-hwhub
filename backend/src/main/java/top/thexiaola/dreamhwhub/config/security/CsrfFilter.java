package top.thexiaola.dreamhwhub.config.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * CSRF防护过滤器
 * 基于JWT Token的CSRF防护方案
 */
@Component
public class CsrfFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfFilter.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    // 需要CSRF保护的HTTP方法
    private static final Set<String> CSRF_PROTECTED_METHODS = new HashSet<>(
            Arrays.asList("POST", "PUT", "DELETE", "PATCH")
    );
    
    // 不需要CSRF保护的路径(白名单)
    private static final Set<String> CSRF_EXEMPT_PATHS = new HashSet<>(Arrays.asList(
            "/api/users/login",
            "/api/users/logout",
            "/api/users/register",
            "/api/users/send-code",
            "/api/users/retrieve-password/code",
            "/api/users/retrieve-password/modify"
    ));
    
    @Value("${app.csrf.enabled:true}")
    private boolean csrfEnabled;
    
    @Value("${app.csrf.header-name:X-CSRF-Token}")
    private String csrfHeaderName;
    
    @Value("${app.jwt.secret:defaultSecretKeyForDreamHwhub2026ChangeInProduction}")
    private String jwtSecret;

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
            // 策略: CSRF Token应该是JWT Token的HMAC-SHA256哈希值
            if (authToken != null && authToken.startsWith("Bearer ")) {
                String jwtToken = authToken.substring(7);
                
                // 使用HMAC-SHA256验证CSRF Token
                if (!isValidCsrfToken(jwtToken, csrfToken)) {
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
     * 使用HMAC-SHA256算法,基于JWT Token生成预期的CSRF Token并比对
     */
    private boolean isValidCsrfToken(String jwtToken, String csrfToken) {
        try {
            // 基于JWT Token和密钥生成预期的CSRF Token
            String expectedCsrfToken = generateCsrfToken(jwtToken);
            
            // 使用常量时间比较防止时序攻击
            return constantTimeEquals(expectedCsrfToken, csrfToken);
        } catch (Exception e) {
            logger.error("Failed to validate CSRF token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 基于JWT Token生成CSRF Token
     * 使用HMAC-SHA256算法
     */
    private String generateCsrfToken(String jwtToken) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8), 
            HMAC_ALGORITHM
        );
        mac.init(secretKeySpec);
        
        byte[] hmacBytes = mac.doFinal(jwtToken.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
    }
    
    /**
     * 常量时间字符串比较,防止时序攻击
     */
    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        if (expected.length() != actual.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ actual.charAt(i);
        }
        return result == 0;
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
