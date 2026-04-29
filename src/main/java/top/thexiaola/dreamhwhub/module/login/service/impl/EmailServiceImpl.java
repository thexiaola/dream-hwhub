package top.thexiaola.dreamhwhub.module.login.service.impl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
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
                throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "邮箱地址不存在或无效，请检查后重新输入");
            }
            log.error("Failed to send email: {}, recipient: {}, subject: {}", errorMessage, to, subject);
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "邮件发送失败：" + errorMessage);
        } catch (Exception e) {
            log.error("Failed to send email: {}, recipient: {}, subject: {}", e.getMessage(), to, subject, e);
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 发送验证码（通用方法）
     * @param email 邮箱
     * @param userNo 学号/工号
     * @param username 用户名
     * @param isModify 是否为换绑验证码
     * @param isRetrieve 是否为找回密码验证码
     */
    private void sendVerificationCodeInternal(String email, String userNo, String username, boolean isModify, boolean isRetrieve) {
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
        String compositeKey;
        if (isRetrieve) {
            compositeKey = buildRetrievePasswordKey(userNo, username, email);
        } else if (isModify) {
            compositeKey = buildModifyKey(userNo, username, email);
        } else {
            compositeKey = buildCompositeKey(userNo, username, email);
        }
        VerificationCodeInfo codeInfo = new VerificationCodeInfo(code, LocalDateTime.now().plusMinutes(expiryMinutes));
        verificationCodes.put(compositeKey, codeInfo);
    
        // 记录发送时间
        emailLastSendTime.put(email, LocalDateTime.now());
    
        // 记录验证码生成日志
        String codeType = isRetrieve ? "retrieve password" : (isModify ? "modify" : "registration");
        log.info("Generated {} verification code {} for email: {}, userNo: {}, username: {}", 
                codeType, code, email, userNo, username);
    
        // 发送邮件
        if (isRetrieve) {
            sendRetrievePasswordCodeEmail(email, code);
        } else if (isModify) {
            sendModifyCodeEmail(email, code);
        } else {
            sendVerificationCodeEmail(email, code);
        }
    }

    /**
     * 发送注册验证码（绑定 userNo、username、email）
     */
    @Override
    public void sendVerificationCode(String email, String userNo, String username) {
        sendVerificationCodeInternal(email, userNo, username, false, false);
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
     * 验证注册验证码 (需要匹配 userNo、username、email)
     * @param email 邮箱地址
     * @param code 验证码
     * @param userNo 学号/工号
     * @param username 用户名
     * @return 验证是否成功
     */
    @Override
    public boolean verifyRegistrationCode(String email, String code, String userNo, String username) {
        return verifyCodeInternal(buildCompositeKey(userNo, username, email), code);
    }
    
    /**
     * 验证换绑验证码 (需要匹配 userNo、username、email)
     * @param email 邮箱地址
     * @param code 验证码
     * @param userNo 学号/工号
     * @param username 用户名
     * @return 验证是否成功
     */
    @Override
    public boolean verifyModifyCode(String email, String code, String userNo, String username) {
        return verifyCodeInternal(buildModifyKey(userNo, username, email), code);
    }
    
    /**
     * 内部验证码验证方法
     * @param key 验证码存储的 key
     * @param code 用户输入的验证码
     * @return 验证是否成功
     */
    private boolean verifyCodeInternal(String key, String code) {
        VerificationCodeInfo codeInfo = verificationCodes.get(key);
        if (codeInfo != null) {
            // 检查是否过期
            if (LocalDateTime.now().isAfter(codeInfo.expiryTime())) {
                verificationCodes.remove(key);
                return false;
            }
            if (codeInfo.code().equals(code)) {
                // 验证成功后删除该验证码
                verificationCodes.remove(key);
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
    
    /**
     * 发送换绑验证码
     */
    @Override
    public void sendModifyEmailCode(String email, String userNo, String username) {
        sendVerificationCodeInternal(email, userNo, username, true, false);
    }
    
    /**
     * 构建换绑验证码的组合键：modify#userNo#username#email
     */
    private String buildModifyKey(String userNo, String username, String email) {
        return "modify#" + userNo + "#" + username + "#" + email;
    }
    
    /**
     * 构建找回密码验证码的组合键：retrieve#userNo#username#email
     */
    private String buildRetrievePasswordKey(String userNo, String username, String email) {
        return "retrieve#" + userNo + "#" + username + "#" + email;
    }
    
    /**
     * 发送找回密码验证码
     */
    @Override
    public void sendRetrievePasswordEmailCode(String email, String userNo, String username) {
        sendVerificationCodeInternal(email, userNo, username, false, true);
    }
    
    /**
     * 验证找回密码验证码
     */
    @Override
    public boolean verifyRetrievePasswordCode(String email, String code, String userNo, String username) {
        return verifyCodeInternal(buildRetrievePasswordKey(userNo, username, email), code);
    }
    
    private void sendRetrievePasswordCodeEmail(String email, String code) {
        String subject = "Dream HWHub 找回密码验证码";
        String content = String.format(
                "您好！\n\n您正在申请找回密码，验证码是：%s。\n\n验证码有效期为%d分钟，请及时使用。\n\n如非本人操作，请立即联系管理员。\n\n此邮件由系统自动发送，请勿回复。\n\nDream HWHub 团队",
                code, expiryMinutes
        );
        sendEmail(email, subject, content);
    }
    
    private void sendModifyCodeEmail(String email, String code) {
        String subject = "Dream HWHub 换绑验证码";
        String content = String.format(
                "您好！\n\n您正在申请修改绑定邮箱，验证码是：%s。\n\n验证码有效期为%d分钟，请及时使用。\n\n如非本人操作，请立即联系管理员。\n\n此邮件由系统自动发送，请勿回复。\n\nDream HWHub 团队",
                code, expiryMinutes
        );
        sendEmail(email, subject, content);
    }
    
    // 存储验证码及其过期时间
    private record VerificationCodeInfo(String code, LocalDateTime expiryTime) {}
}