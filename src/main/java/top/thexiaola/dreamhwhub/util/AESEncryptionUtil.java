package top.thexiaola.dreamhwhub.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES-256-GCM 加密工具类
 */
@Component
public class AESEncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(AESEncryptionUtil.class);
    
    // AES-GCM 参数
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // GCM推荐的IV长度
    private static final int GCM_TAG_LENGTH = 16; // GCM标签长度
    
    @Value("${app.password.key}")
    private String encryptionKey;

    /**
     * 加密密码
     * @param plainText 明文密码
     * @return 加密后的密码字节数组（包含IV+密文）
     */
    public byte[] encrypt(String plainText) {
        try {
            // 验证输入
            if (plainText == null || plainText.isEmpty()) {
                // 输入验证失败
                throw new IllegalArgumentException("Password cannot be null or empty");
            }
            
            // 初始化密钥
            SecretKeySpec key = initSecretKey();
            
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            
            // 执行加密
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            
            // 组合IV和密文
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);
            
            // 加密成功，返回结果
            return combined;
        } catch (Exception e) {
            // 加密过程失败
            throw new RuntimeException("Password encryption failed", e);
        }
    }

    /**
     * 初始化AES密钥
     */
    private SecretKeySpec initSecretKey() {
        try {
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                // 密钥配置缺失
                throw new IllegalStateException("Encryption key is missing");
            }
            
            byte[] keyBytes = hexStringToByteArray(encryptionKey);
            if (keyBytes.length != 32) {
                // 密钥长度无效
                throw new IllegalArgumentException("Invalid encryption key length");
            }
            
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            // 密钥初始化失败
            throw new RuntimeException("AES encryption initialization failed", e);
        }
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     */
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
}