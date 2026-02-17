package top.thexiaola.dreamhwhub.util;

import org.junit.jupiter.api.Test;
import top.thexiaola.dreamhwhub.module.login.domain.User;

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
        assertEquals("", nullUserInfo);
        
        // 测试部分字段为null的情况
        User partialUser = new User();
        partialUser.setId(2);
        partialUser.setUserNo("2021002");
        // username, email, permission 为 null
        
        String partialUserInfo = LogUtil.getUserInfo(partialUser);
        String expectedPartial = "id: 2, user_no: 2021002";
        assertEquals(expectedPartial, partialUserInfo);
        
        // 测试字符串"null"的情况
        User stringNullUser = new User();
        stringNullUser.setId(3);
        stringNullUser.setUserNo("null");
        stringNullUser.setUsername("null");
        stringNullUser.setEmail("test@example.com");
        stringNullUser.setPermission((short) 0);
        
        String stringNullUserInfo = LogUtil.getUserInfo(stringNullUser);
        String expectedStringNull = "id: 3, user_no: null, username: null, email: test@example.com, permission: 0";
        assertEquals(expectedStringNull, stringNullUserInfo);
    }
    
    @Test
    void testGetIpOnly() {
        // 验证返回格式
        String ipInfo = LogUtil.getIpOnly(null);
        assertTrue(ipInfo.startsWith("ip: "));
    }
}