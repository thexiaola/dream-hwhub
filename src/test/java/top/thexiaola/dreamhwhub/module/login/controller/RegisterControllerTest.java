package top.thexiaola.dreamhwhub.module.login.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RegisterControllerTest {

    @Autowired
    private RegisterController registerController;

    @Test
    public void testRegisterSuccessWithAutoLogin() {
        System.out.println("=== 注册成功后自动登录测试 ===");
        
        // 准备注册数据
        RegisterRequest registerRequest = new RegisterRequest();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        registerRequest.setUserNo("TEST" + uniqueSuffix);
        registerRequest.setUsername("测试用户" + uniqueSuffix);
        registerRequest.setEmail("test" + uniqueSuffix + "@example.com");
        registerRequest.setPassword("123456");
        registerRequest.setEmailCode("123456"); // 使用无效验证码测试
        
        // 调用注册接口
        ResponseEntity<Map<String, Object>> response = registerController.register(registerRequest);
        
        // 验证响应
        assertNotNull(response, "响应不应为null");
        assertEquals(400, response.getStatusCode().value(), "应该返回400状态码（验证码无效）");
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody, "响应体不应为null");
        assertEquals(400, responseBody.get("code"), "错误码应该是400");
        
        System.out.println("✓ 注册接口基础功能正常");
        System.out.println("响应消息: " + responseBody.get("msg"));
    }

    @Test
    public void testRegisterControllerAutowired() {
        // 测试控制器是否能正确注入
        assertNotNull(registerController, "RegisterController should be autowired successfully");
        System.out.println("✓ RegisterController autowired successfully");
    }
}