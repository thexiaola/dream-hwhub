package top.thexiaola.dreamhwhub.support.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具类
 */
@Component
public class AESEncryptionUtil {

    // AES-GCM 参数
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // GCM推荐的IV长度
    private static final int GCM_TAG_LENGTH = 16; // GCM标签长度
    
    @Value("${app.password.key}")
    private String encryptionKey;

    /**
     * 加密密码并进行Base64编码
     * @param plainText 明文密码
     * @return Base64编码的加密字符串（包含IV+密文）
     */
    public String encrypt(String plainText) {
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
            
            // 加密成功，返回Base64编码的结果
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // 加密过程失败
            throw new RuntimeException("Password encryption failed", e);
        }
    }
    
    /**
     * 解密Base64编码的密码
     * @param base64EncryptedData Base64编码的加密数据（包含IV+密文）
     * @return 解密后的明文密码
     */
    public String decrypt(String base64EncryptedData) {
        try {
            // 验证输入
            if (base64EncryptedData == null || base64EncryptedData.isEmpty()) {
                // 输入验证失败
                throw new IllegalArgumentException("Encrypted data cannot be null or empty");
            }
            
            // Base64解码
            byte[] encryptedData = Base64.getDecoder().decode(base64EncryptedData);
            
            // 验证解码后的数据长度
            if (encryptedData.length <= GCM_IV_LENGTH) {
                // 输入验证失败
                throw new IllegalArgumentException("Invalid encrypted data");
            }
            
            // 初始化密钥
            SecretKeySpec key = initSecretKey();
            
            // 提取IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            
            // 执行解密
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] decryptedData = cipher.doFinal(cipherText);
            
            // 解密成功，返回结果
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解密过程失败
            throw new RuntimeException("Password decryption failed", e);
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
     * 验证密码是否正确
     * @param plainPassword 明文密码
     * @param base64EncryptedPassword Base64编码的加密密码
     * @return 密码是否匹配
     */
    public boolean verifyPassword(String plainPassword, String base64EncryptedPassword) {
        try {
            String decryptedPassword = decrypt(base64EncryptedPassword);
            return plainPassword.equals(decryptedPassword);
        } catch (Exception e) {
            // 解密失败或密码不匹配
            return false;
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