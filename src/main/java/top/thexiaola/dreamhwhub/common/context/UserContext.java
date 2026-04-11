package top.thexiaola.dreamhwhub.common.context;

import top.thexiaola.dreamhwhub.module.login.domain.User;

/**
 * 用户上下文
 */
public class UserContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public static void setUser(User user) {
        currentUser.set(user);
    }

    public static User getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
