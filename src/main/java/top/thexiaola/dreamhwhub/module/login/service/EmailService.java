package top.thexiaola.dreamhwhub.module.login.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送邮件
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmail(String to, String subject, String content);

    /**
     * 发送验证码邮件
     * @param to 收件人邮箱
     * @param code 验证码
     */
    void sendVerificationCode(String to, String code);
    
    /**
     * 生成并发送验证码
     * @param email 收件人邮箱
     */
    void sendVerificationCode(String email);
    
    /**
     * 验证验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @return 验证是否成功
     */
    boolean verifyCode(String email, String code);
}