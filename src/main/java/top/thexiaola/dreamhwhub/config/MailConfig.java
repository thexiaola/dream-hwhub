package top.thexiaola.dreamhwhub.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.beans.factory.annotation.Value;

import java.util.Properties;

// 邮件配置类
@Configuration
@ConditionalOnProperty(name = "spring.mail.host")
public class MailConfig {
    
    @Value("${spring.mail.host}")
    private String host;
    
    @Value("${spring.mail.port}")
    private int port;
    
    @Value("${spring.mail.username}")
    private String username;
    
    @Value("${spring.mail.password}")
    private String password;
    
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean smtpAuth;
    
    @Value("${spring.mail.properties.mail.smtp.ssl.enable}")
    private boolean sslEnable;
    
    @Value("${spring.mail.properties.mail.smtp.socketFactory.class:javax.net.ssl.SSLSocketFactory}")
    private String socketFactoryClass;
    
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        Properties props = new Properties();
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.ssl.enable", sslEnable);
        props.put("mail.smtp.socketFactory.class", socketFactoryClass);
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.charset", "UTF-8");
        
        mailSender.setJavaMailProperties(props);
        
        return mailSender;
    }
}