package top.thexiaola.dreamhwhub.support.password;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt密码加密工具类
 * 使用单向哈希算法,比AES可逆加密更安全
 */
@Component
public class PasswordUtil {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 对明文密码进行BCrypt加密
     *
     * @param plainPassword 明文密码
     * @return BCrypt哈希值
     */
    public String encode(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return PASSWORD_ENCODER.encode(plainPassword);
    }

    /**
     * 验证明文密码是否与BCrypt哈希匹配
     *
     * @param plainPassword     明文密码
     * @param encodedPassword   BCrypt哈希值
     * @return 是否匹配
     */
    public boolean matches(String plainPassword, String encodedPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return false;
        }
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }
        return PASSWORD_ENCODER.matches(plainPassword, encodedPassword);
    }
}
