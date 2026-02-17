package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testSendVerificationCode() {
        // 测试发送验证码功能
        String testEmail = "test@example.com";
        
        // 发送验证码
        assertDoesNotThrow(() -> emailService.sendVerificationCode(testEmail));
        
        System.out.println("验证码发送测试完成");
    }

    @Test
    public void testVerifyCode() {
        // 测试验证码验证功能
        String testEmail = "test2@example.com";
        String testCode = "123456";
        
        // 先发送验证码
        emailService.sendVerificationCode(testEmail);
        
        // 验证验证码（这里会返回false因为我们不知道实际发送的验证码）
        boolean result = emailService.verifyCode(testEmail, testCode);
        assertFalse(result, "验证码应该验证失败");
        
        System.out.println("验证码验证测试完成");
    }

    @Test
    public void testInvalidEmailFormat() {
        // 测试无效邮箱格式
        String invalidEmail = "invalid-email";
        
        // 发送验证码应该不会抛出异常，但会在日志中记录警告
        assertDoesNotThrow(() -> emailService.sendVerificationCode(invalidEmail));
        
        System.out.println("无效邮箱格式测试完成");
    }
}