package top.thexiaola.dreamhwhub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件配置类
 */
@Slf4j
@Configuration
@PropertySource("classpath:mail-config.properties")
public class MailConfig {

    @Value("${spring.mail.host}")
    private String mailHost;
    
    @Value("${spring.mail.port}")
    private int mailPort;
    
    @Value("${spring.mail.username}")
    private String mailUsername;
    
    @Value("${spring.mail.password}")
    private String mailPassword;
    
    @Value("${spring.mail.properties.mail.transport.protocol:smtp}")
    private String mailProtocol;
    
    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String mailSmtpAuth;
    
    @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
    private String mailSmtpSslEnable;
    
    @Value("${spring.mail.properties.mail.smtp.ssl.checkserveridentity:true}")
    private String mailSmtpSslCheckServerIdentity;
    
    @Value("${spring.mail.properties.mail.smtp.socketFactory.class:javax.net.ssl.SSLSocketFactory}")
    private String mailSocketFactoryClass;
    
    @Value("${spring.mail.properties.mail.smtp.socketFactory.fallback:false}")
    private String mailSocketFactoryFallback;
    
    @Value("${spring.mail.properties.mail.smtp.charset:UTF-8}")
    private String mailCharset;

    /**
     * 邮件发送器配置
     */
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        if (mailHost == null || mailHost.isEmpty()) {
            log.error("spring.mail.host not configured in mail-config.properties");
            return null;
        }
        
        if (mailUsername == null || mailUsername.isEmpty()) {
            log.error("spring.mail.username not configured in mail-config.properties");
            return null;
        }
        
        if (mailPassword == null || mailPassword.isEmpty()) {
            log.error("spring.mail.password not configured in mail-config.properties");
            return null;
        }
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", mailProtocol);
        props.put("mail.smtp.auth", mailSmtpAuth);
        props.put("mail.smtp.ssl.enable", mailSmtpSslEnable);
        props.put("mail.smtp.ssl.checkserveridentity", mailSmtpSslCheckServerIdentity);
        props.put("mail.smtp.socketFactory.class", mailSocketFactoryClass);
        props.put("mail.smtp.socketFactory.fallback", mailSocketFactoryFallback);
        props.put("mail.smtp.charset", mailCharset);

        log.info("JavaMailSender initialized with host: {}, username: {}", mailHost, mailUsername);
        
        return mailSender;
    }
}