package top.thexiaola.dreamhwhub.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AESEncryptionDecryptionTest {

    @Autowired
    private AESEncryptionUtil aesEncryptionUtil;

    @Test
    public void testEncryptDecryptConsistency() {
        System.out.println("=== AES加解密一致性测试 ===");
        
        String originalPassword = "mySecretPassword123";
        
        // 加密
        byte[] encryptedPassword = aesEncryptionUtil.encrypt(originalPassword);
        System.out.println("原始密码: " + originalPassword);
        System.out.println("加密后长度: " + encryptedPassword.length + " 字节");
        
        // 解密
        String decryptedPassword = aesEncryptionUtil.decrypt(encryptedPassword);
        System.out.println("解密后密码: " + decryptedPassword);
        
        // 验证一致性
        assertEquals(originalPassword, decryptedPassword, "解密后的密码应该与原始密码一致");
        System.out.println("✓ 加解密一致性验证通过");
    }

    @Test
    public void testPasswordVerification() {
        System.out.println("\n=== 密码验证功能测试 ===");
        
        String password = "testPassword123";
        
        // 加密密码
        byte[] encryptedPassword = aesEncryptionUtil.encrypt(password);
        
        // 验证正确密码
        boolean correctVerification = aesEncryptionUtil.verifyPassword(password, encryptedPassword);
        assertTrue(correctVerification, "正确密码应该验证通过");
        System.out.println("✓ 正确密码验证通过");
        
        // 验证错误密码
        boolean wrongVerification = aesEncryptionUtil.verifyPassword("wrongPassword", encryptedPassword);
        assertFalse(wrongVerification, "错误密码应该验证失败");
        System.out.println("✓ 错误密码验证失败");
    }

    @Test
    public void testMultipleEncryptionsDifferentResults() {
        System.out.println("\n=== 多次加密结果差异性测试 ===");
        
        String samePassword = "samePassword";
        
        // 对同一密码进行多次加密
        byte[] encrypted1 = aesEncryptionUtil.encrypt(samePassword);
        byte[] encrypted2 = aesEncryptionUtil.encrypt(samePassword);
        byte[] encrypted3 = aesEncryptionUtil.encrypt(samePassword);
        
        System.out.println("第一次加密结果长度: " + encrypted1.length);
        System.out.println("第二次加密结果长度: " + encrypted2.length);
        System.out.println("第三次加密结果长度: " + encrypted3.length);
        
        // 验证加密结果确实不同（由于随机IV）
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted2), "相同密码的两次加密结果应该不同");
        assertFalse(java.util.Arrays.equals(encrypted1, encrypted3), "相同密码的两次加密结果应该不同");
        assertFalse(java.util.Arrays.equals(encrypted2, encrypted3), "相同密码的两次加密结果应该不同");
        System.out.println("✓ 多次加密结果确实不同（IV随机性）");
        
        // 但都能正确解密
        assertEquals(samePassword, aesEncryptionUtil.decrypt(encrypted1));
        assertEquals(samePassword, aesEncryptionUtil.decrypt(encrypted2));
        assertEquals(samePassword, aesEncryptionUtil.decrypt(encrypted3));
        System.out.println("✓ 所有加密结果都能正确解密");
    }

    @Test
    public void testEdgeCases() {
        System.out.println("\n=== 边界情况测试 ===");
        
        // 测试空密码
        assertThrows(IllegalArgumentException.class, () -> {
            aesEncryptionUtil.encrypt("");
        }, "空密码应该抛出异常");
        System.out.println("✓ 空密码处理正确");
        
        // 测试null密码
        assertThrows(IllegalArgumentException.class, () -> {
            aesEncryptionUtil.encrypt(null);
        }, "null密码应该抛出异常");
        System.out.println("✓ null密码处理正确");
        
        // 测试解密无效数据
        assertThrows(RuntimeException.class, () -> {
            aesEncryptionUtil.decrypt(new byte[]{1, 2, 3}); // 太短的数据
        }, "无效加密数据应该抛出异常");
        System.out.println("✓ 无效数据解密处理正确");
    }
}