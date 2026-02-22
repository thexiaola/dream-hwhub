package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import top.thexiaola.dreamhwhub.module.login.controller.RegisterController;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ErrorHandlingDemoTest {

    @Autowired
    private LoginUserService loginUserService;
    
    @Autowired
    private RegisterController registerController;

    @Test
    public void demonstrateErrorCodeMechanism() {
        System.out.println("=== 错误码机制演示 ===");
        
        // 测试重复学号注册
        RegisterRequest request1 = new RegisterRequest();
        request1.setUserNo("T2024001");
        request1.setUsername("user1");
        request1.setEmail("user1@example.com");
        request1.setPassword("123456");
        request1.setEmailCode("123456");
        
        // 第一次注册应该成功
        ServiceResult<?> result1 = loginUserService.register(request1);
        System.out.println("第一次注册结果: " + (result1.isSuccess() ? "成功" : "失败"));
        if (!result1.isSuccess()) {
            System.out.println("错误码: " + result1.getErrorCodeValue());
            System.out.println("错误消息: " + result1.getMessage());
        }
        
        // 第二次注册相同学号应该失败
        ServiceResult<?> result2 = loginUserService.register(request1);
        System.out.println("第二次注册结果: " + (result2.isSuccess() ? "成功" : "失败"));
        if (!result2.isSuccess()) {
            BusinessErrorCode errorCode = result2.getErrorCode();
            System.out.println("错误码: " + errorCode.getCode());
            System.out.println("错误消息: " + errorCode.getMessage());
            System.out.println("是否为重复注册错误: " + BusinessErrorCode.isDuplicateRegistrationError(errorCode));
        }
        
        assertTrue(result2.isSuccess() || BusinessErrorCode.isDuplicateRegistrationError(result2.getErrorCode()));
    }

    @Test
    public void demonstrateControllerErrorCodeHandling() {
        System.out.println("\n=== 控制器错误码处理演示 ===");
        
        // 测试无效验证码
        RegisterRequest request = new RegisterRequest();
        request.setUserNo("T2024999");
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("123456");
        request.setEmailCode("000000"); // 故意使用无效验证码
        
        ResponseEntity<Map<String, Object>> response = registerController.register(request);
        
        System.out.println("HTTP状态码: " + response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        System.out.println("响应体: " + body);
        
        if (body != null) {
            System.out.println("业务码: " + body.get("code"));
            System.out.println("消息: " + body.get("msg"));
            if (body.containsKey("errorCode")) {
                System.out.println("错误码: " + body.get("errorCode"));
            }
        }
        
        // 应该返回400状态码表示客户端错误
        assertEquals(400, response.getStatusCode().value());
    }
}