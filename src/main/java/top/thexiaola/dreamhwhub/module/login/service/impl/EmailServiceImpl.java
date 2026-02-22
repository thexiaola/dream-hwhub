package top.thexiaola.dreamhwhub.module.login.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
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
    
    @Autowired
    private JavaMailSender mailSender;
    
    // 存储验证码及其过期时间
    private final Map<String, VerificationCodeInfo> verificationCodes = new ConcurrentHashMap<>();

    @Value("${app.verification-code.expiry-minutes:30}")
    private int expiryMinutes;
    
    @Value("${spring.mail.username}")
    private String senderEmail;
    
    @Value("${spring.mail.properties.mail.from.nickname:系统管理员}")
    private String senderNickname;
    
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
    public ServiceResult<Void> sendEmail(String to, String subject, String content) {
        if (mailSender == null) {
            log.error("Mail server not configured, recipient: {}, subject: {}", to, subject);
            return ServiceResult.failure(BusinessErrorCode.EMAIL_SERVER_NOT_CONFIGURED);
        }
        
        // 验证邮箱格式
        if (to == null || !to.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            log.warn("Invalid email format: {}", to);
            return ServiceResult.failure(BusinessErrorCode.INVALID_EMAIL_FORMAT);
        }
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(senderEmail, ISO_to_UTF8(senderNickname), "UTF-8"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);
            mailSender.send(message);
            return ServiceResult.success(null);
        } catch (Exception e) {
            log.error("Failed to send email: {}, recipient: {}, subject: {}", e.getMessage(), to, subject, e);
            return ServiceResult.failure(BusinessErrorCode.EMAIL_SENDING_FAILED, "邮件发送失败: " + e.getMessage());
        }
    }

    @Override
    public ServiceResult<Void> sendVerificationCode(String email) {
        // 生成6位随机数字验证码
        Random random = new Random();
        String code = String.format("%06d", random.nextInt(999999));
        
        // 存储验证码及过期时间
        VerificationCodeInfo codeInfo = new VerificationCodeInfo(code, LocalDateTime.now().plusMinutes(expiryMinutes));
        verificationCodes.put(email, codeInfo);

        // 发送邮件
        return sendVerificationCodeEmail(email, code);
    }

    @Override
    public ServiceResult<Void> sendVerificationCode(String to, String code) {
        return sendVerificationCodeEmail(to, code);
    }
    
    public boolean verifyCode(String email, String code) {
        VerificationCodeInfo codeInfo = verificationCodes.get(email);
        if (codeInfo != null) {
            // 检查是否过期
            if (LocalDateTime.now().isAfter(codeInfo.expiryTime())) {
                // 验证码已过期，删除它
                verificationCodes.remove(email);
                return false;
            }
            
            if (codeInfo.code().equals(code)) {
                // 验证成功后删除该验证码
                verificationCodes.remove(email);
                return true;
            }
        }
        return false;
    }
    
    private ServiceResult<Void> sendVerificationCodeEmail(String email, String code) {
        String subject = "Dream HWHub 验证码";
        String content = String.format(
                "您好！\n\n您的验证码是：%s\n\n验证码有效期为%d分钟，请及时使用。\n\n此邮件由系统自动发送，请勿回复。\n\nDream HWHub 团队",
                code, expiryMinutes
        );
        return sendEmail(email, subject, content);
    }
    
    // 存储验证码及其过期时间
    private record VerificationCodeInfo(String code, LocalDateTime expiryTime) {}
}