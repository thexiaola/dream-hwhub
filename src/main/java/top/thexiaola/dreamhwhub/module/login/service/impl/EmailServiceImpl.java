package top.thexiaola.dreamhwhub.module.login.service.impl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮件服务实现类
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    private final JavaMailSender mailSender;
    
    // 存储验证码及其过期时间
    private final Map<String, VerificationCodeInfo> verificationCodes = new ConcurrentHashMap<>();
    
    // 存储邮箱最后发送时间
    private final Map<String, LocalDateTime> emailLastSendTime = new ConcurrentHashMap<>();

    // 发送验证码的冷却时间（秒）
    @Value("${app.verification-code.cooldown-seconds}")
    private int cooldownSeconds;

    // 验证码有效期（分钟）
    @Value("${app.verification-code.expiry-minutes}")
    private int expiryMinutes;
    
    @Value("${spring.mail.username}")
    private String senderEmail;
    
    @Value("${spring.mail.properties.mail.from.nickname:系统管理员}")
    private String senderNickname;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        if (mailSender != null) {
            log.info("EmailServiceImpl initialized successfully with JavaMailSender: {}", mailSender.getClass().getSimpleName());
        } else {
            log.error("CRITICAL: JavaMailSender is NULL in EmailServiceImpl constructor! Bean creation order issue?");
        }
    }

    /**
     * ISO-8859-1 编码转换 UTF-8 编码
     * @param text ISO-8859-1 编码的文本
     * @return UTF-8 编码的文本
     */
    private String ISO_to_UTF8(String text) {
        if (text == null) return null;
        try {
            // 尝试用ISO-8859-1解码再用UTF-8编码
            byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果转换失败，返回原文本
            return text;
        }
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        if (mailSender == null) {
            log.error("Mail server not configured, recipient: {}, subject: {}", to, subject);
            throw new BusinessException(BusinessErrorCode.EMAIL_SERVER_NOT_CONFIGURED);
        }
            
        // 验证邮箱格式
        if (to == null || !to.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            log.warn("Invalid email format: {}", to);
            throw new BusinessException(BusinessErrorCode.INVALID_EMAIL_FORMAT);
        }
            
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(senderEmail, ISO_to_UTF8(senderNickname), "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);
            mailSender.send(message);
        } catch (MailSendException e) {
            // 特殊处理邮件发送失败，特别是 550 错误（邮箱不存在）
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("550")) {
                log.warn("Email address does not exist or is invalid: {}, SMTP error: {}", to, errorMessage);
                throw new BusinessException(BusinessErrorCode.INVALID_EMAIL_FORMAT, "邮箱地址不存在或无效，请检查后重新输入");
            }
            log.error("Failed to send email: {}, recipient: {}, subject: {}", errorMessage, to, subject);
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "邮件发送失败：" + errorMessage);
        } catch (Exception e) {
            log.error("Failed to send email: {}, recipient: {}, subject: {}", e.getMessage(), to, subject, e);
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 发送注册验证码（绑定 userNo、username、email）
     */
    @Override
    public void sendVerificationCode(String email, String userNo, String username) {
        // 检查发送频率限制
        Long remainingTime = checkSendFrequency(email);
        if (remainingTime != null && remainingTime > 0) {
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "验证码已发送，请在" + remainingTime + "秒后再次尝试", remainingTime);
        }
        
        // 生成 6 位随机数字验证码
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(999999));
            
        // 删除该邮箱的所有旧验证码
        removeOldVerificationCodesByEmail(email);
        
        // 使用组合 key 存储新验证码
        String compositeKey = buildCompositeKey(userNo, username, email);
        VerificationCodeInfo codeInfo = new VerificationCodeInfo(code, LocalDateTime.now().plusMinutes(expiryMinutes));
        verificationCodes.put(compositeKey, codeInfo);
    
        // 记录发送时间
        emailLastSendTime.put(email, LocalDateTime.now());
    
        // 记录验证码生成日志
        log.info("Generated verification code {} for email: {}, userNo: {}, username: {}", code, email, userNo, username);
    
        // 发送邮件
        sendVerificationCodeEmail(email, code);
    }
    
    /**
     * 检查发送频率限制
     * @return 剩余等待时间（秒），如果无限制则返回 null
     */
    private Long checkSendFrequency(String email) {
        LocalDateTime lastSendTime = emailLastSendTime.get(email);
        if (lastSendTime != null) {
            long secondsSinceLastSend = Duration.between(lastSendTime, LocalDateTime.now()).getSeconds();
            if (secondsSinceLastSend < cooldownSeconds) {
                return cooldownSeconds - secondsSinceLastSend;
            }
        }
        return null;
    }
    
    /**
     * 删除指定邮箱的所有旧验证码
     */
    private void removeOldVerificationCodesByEmail(String email) {
        verificationCodes.keySet().removeIf(key -> key.endsWith("#" + email));
    }
        
    /**
     * 验证注册验证码（需要匹配 userNo、username、email）
     */
    @Override
    public boolean verifyCode(String email, String code, String userNo, String username) {
        String compositeKey = buildCompositeKey(userNo, username, email);
        VerificationCodeInfo codeInfo = verificationCodes.get(compositeKey);
        if (codeInfo != null) {
            // 检查是否过期
            if (LocalDateTime.now().isAfter(codeInfo.expiryTime())) {
                // 验证码已过期，删除它
                verificationCodes.remove(compositeKey);
                return false;
            }
                
            if (codeInfo.code().equals(code)) {
                // 验证成功后删除该验证码
                verificationCodes.remove(compositeKey);
                return true;
            }
        }
        return false;
    }
        
    /**
     * 构建组合键：userNo#username#email
     */
    private String buildCompositeKey(String userNo, String username, String email) {
        return userNo + "#" + username + "#" + email;
    }
    
    private void sendVerificationCodeEmail(String email, String code) {
        String subject = "Dream HWHub 验证码";
        String content = String.format(
                "您好！\n\n您的验证码是：%s。\n\n验证码有效期为%d分钟，请及时使用。\n\n此邮件由系统自动发送，请勿回复。\n\nDream HWHub 团队",
                code, expiryMinutes
        );
        sendEmail(email, subject, content);
    }
    
    // 存储验证码及其过期时间
    private record VerificationCodeInfo(String code, LocalDateTime expiryTime) {}
}