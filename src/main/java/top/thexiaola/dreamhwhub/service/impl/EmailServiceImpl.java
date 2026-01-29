package top.thexiaola.dreamhwhub.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.service.EmailService;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class EmailServiceImpl implements EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    private final JavaMailSender mailSender;
    
    // 存储验证码及其过期时间
    private final Map<String, VerificationCodeInfo> verificationCodes = new ConcurrentHashMap<>();
    
    @Value("${app.verification-code.expiry-minutes:30}")
    private int expiryMinutes;
    
    @Value("${spring.mail.username}")
    private String senderEmail;
    
    @Value("${spring.mail.properties.mail.from.nickname}")
    private String senderNickname;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationCode(String email) {
        logger.info("正在为邮箱 {} 发送验证码", email);
        
        // 生成6位随机数字验证码
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(999999));
        
        // 存储验证码及过期时间
        VerificationCodeInfo codeInfo = new VerificationCodeInfo(code, LocalDateTime.now().plusMinutes(expiryMinutes));
        verificationCodes.put(email, codeInfo);
        
        logger.debug("验证码 {} 已生成并存储，将在 {} 分钟后过期", code, expiryMinutes);
        
        // 发送邮件
        sendEmail(email, code);

        logger.info("验证码发送流程完成，邮箱: {}", email);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        logger.debug("正在验证邮箱 {} 的验证码", email);
        
        VerificationCodeInfo codeInfo = verificationCodes.get(email);
        if (codeInfo != null) {
            // 检查是否过期
            if (LocalDateTime.now().isAfter(codeInfo.expiryTime())) {
                logger.warn("验证码已过期，邮箱: {}, 验证码: {}", email, code);
                
                // 验证码已过期，删除它
                verificationCodes.remove(email);
                return false;
            }
            
            if (codeInfo.code().equals(code)) {
                logger.info("验证码验证成功，邮箱: {}", email);
                
                // 验证成功后删除该验证码
                verificationCodes.remove(email);
                return true;
            } else {
                logger.warn("验证码不匹配，邮箱: {}, 输入验证码: {}, 正确验证码: {}", email, code, codeInfo.code());
            }
        } else {
            logger.warn("未找到邮箱 {} 的验证码记录", email);
        }
        return false;
    }
    
    private void sendEmail(String email, String code) {
        if (mailSender == null) {
            logger.error("邮件服务器未配置，验证码为: {} (发送至: {})", code, email);
            System.out.println("邮件服务器未配置，验证码为: " + code + " (发送至: " + email + ")");
            return;
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(email);
            helper.setSubject("验证码");
            helper.setText("您的验证码是：" + code + "，有效期" + expiryMinutes + "分钟");
            // 设置发件人地址和昵称
            helper.setFrom(senderEmail, senderNickname);
            
            mailSender.send(message);
            logger.info("验证码邮件已成功发送至: {}", email);
        } catch (Exception e) {
            // 如果邮件发送失败，记录错误但继续执行
            logger.error("邮件发送失败: {}, 邮箱: {}, 验证码: {}", e.getMessage(), email, code, e);
            System.out.println("邮件发送失败: " + e.getMessage());
            System.out.println("验证码为: " + code + " (发送至: " + email + ")");
        }
    }

    // 存储验证码及其过期时间
    private record VerificationCodeInfo(String code, LocalDateTime expiryTime) {}
}