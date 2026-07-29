package top.thexiaola.dreamhwhub.support.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.thexiaola.dreamhwhub.module.login.entity.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类 - 用于生成、验证和解析JWT Token
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret:defaultSecretKeyForDreamHwhub2026ChangeInProduction}")
    private String secret;

    @Value("${app.jwt.expiration:86400000}") // 默认24小时(毫秒)
    private long expiration;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT Token，包含完整用户信息
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("userNo", user.getUserNo());
        claims.put("email", user.getEmail());
        claims.put("permission", user.getPermission());
        claims.put("isBanned", user.getIsBanned() != null && user.getIsBanned() ? 1 : 0);
        claims.put("phone", user.getPhone());
        claims.put("idName", user.getIdName());
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String token = Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        log.debug("Generated JWT token for user: {}", user.getId());
        return token;
    }

    /**
     * 从Token中解析Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired token");
        }
    }

    /**
     * 从Token中获取完整用户信息（无需查库）
     */
    public User getUserFromToken(String token) {
        Claims claims = parseToken(token);
        User user = new User();
        user.setId(claims.get("userId", Integer.class));
        user.setUsername(claims.get("username", String.class));
        user.setUserNo(claims.get("userNo", String.class));
        user.setEmail(claims.get("email", String.class));
        user.setPermission(claims.get("permission", Short.class));
        Integer isBanned = claims.get("isBanned", Integer.class);
        user.setIsBanned(isBanned != null && isBanned == 1);
        user.setPhone(claims.get("phone", String.class));
        user.setIdName(claims.get("idName", String.class));
        return user;
    }

    /**
     * 从Token中获取用户ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Integer.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
