package top.thexiaola.dreamhwhub.support.session;

import jakarta.servlet.http.HttpServletRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;

/**
 * 用户工具类，提供获取当前登录用户等常用功能 - 基于JWT Token
 */
public class UserUtils {
    
    /**
     * 获取当前登录用户(从request属性中获取)
     * @return 当前登录的User对象，未登录则返回null
     */
    public static User getCurrentUser() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        
        // 从request属性中获取由AuthInterceptor设置的用户信息
        return (User) request.getAttribute("currentUser");
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
     * 获取当前HTTP请求
     * @return HttpServletRequest对象
     */
    private static HttpServletRequest getCurrentRequest() {
        return LogUtil.getRequest();
    }
}