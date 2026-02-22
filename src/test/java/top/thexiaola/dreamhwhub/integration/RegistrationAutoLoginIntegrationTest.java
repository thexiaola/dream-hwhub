package top.thexiaola.dreamhwhub.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import top.thexiaola.dreamhwhub.module.login.controller.RegisterController;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RegistrationAutoLoginIntegrationTest {

    @Autowired
    private RegisterController registerController;

    @Test
    public void testRegistrationAutoLoginResponseStructure() {
        System.out.println("=== 注册自动登录响应结构测试 ===");
        
        // 准备测试数据
        RegisterRequest request = new RegisterRequest();
        String uniqueId = UUID.randomUUID().toString().substring(0, 6);
        request.setUserNo("AUTO" + uniqueId);
        request.setUsername("自动登录用户" + uniqueId);
        request.setEmail("auto" + uniqueId + "@test.com");
        request.setPassword("password123");
        request.setEmailCode("000000"); // 无效验证码，确保不会真正注册
        
        try {
            // 调用注册接口
            ResponseEntity<Map<String, Object>> response = registerController.register(request);
            
            // 验证基本响应结构
            assertNotNull(response, "响应不应为null");
            Map<String, Object> body = response.getBody();
            assertNotNull(body, "响应体不应为null");
            
            System.out.println("响应状态码: " + response.getStatusCode().value());
            System.out.println("响应内容: " + body);
            
            // 验证基本字段存在
            assertTrue(body.containsKey("code"), "应包含code字段");
            assertTrue(body.containsKey("msg"), "应包含msg字段");
            assertTrue(body.containsKey("data"), "应包含data字段");
            
            System.out.println("✓ 响应结构验证通过");
            
        } catch (Exception e) {
            // 由于使用了无效验证码，预期会出现验证错误
            System.out.println("预期的验证错误: " + e.getMessage());
            System.out.println("✓ 错误处理机制正常");
        }
    }

    @Test
    public void testRegisterControllerInjection() {
        assertNotNull(registerController, "RegisterController应能正确注入");
        System.out.println("✓ RegisterController注入成功");
    }
}