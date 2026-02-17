package top.thexiaola.dreamhwhub.util;

import top.thexiaola.dreamhwhub.module.login.domain.User;
import org.slf4j.Logger;

// 日志工具类
public class LogUtil {

    /**
     * 获取用户信息字符串
     * @param user 用户对象
     * @return 格式化的用户信息字符串
     */
    public static String getUserInfo(User user) {
        if (user == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 添加非空字段
        if (user.getId() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("id: ").append(user.getId());
        }
        
        if (user.getUserNo() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("user_no: ").append(user.getUserNo());
        }
        
        if (user.getUsername() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("username: ").append(user.getUsername());
        }
        
        if (user.getEmail() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("email: ").append(user.getEmail());
        }
        
        if (user.getPermission() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("permission: ").append(user.getPermission());
        }
        
        return sb.toString();
    }
    
    /**
     * 获取客户端IP地址
     * @param request HttpServletRequest对象
     * @return IP地址
     */
    public static String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    /**
     * 获取带IP的用户信息
     * @param user 用户对象
     * @param request HttpServletRequest对象
     * @return 包含IP的完整用户信息
     */
    public static String getUserInfoWithIp(User user, jakarta.servlet.http.HttpServletRequest request) {
        String userInfo = getUserInfo(user);
        String ip = getClientIp(request);
        return userInfo + ", ip: " + ip;
    }
    
    /**
     * 获取仅包含IP的信息（用于未登录情况）
     * @param request HttpServletRequest对象
     * @return IP信息
     */
    public static String getIpOnly(jakarta.servlet.http.HttpServletRequest request) {
        return "ip: " + getClientIp(request);
    }
    
    public static void info(Logger logger, String message, User user, jakarta.servlet.http.HttpServletRequest request) {
        logger.info("[{}] {}", getUserInfoWithIp(user, request), message);
    }
    
    public static void debug(Logger logger, String message, User user, jakarta.servlet.http.HttpServletRequest request) {
        logger.debug("[{}] {}", getUserInfoWithIp(user, request), message);
    }
    
    public static void warn(Logger logger, String message, User user, jakarta.servlet.http.HttpServletRequest request) {
        logger.warn("[{}] {}", getUserInfoWithIp(user, request), message);
    }
    
    public static void error(Logger logger, String message, User user, jakarta.servlet.http.HttpServletRequest request) {
        logger.error("[{}] {}", getUserInfoWithIp(user, request), message);
    }
    
    // 未登录情况下的日志记录方法
    public static void infoAnonymous(Logger logger, String message, jakarta.servlet.http.HttpServletRequest request) {
        logger.info("[{}] {}", getIpOnly(request), message);
    }
    
    public static void debugAnonymous(Logger logger, String message, jakarta.servlet.http.HttpServletRequest request) {
        logger.debug("[{}] {}", getIpOnly(request), message);
    }
    
    public static void warnAnonymous(Logger logger, String message, jakarta.servlet.http.HttpServletRequest request) {
        logger.warn("[{}] {}", getIpOnly(request), message);
    }
    
    public static void errorAnonymous(Logger logger, String message, jakarta.servlet.http.HttpServletRequest request) {
        logger.error("[{}] {}", getIpOnly(request), message);
    }
}