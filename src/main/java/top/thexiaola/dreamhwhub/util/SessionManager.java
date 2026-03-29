package top.thexiaola.dreamhwhub.util;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.thexiaola.dreamhwhub.module.login.domain.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session管理工具类，用于管理用户Session
 * 提供根据用户ID查找、更新、销毁Session的功能
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    // 存储用户ID到Session的映射，使用ConcurrentHashMap保证线程安全
    private static final Map<Integer, HttpSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 添加用户Session到管理中
     * 
     * @param userId  用户ID
     * @param session 用户Session
     */
    public static void addSession(Integer userId, HttpSession session) {
        if (userId != null && session != null) {
            USER_SESSION_MAP.put(userId, session);
            logger.debug("Added session for user: {}", userId);
        }
    }

    /**
     * 从管理中移除用户Session
     * 
     * @param userId 用户ID
     */
    public static void removeSession(Integer userId) {
        if (userId != null) {
            USER_SESSION_MAP.remove(userId);
            logger.debug("Removed session for user: {}", userId);
        }
    }

    /**
     * 从管理中移除用户Session
     * 
     * @param session 用户Session
     */
    public static void removeSession(HttpSession session) {
        if (session != null) {
            // 遍历Map，找到对应的userId并移除
            USER_SESSION_MAP.entrySet().removeIf(entry -> entry.getValue().equals(session));
            logger.debug("Removed session from map");
        }
    }

    /**
     * 根据用户ID获取Session
     * 
     * @param userId 用户ID
     * @return 用户Session，如果不存在则返回null
     */
    public static HttpSession getSession(Integer userId) {
        if (userId != null) {
            return USER_SESSION_MAP.get(userId);
        }
        return null;
    }

    /**
     * 更新用户Session中的用户信息
     * 
     * @param userId 用户ID
     * @param user   最新的用户信息
     */
    public static void updateUserSession(Integer userId, User user) {
        if (userId != null && user != null) {
            HttpSession session = USER_SESSION_MAP.get(userId);
            if (session != null) {
                try {
                    // 更新Session中的用户信息
                    session.setAttribute("user", user);
                    logger.debug("Updated user session for user: {}", userId);
                } catch (Exception e) {
                    logger.error("Failed to update user session for user: {}", userId, e);
                }
            }
        }
    }

    /**
     * 销毁用户Session
     * 
     * @param userId 用户ID
     */
    public static void invalidateSession(Integer userId) {
        if (userId != null) {
            HttpSession session = USER_SESSION_MAP.get(userId);
            if (session != null) {
                try {
                    // 销毁Session
                    session.invalidate();
                    // 从Map中移除
                    USER_SESSION_MAP.remove(userId);
                    logger.debug("Invalidated session for user: {}", userId);
                } catch (Exception e) {
                    logger.error("Failed to invalidate session for user: {}", userId, e);
                }
            }
        }
    }

    /**
     * 销毁所有Session
     */
    public static void invalidateAllSessions() {
        for (Map.Entry<Integer, HttpSession> entry : USER_SESSION_MAP.entrySet()) {
            try {
                entry.getValue().invalidate();
                logger.debug("Invalidated all session for user: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Failed to invalidate session for user: {}", entry.getKey(), e);
            }
        }
        USER_SESSION_MAP.clear();
    }

    /**
     * 获取当前管理的Session数量
     * 
     * @return Session数量
     */
    public static int getSessionCount() {
        return USER_SESSION_MAP.size();
    }
}