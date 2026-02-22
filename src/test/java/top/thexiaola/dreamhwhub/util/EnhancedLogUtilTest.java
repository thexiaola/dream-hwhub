package top.thexiaola.dreamhwhub.util;

import org.junit.jupiter.api.Test;
import top.thexiaola.dreamhwhub.module.login.domain.User;

import static org.junit.jupiter.api.Assertions.*;

public class EnhancedLogUtilTest {

    @Test
    public void testGetUserInfoString() {
        // 测试用户信息字符串生成
        User user = new User();
        user.setId(1);
        user.setUserNo("STU001");
        user.setUsername("张三");
        user.setEmail("zhangsan@example.com");
        user.setPermission((short) 10);
        
        String ip = "192.168.1.100";
        String userInfo = LogUtil.getUserInfoString(ip, user);
        
        System.out.println("生成的用户信息日志: " + userInfo);
        
        // 验证包含关键信息
        assertTrue(userInfo.contains("ip: " + ip));
        assertTrue(userInfo.contains("id: 1"));
        assertTrue(userInfo.contains("user_no: STU001"));
        assertTrue(userInfo.contains("username: 张三"));
        assertTrue(userInfo.contains("email: zhangsan@example.com"));
        assertTrue(userInfo.contains("permission: 10"));
    }

    @Test
    public void testOperationLogs() {
        User user = new User();
        user.setId(1);
        user.setUserNo("STU001");
        user.setUsername("张三");
        user.setEmail("zhangsan@example.com");
        user.setPermission((short) 10);
        
        // 测试成功操作日志
        String successLog = LogUtil.getSuccessLog("User registration", user);
        System.out.println("成功操作日志: " + successLog);
        assertTrue(successLog.contains("User registration successfully"));
        assertTrue(successLog.contains("id: 1"));
        
        // 测试失败操作日志
        String failureLog = LogUtil.getFailureLog("User login", "incorrect password", user);
        System.out.println("失败操作日志: " + failureLog);
        assertTrue(failureLog.contains("User login failed: incorrect password"));
        assertTrue(failureLog.contains("id: 1"));
        
        // 测试操作日志
        String operationLog = LogUtil.getOperationLog("Password reset", user);
        System.out.println("操作日志: " + operationLog);
        assertTrue(operationLog.contains("Password reset"));
        assertTrue(operationLog.contains("id: 1"));
    }

    @Test
    public void testNullUserHandling() {
        // 测试null用户处理
        String ip = "192.168.1.100";
        String userInfo = LogUtil.getUserInfoString(ip, null);
        System.out.println("null用户信息: " + userInfo);
        assertTrue(userInfo.contains("ip: " + ip));
        assertTrue(userInfo.contains("not logged in"));
        
        // 测试null用户操作日志
        String log = LogUtil.getSuccessLog("Email verification", null);
        System.out.println("null用户操作日志: " + log);
        assertTrue(log.contains("Email verification successfully"));
        assertTrue(log.contains("not logged in"));
    }

    @Test
    public void testFormatValue() {
        // 测试null值处理（通过间接方式测试）
        User user = new User();
        user.setUserNo(null);
        user.setUsername("null");
        
        String userInfo = LogUtil.getUserInfo(user);
        assertTrue(userInfo.contains("user_no: null"));
        assertTrue(userInfo.contains("username: null"));
    }
}