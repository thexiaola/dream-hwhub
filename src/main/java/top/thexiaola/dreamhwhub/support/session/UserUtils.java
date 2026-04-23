package top.thexiaola.dreamhwhub.support.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;

/**
 * 用户工具类，提供获取当前登录用户等常用功能
 */
public class UserUtils {
    
    /**
     * 获取当前登录用户
     * @return 当前登录的User对象，未登录则返回null
     */
    public static User getCurrentUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        
        return (User) session.getAttribute("user");
    }
    
    /**
     * 获取当前用户ID
     * @return 当前用户ID，未登录则返回null
     */
    public static Integer getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
    
    /**
     * 获取当前用户名
     * @return 当前用户名，未登录则返回null
     */
    public static String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }
    
    /**
     * 检查用户是否已登录
     * @return 已登录返回true，否则返回false
     */
    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }
    
    /**
     * 获取当前HTTP请求
     * @return HttpServletRequest对象
     */
    private static HttpServletRequest getCurrentRequest() {
        return LogUtil.getRequest();
    }
    
    /**
     * 获取客户端IP地址
     * @return 客户端IP地址
     */
    public static String getClientIpAddress() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况，取第一个非unknown的IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }
    
    /**
     * 获取格式化的用户信息字符串
     * @param user 用户对象
     * @return 格式化的用户信息
     */
    public static String getUserInfoString(User user) {
        if (user == null) {
            return "not logged in";
        }
        return String.format("id: %d, user_no: %s, username: %s, email: %s", 
            user.getId(), user.getUserNo(), user.getUsername(), user.getEmail());
    }
    
    /**
     * 获取带IP的完整用户信息字符串
     * @param user 用户对象
     * @return 带IP的完整用户信息
     */
    public static String getFullUserInfoString(User user) {
        String ip = getClientIpAddress();
        String userInfo = getUserInfoString(user);
        return String.format("ip: %s, %s", ip != null ? ip : "unknown", userInfo);
    }
}