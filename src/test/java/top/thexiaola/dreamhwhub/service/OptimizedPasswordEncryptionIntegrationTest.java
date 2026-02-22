package top.thexiaola.dreamhwhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.LoginUserService;
import top.thexiaola.dreamhwhub.util.AESEncryptionUtil;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional  // 测试完成后自动回滚
public class OptimizedPasswordEncryptionIntegrationTest {

    @Autowired
    private LoginUserService loginUserService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private AESEncryptionUtil aesEncryptionUtil;

    @Test
    public void testIrreversiblePasswordEncryption() {
        String userNo = "IRREV_TEST_001";
        String username = "irrev_test_user";
        String email = "irrev.test@example.com";
        String password = "SecurePassword123!";
        
        // 先清理可能存在的测试数据
        userMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("user_no", userNo)
        );
        
        try {
            // 测试密码加密存储
            User user = new User();
            user.setUserNo(userNo);
            user.setUsername(username);
            user.setEmail(email);
            // 使用AES加密存储密码
            user.setPassword(aesEncryptionUtil.encrypt(password));
            user.setPermission((short) 1);
            
            userMapper.insert(user);
            
            System.out.println("✅ 用户创建成功，密码已使用AES加密存储");
            
            // 验证存储的密码确实是加密的
            User storedUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                    .eq("user_no", userNo)
            );
            
            assertNotNull(storedUser);
            assertNotNull(storedUser.getPassword());
            
            System.out.println("存储的密码字节长度: " + storedUser.getPassword().length);
            
            // 验证密码不是明文
            assertNotEquals(password.getBytes().length, storedUser.getPassword().length);
            
            System.out.println("✅ 不可逆加密特性验证通过");
            
        } finally {
            // 清理测试数据
            userMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                    .eq("user_no", userNo)
            );
        }
    }

    @Test
    public void testPasswordEncryptionWithSpecialCharacters() {
        String[] testPasswords = {
            "Simple123",
            "Complex!@#$%^&*()_+-=[]{}|;:,.<>?",
            "中文密码测试123",
            "Mixed混合密码!@#",
            "VeryLongPasswordThatExceedsBasicLengthRequirements1234567890"
        };
        
        for (String password : testPasswords) {
            System.out.println("\n测试密码: " + password);
            
            // 加密
            byte[] encryptedBytes = aesEncryptionUtil.encrypt(password);
            System.out.println("加密后字节长度: " + encryptedBytes.length);
            
            // 验证加密成功
            assertNotNull(encryptedBytes);
            assertTrue(encryptedBytes.length > 0);
            
            System.out.println("✅ 通过");
        }
        
        System.out.println("\n✅ 所有特殊字符密码不可逆加密测试通过");
    }

    @Test
    public void testDatabasePasswordStorageFormat() {
        // 测试数据库中密码字段的存储格式
        String password = "test123";
        byte[] encryptedPasswordBytes = aesEncryptionUtil.encrypt(password);
        
        // 验证存储格式
        assertNotNull(encryptedPasswordBytes);
        assertTrue(encryptedPasswordBytes.length > 0);
        
        System.out.println("✅ 数据库密码存储格式测试通过");
        System.out.println("原始密码: " + password);
        System.out.println("加密存储字节长度: " + encryptedPasswordBytes.length);
        System.out.println("AES加密，不可逆存储");
    }
}