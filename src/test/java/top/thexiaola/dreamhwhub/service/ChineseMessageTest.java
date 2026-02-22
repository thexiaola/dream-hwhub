package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import top.thexiaola.dreamhwhub.module.login.controller.LoginUserController;
import top.thexiaola.dreamhwhub.module.login.dto.EmailCodeRequest;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ChineseMessageTest {

    @Autowired
    private LoginUserController loginUserController;

    @Test
    public void testChineseErrorMessageRegistration() {
        // 测试注册失败场景 - 验证码错误
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUserNo("T2024001");
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("123456");
        registerRequest.setEmailCode("000000"); // 错误的验证码
        
        ResponseEntity<Map<String, Object>> response = loginUserController.register(registerRequest);
        
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        assertEquals(400, responseBody.get("code"));
        assertEquals("邮箱验证码无效或已过期", responseBody.get("msg"));
        
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
        assertEquals("账号或密码错误！", responseBody.get("msg"));
        
        System.out.println("登录失败中文提示: " + responseBody.get("msg"));
    }

    @Test
    public void testChineseSuccessMessages() {
        // 测试发送验证码成功提示
        EmailCodeRequest emailCodeRequest = new EmailCodeRequest();
        emailCodeRequest.setEmail("test@example.com");
        
        ResponseEntity<Map<String, Object>> response = loginUserController.sendRegisterCode(emailCodeRequest);
        
        Map<String, Object> responseBody = response.getBody();
        assertNotNull(responseBody);
        if (responseBody.get("code").equals(200)) {
            assertEquals("验证码发送成功！", responseBody.get("msg"));
            System.out.println("验证码发送成功中文提示: " + responseBody.get("msg"));
        } else {
            // 如果发送失败，也应该有中文提示
            assertTrue(((String)responseBody.get("msg")).contains("失败"));
            System.out.println("验证码发送失败中文提示: " + responseBody.get("msg"));
        }
    }

    @Test
    public void testChineseValidationMessages() {
        // 测试参数验证失败的中文提示
        RegisterRequest invalidRequest = new RegisterRequest();
        // 不设置任何字段，触发验证错误
        
        ResponseEntity<Map<String, Object>> response = loginUserController.register(invalidRequest);
        
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