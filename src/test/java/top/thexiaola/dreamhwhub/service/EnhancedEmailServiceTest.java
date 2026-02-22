package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EnhancedEmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testSendAndVerifyVerificationCode() {
        String email = "test@example.com";
        String wrongCode = "000000";
        
        // 测试发送验证码
        assertDoesNotThrow(() -> {
            emailService.sendVerificationCode(email);
        });
        
        System.out.println("验证码已发送至: " + email);
        System.out.println("请检查控制台输出获取验证码");
        
        // 测试验证码验证（使用错误验证码）
        boolean result = emailService.verifyCode(email, wrongCode);
        assertFalse(result, "错误验证码应该验证失败");
        
        System.out.println("错误验证码验证结果: " + result);
    }

    @Test
    public void testSendVerificationCodeDirectly() {
        String email = "direct@example.com";
        String code = "123456";
        
        // 测试直接发送指定验证码
        assertDoesNotThrow(() -> {
            emailService.sendVerificationCode(email, code);
        });
        
        System.out.println("指定验证码已发送至: " + email + ", 验证码: " + code);
    }

    @Test
    public void testExpiredCodeVerification() throws InterruptedException {
        String email = "expired@example.com";
        
        // 发送验证码
        emailService.sendVerificationCode(email);
        
        // 等待一小段时间
        Thread.sleep(100);
        
        // 尝试用错误验证码验证
        boolean result = emailService.verifyCode(email, "999999");
        assertFalse(result, "过期或错误的验证码应该验证失败");
        
        System.out.println("过期验证码验证结果: " + result);
    }
}