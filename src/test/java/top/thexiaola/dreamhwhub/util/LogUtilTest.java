package top.thexiaola.dreamhwhub.util;

import org.junit.jupiter.api.Test;
import top.thexiaola.dreamhwhub.domain.User;

import static org.junit.jupiter.api.Assertions.*;

public class LogUtilTest {

    @Test
    void testGetUserInfo() {
        // 测试正常用户信息
        User user = new User();
        user.setId(1);
        user.setUserNo("2021001");
        user.setUsername("张三");
        user.setEmail("zhangsan@example.com");
        user.setPermission((short) 1);
        
        String userInfo = LogUtil.getUserInfo(user);
        String expected = "id: 1, user_no: 2021001, username: 张三, email: zhangsan@example.com, permission: 1";
        assertEquals(expected, userInfo);
        
        // 测试null用户
        String nullUserInfo = LogUtil.getUserInfo(null);
        String expectedNull = "id: null, user_no: null, username: null, email: null, permission: null";
        assertEquals(expectedNull, nullUserInfo);
    }
}