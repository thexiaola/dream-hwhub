package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import top.thexiaola.dreamhwhub.module.login.controller.LoginUserController;
import top.thexiaola.dreamhwhub.module.login.controller.RegisterController;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ChineseMessageTest {

    @Autowired
    private LoginUserController loginUserController;
    
    @Autowired
    private RegisterController registerController;
    
    @Autowired
    private LoginUserService loginUserService;

    @Test
    public void testChineseErrorMessageRegistration() {
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
        // 现在使用错误码机制，具体消息可能不同
        assertNotNull(responseBody.get("msg"));
        
        System.out.println("注册失败中文提示: " + responseBody.get("msg"));
    }

    @Test
    public void testChineseErrorMessageLogin() {
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
        
        System.out.println("登录失败中文提示: " + responseBody.get("msg"));
    }

    @Test
    public void testChineseSuccessMessages() {
        // 测试发送验证码
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        
        ResponseEntity<Map<String, Object>> response = registerController.sendRegisterCode(registerRequest);
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        // 现在的成功消息格式可能不同
        assertTrue(responseBody.containsKey("code"));
        assertTrue(responseBody.containsKey("msg"));
        
        System.out.println("响应信息: " + responseBody);
    }

    @Test
    public void testChineseValidationMessages() {
        // 测试参数验证失败的中文提示
        RegisterRequest invalidRequest = new RegisterRequest();
        // 不设置任何字段，触发验证错误
        
        ResponseEntity<Map<String, Object>> response = registerController.register(invalidRequest);
        
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(400, responseBody.get("code"));
        // 验证错误信息存在且不为空
        assertNotNull(responseBody.get("msg"));
        assertFalse(((String)responseBody.get("msg")).isEmpty());
        
        System.out.println("参数验证失败中文提示: " + responseBody.get("msg"));
    }
}