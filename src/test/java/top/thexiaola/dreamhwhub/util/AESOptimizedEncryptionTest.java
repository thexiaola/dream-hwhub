package top.thexiaola.dreamhwhub.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AESOptimizedEncryptionTest {

    @Autowired
    private AESEncryptionUtil aesEncryptionUtil;

    @Test
    public void testEncryptOnly() {
        String password = "MySecurePassword123!";
        
        // 测试加密功能
        byte[] encryptedBytes = aesEncryptionUtil.encrypt(password);
        
        System.out.println("原始密码: " + password);
        System.out.println("加密后字节长度: " + encryptedBytes.length);
        System.out.println("加密后字节数据: " + java.util.Arrays.toString(encryptedBytes));
        
        // 验证加密结果不为空
        assertNotNull(encryptedBytes);
        assertTrue(encryptedBytes.length > 0);
        assertNotEquals(password.getBytes().length, encryptedBytes.length);
        
        System.out.println("✅ AES加密功能测试通过");
    }

    @Test
    public void testMultipleEncryptionsDifferentResults() {
        String password = "SamePassword123";
        
        // 多次加密同一个密码应该产生不同的结果（因为随机IV）
        byte[] encrypted1 = aesEncryptionUtil.encrypt(password);
        byte[] encrypted2 = aesEncryptionUtil.encrypt(password);
        
        // 验证每次加密结果都不同
        assertNotEquals(encrypted1.length, 0);
        assertNotEquals(encrypted2.length, 0);
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2), 
                   "相同密码的多次加密结果应该不同（随机IV）");
        
        System.out.println("✅ 多次加密随机性测试通过");
    }

    @Test
    public void testEdgeCases() {
        // 测试边界情况
        assertThrows(RuntimeException.class, () -> {
            aesEncryptionUtil.encrypt(null);
        }, "加密null密码应该抛出异常");
        
        assertThrows(RuntimeException.class, () -> {
            aesEncryptionUtil.encrypt("");
        }, "加密空密码应该抛出异常");
        
        // 测试特殊字符
        String specialPassword = "Special!@#$%^&*()_+-=[]{}|;:,.<>?中文测试";
        byte[] encrypted = aesEncryptionUtil.encrypt(specialPassword);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);
        
        System.out.println("✅ 边界情况测试通过");
    }
}