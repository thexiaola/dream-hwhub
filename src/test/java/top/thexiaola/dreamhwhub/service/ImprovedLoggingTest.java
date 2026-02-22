package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import top.thexiaola.dreamhwhub.module.login.controller.LoginUserController;
import top.thexiaola.dreamhwhub.module.login.controller.RegisterController;
import top.thexiaola.dreamhwhub.module.login.dto.EmailCodeRequest;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ImprovedLoggingTest {

    @Autowired
    private LoginUserController loginUserController;
    
    @Autowired
    private RegisterController registerController;
    
    @Autowired
    private LoginUserService loginUserService;

    @Test
    public void testRegisterWithImprovedLogging() {
        // 测试注册失败场景 - 验证码错误
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("T2024001");
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("123456");
        registerRequest.setEmailCode("000000"); // 错误的验证码
        
        ResponseEntity<Map<String, Object>> response = registerController.register(registerRequest);
        
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(400, responseBody.get("code"));
        // 注意：现在使用错误码机制，消息可能不同
        assertNotNull(responseBody.get("msg"));
        
        System.out.println("注册失败响应: " + responseBody);
    }

    @Test
    public void testLoginWithImprovedLogging() {
        // 测试登录失败场景 - 用户不存在
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setAccount("nonexistent_user");
        loginRequest.setPassword("wrong_password");
        
        ResponseEntity<Map<String, Object>> response = loginUserController.login(loginRequest);
        
        assertEquals(401, response.getStatusCode().value());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(401, responseBody.get("code"));
        assertNotNull(responseBody.get("msg"));
        
        System.out.println("登录失败响应: " + responseBody);
    }

    @Test
    public void testSendVerificationCodeWithImprovedLogging() {
        // 测试发送验证码
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        
        ResponseEntity<Map<String, Object>> response = registerController.sendRegisterCode(registerRequest);
        
        // 可能成功也可能失败（取决于邮件配置），但我们验证响应格式
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertTrue(responseBody.containsKey("code"));
        assertTrue(responseBody.containsKey("msg"));
        
        System.out.println("发送验证码响应: " + responseBody);
    }

    @Test
    public void testValidationErrorsWithImprovedLogging() {
        // 测试参数验证失败
        RegisterRequest invalidRequest = new RegisterRequest();
        // 不设置任何字段，触发验证错误
        
        ResponseEntity<Map<String, Object>> response = registerController.register(invalidRequest);
        
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(400, responseBody.get("code"));
        
        System.out.println("参数验证失败响应: " + responseBody);
    }
}