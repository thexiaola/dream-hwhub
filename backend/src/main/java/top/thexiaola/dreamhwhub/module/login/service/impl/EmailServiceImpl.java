package top.thexiaola.dreamhwhub.module.login.service.impl;

import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.PreDestroy;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    
    // 存储验证码及其过期时间（使用LRU Map限制最大容量为10000）
    private final Map<String, VerificationCodeInfo> verificationCodes = Collections.synchronizedMap(
            new LinkedHashMap<>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, VerificationCodeInfo> eldest) {
                    return size() > 10000; // 最大容量10000条
                }
            }
    );
    
    // 存储邮箱最后发送时间（同样限制容量）
    private final Map<String, LocalDateTime> emailLastSendTime = Collections.synchronizedMap(
            new LinkedHashMap<>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, LocalDateTime> eldest) {
                    return size() > 10000;
                }
            }
    );

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

    // 异步发送邮件的线程池，避免 SMTP 耗时阻塞接口响应
    private final ExecutorService mailExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "mail-sender");
        t.setDaemon(false);
        return t;
    });

    @PreDestroy
    public void shutdownMailExecutor() {
        mailExecutor.shutdown();
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
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "邮件发送失败，请稍后重试");
        } catch (Exception e) {
            log.error("Failed to send email: {}, recipient: {}, subject: {}", e.getMessage(), to, subject, e);
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "邮件发送失败，请稍后重试");
        }
    }

    /**
     * 发送验证码（通用方法）
     * @param email 邮箱
     * @param username 用户名
     * @param isModify 是否为换绑验证码
     * @param isRetrieve 是否为找回密码验证码
     * @param isSensitive 是否为敏感操作身份验证码
     */
    private void sendVerificationCodeInternal(String email, String username, boolean isModify, boolean isRetrieve,
                                              boolean isSensitive) {
        // 检查发送频率限制
        Long remainingTime = checkSendFrequency(email);
        if (remainingTime != null && remainingTime > 0) {
            throw new BusinessException(BusinessErrorCode.EMAIL_SENDING_FAILED, "验证码已发送，请在" + remainingTime + "秒后再次尝试", remainingTime);
        }
        
        // 生成 6 位随机数字验证码
        String code = RandomUtil.randomNumbers(6);
            
        // 删除该邮箱的所有旧验证码
        removeOldVerificationCodesByEmail(email);
        
        // 使用组合 key 存储新验证码
        String compositeKey;
        if (isRetrieve) {
            compositeKey = buildRetrievePasswordKey(username, email);
        } else if (isModify) {
            compositeKey = buildModifyKey(username, email);
        } else if (isSensitive) {
            compositeKey = buildSensitiveOperationKey(username, email);
        } else {
            compositeKey = buildCompositeKey(username, email);
        }
        VerificationCodeInfo codeInfo = new VerificationCodeInfo(code, LocalDateTime.now().plusMinutes(expiryMinutes));
        verificationCodes.put(compositeKey, codeInfo);
    
        // 记录发送时间
        emailLastSendTime.put(email, LocalDateTime.now());

        // 异步发送邮件，SMTP 耗时可能较长，避免阻塞接口响应
        if (isRetrieve) {
            mailExecutor.execute(() -> sendCodeEmailQuietly(code, () -> sendRetrievePasswordCodeEmail(email, code), email));
        } else if (isModify) {
            mailExecutor.execute(() -> sendCodeEmailQuietly(code, () -> sendModifyCodeEmail(email, code), email));
        } else if (isSensitive) {
            mailExecutor.execute(() -> sendCodeEmailQuietly(code, () -> sendSensitiveOperationCodeEmail(email, code), email));
        } else {
            mailExecutor.execute(() -> sendCodeEmailQuietly(code, () -> sendVerificationCodeEmail(email, code), email));
        }
    }

    /**
     * 静默兜底包装：sendEmail 内部已记录详细错误日志，这里仅避免未捕获异常进入线程默认处理器；
     * 注意：日志中严禁打印验证码等敏感内容
     */
    private void sendCodeEmailQuietly(String code, Runnable sender, String email) {
        try {
            sender.run();
            log.info("Verification code sent successfully to email: {}", email);
        } catch (Exception e) {
            log.warn("Failed to send verification code email to {} in background: {}", email, e.getMessage());
        }
    }

    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * 发送注册验证码（绑定 username、email）
     */
    @Override
    public void sendVerificationCode(String email, String username) {
        sendVerificationCodeInternal(email, username, false, false, false);
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
     * 验证注册验证码 (需要匹配 username、email)
     * @param email 邮箱地址
     * @param code 验证码
     * @param username 用户名
     * @return 验证是否成功
     */
    @Override
    public boolean verifyRegistrationCode(String email, String code, String username) {
        return verifyCodeInternal(buildCompositeKey(username, email), code);
    }
    
    /**
     * 验证换绑验证码 (需要匹配 username、email)
     * @param email 邮箱地址
     * @param code 验证码
     * @param username 用户名
     * @return 验证是否成功
     */
    @Override
    public boolean verifyModifyCode(String email, String code, String username) {
        return verifyCodeInternal(buildModifyKey(username, email), code);
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
     * 构建组合键：username#email
     */
    private String buildCompositeKey(String username, String email) {
        return username + "#" + email;
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
    public void sendModifyEmailCode(String email, String username) {
        sendVerificationCodeInternal(email, username, true, false, false);
    }
    
    /**
     * 构建换绑验证码的组合键：modify#username#email
     */
    private String buildModifyKey(String username, String email) {
        return "modify#" + username + "#" + email;
    }
    
    /**
     * 构建找回密码验证码的组合键：retrieve#username#email
     */
    private String buildRetrievePasswordKey(String username, String email) {
        return "retrieve#" + username + "#" + email;
    }
    
    /**
     * 发送找回密码验证码
     */
    @Override
    public void sendRetrievePasswordEmailCode(String email, String username) {
        sendVerificationCodeInternal(email, username, false, true, false);
    }
    
    /**
     * 验证找回密码验证码
     */
    @Override
    public boolean verifyRetrievePasswordCode(String email, String code, String username) {
        return verifyCodeInternal(buildRetrievePasswordKey(username, email), code);
    }

    /**
     * 发送「敏感操作」身份验证码
     */
    @Override
    public void sendSensitiveOperationCode(String email, String username) {
        sendVerificationCodeInternal(email, username, false, false, true);
    }

    /**
     * 验证「敏感操作」身份验证码
     */
    @Override
    public boolean verifySensitiveOperationCode(String email, String code, String username) {
        return verifyCodeInternal(buildSensitiveOperationKey(username, email), code);
    }

    /**
     * 构建敏感操作验证码的组合键：sensitive#username#email
     */
    private String buildSensitiveOperationKey(String username, String email) {
        return "sensitive#" + username + "#" + email;
    }

    private void sendSensitiveOperationCodeEmail(String email, String code) {
        String subject = "Dream HWHub 敏感操作身份验证码";
        String content = String.format(
                "您好！\n\n您正在进行一项敏感操作（如解散班级、退出学校、移出成员或管理操作），身份验证码是：%s。\n\n验证码有效期为%d分钟，请及时使用。\n\n如非本人操作，请立即修改密码并联系管理员。\n\n此邮件由系统自动发送，请勿回复。\n\nDream HWHub 团队",
                code, expiryMinutes
        );
        sendEmail(email, subject, content);
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
                "您好！\n\n您正在申请修改绑定邮箱，验证码是：%s。\n\n验证码有效期为%d分钟，请及时使用。\n如非本人操作，请立即联系管理员。\n此邮件由系统自动发送，请勿回复。\n\nDream HWHub 团队",
                code, expiryMinutes
        );
        sendEmail(email, subject, content);
    }
    
    // 存储验证码及其过期时间
    private record VerificationCodeInfo(String code, LocalDateTime expiryTime) {}
}