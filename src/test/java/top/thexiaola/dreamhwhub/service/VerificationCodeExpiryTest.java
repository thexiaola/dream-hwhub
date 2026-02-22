package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.verification-code.expiry-minutes=1"  // 设置较短的过期时间用于测试
})
public class VerificationCodeExpiryTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testVerificationCodeExpiry() throws InterruptedException {
        String email = "test-expiry@example.com";
        String code = "123456";
        
        System.out.println("开始测试验证码有效期功能...");
        System.out.println("当前时间: " + LocalDateTime.now());
        
        // 发送验证码
        System.out.println("1. 发送验证码: " + code);
        emailService.sendVerificationCode(email, code);
        
        // 立即验证 - 应该成功
        System.out.println("2. 立即验证验证码");
        boolean immediateResult = emailService.verifyCode(email, code);
        assertTrue(immediateResult, "验证码应该立即验证成功");
        System.out.println("   结果: 验证成功 ✓");
        
        // 等待过期时间
        System.out.println("3. 等待验证码过期 (1分钟)...");
        TimeUnit.SECONDS.sleep(65); // 等待65秒确保过期
        
        System.out.println("   当前时间: " + LocalDateTime.now());
        System.out.println("4. 再次验证已过期的验证码");
        
        // 再次验证同一验证码 - 应该失败
        boolean expiredResult = emailService.verifyCode(email, code);
        assertFalse(expiredResult, "过期的验证码应该验证失败");
        System.out.println("   结果: 验证失败 (验证码已过期) ✓");
        
        System.out.println("\n验证码有效期测试完成!");
    }

    @Test
    public void testVerificationCodeCleanup() {
        String email = "test-cleanup@example.com";
        String code = "654321";
        
        System.out.println("测试验证码自动清理功能...");
        
        // 发送验证码
        emailService.sendVerificationCode(email, code);
        
        // 验证存在
        boolean beforeCleanup = emailService.verifyCode(email, code);
        assertTrue(beforeCleanup, "验证码应该存在");
        
        // 再次发送相同邮箱的验证码，应该覆盖之前的
        emailService.sendVerificationCode(email, "999999");
        
        // 原验证码应该已经失效
        boolean afterCleanup = emailService.verifyCode(email, code);
        assertFalse(afterCleanup, "原验证码应该已被清理");
        
        System.out.println("验证码自动清理测试完成!");
    }
}