package top.thexiaola.dreamhwhub.support.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.thexiaola.dreamhwhub.module.login.entity.User;

/**
 * 日志工具类
 * 提供用户信息和IP地址获取功能
 */
public class LogUtil {

    /**
     * 获取客户端真实IP地址
     * @param request HttpServletRequest对象
     * @return 客户端真实IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // 获取直接连接的IP地址
        String directIp = request.getRemoteAddr();

        // 1. 优先从X-Forwarded-For头获取真实IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            String[] ipAddresses = ip.split(",");
            for (String addr : ipAddresses) {
                addr = addr.trim();
                if (!addr.isEmpty() && !"unknown".equalsIgnoreCase(addr)) {
                    return addr + "-" + directIp;
                }
            }
        }

        // 2. 如果X-Forwarded-For没有有效IP，尝试从X-Real-IP获取
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            ip = ip.trim();
            return ip + "-" + directIp;
        }

        // 3. 如果以上都没有，使用request.getRemoteAddr()获取直接连接的IP
        ip = directIp;

        // 特殊处理：如果是IPv6的环回地址，返回IPv4的环回地址
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip + "-" + directIp;
    }

    /**
     * 从RequestContextHolder获取请求对象
     * @return HttpServletRequest对象，如果不存在则返回null
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 从RequestContextHolder获取客户端IP
     * @return 客户端IP地址
     */
    public static String getCurrentClientIp() {
        return getClientIp(getRequest());
    }

    /**
     * 获取用户信息字符串（简洁版）
     * @param user 用户对象
     * @return 格式化的用户信息
     */
    public static String getUserInfo(User user) {
        if (user == null) {
            return "";
        }

        // 移除末尾的逗号和空格
        String result = "id: " + user.getId() + ", " +
                "user_no: " + formatValue(user.getUserNo()) + ", " +
                "username: " + formatValue(user.getUsername()) + ", " +
                "email: " + formatValue(user.getEmail()) + ", " +
                "permission: " + user.getPermission();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }

        return result;
    }

    /**
     * 获取格式化的用户信息字符串（完整版）
     * 包含IP、用户详细信息，用于关键操作日志记录
     * @param ip 客户端IP地址
     * @param user 用户对象
     * @return 格式化的用户信息字符串
     */
    public static String getUserInfoString(String ip, User user) {
        if (user != null) {
            return String.format("ip: %s, id: %d, user_no: %s, username: %s, email: %s, permission: %d",
                    ip, user.getId(), formatValue(user.getUserNo()), formatValue(user.getUsername()), 
                    formatValue(user.getEmail()), user.getPermission());
        } else {
            return String.format("ip: %s, not logged in", ip);
        }
    }

    /**
     * 获取格式化的用户信息字符串（自动获取当前请求信息）
     * @return 格式化的用户信息字符串
     */
    public static String getUserInfoString() {
        return getUserInfoString(getCurrentClientIp(), null);
    }

    /**
     * 获取带用户信息的操作日志格式
     * @param operation 操作描述
     * @param user 用户对象
     * @return 格式化的操作日志
     */
    public static String getOperationLog(String operation, User user) {
        String ip = getCurrentClientIp();
        String userInfo = getUserInfoString(ip, user);
        return String.format("%s - %s", operation, userInfo);
    }

    /**
     * 获取带用户信息的成功操作日志
     * @param operation 操作描述
     * @param user 用户对象
     * @return 格式化的成功操作日志
     */
    public static String getSuccessLog(String operation, User user) {
        return getOperationLog(operation + " successfully", user);
    }

    /**
     * 获取带用户信息的失败操作日志
     * @param operation 操作描述
     * @param reason 失败原因
     * @param user 用户对象
     * @return 格式化的失败操作日志
     */
    public static String getFailureLog(String operation, String reason, User user) {
        String ip = getCurrentClientIp();
        String userInfo = getUserInfoString(ip, user);
        return String.format("%s failed: %s - %s", operation, reason, userInfo);
    }

    /**
     * 格式化值，处理null和"null"字符串
     * @param value 值
     * @return 格式化后的字符串
     */
    private static String formatValue(String value) {
        if (value == null || "null".equals(value)) {
            return "null";
        }
        return value;
    }
}