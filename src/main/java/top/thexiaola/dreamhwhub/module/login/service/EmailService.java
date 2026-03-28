package top.thexiaola.dreamhwhub.module.login.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmail(String to, String subject, String content);

    /**
     * 生成并发送验证码（用于注册，绑定 userNo、username、email）
     * @param email 收件人邮箱
     * @param userNo 学号/工号
     * @param username 用户名
     */
    void sendVerificationCode(String email, String userNo, String username);
    
    /**
     * 验证注册验证码 (需要匹配 userNo、username、email)
     * @param email 邮箱地址
     * @param code 验证码
     * @param userNo 学号/工号
     * @param username 用户名
     * @return 验证是否成功
     */
    boolean verifyRegistrationCode(String email, String code, String userNo, String username);
    
    /**
     * 验证换绑验证码 (需要匹配 userNo、username、email)
     * @param email 邮箱地址
     * @param code 验证码
     * @param userNo 学号/工号
     * @param username 用户名
     * @return 验证是否成功
     */
    boolean verifyModifyCode(String email, String code, String userNo, String username);
    
    /**
     * 生成并发送换绑验证码
     * @param email 收件人邮箱
     * @param userNo 学号/工号
     * @param username 用户名
     */
    void sendModifyEmailCode(String email, String userNo, String username);
    
    /**
     * 生成并发送找回密码验证码
     * @param email 收件人邮箱
     * @param userNo 学号/工号
     * @param username 用户名
     */
    void sendRetrievePasswordEmailCode(String email, String userNo, String username);
    
    /**
     * 验证找回密码验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @param userNo 学号/工号
     * @param username 用户名
     * @return 验证是否成功
     */
    boolean verifyRetrievePasswordCode(String email, String code, String userNo, String username);
}