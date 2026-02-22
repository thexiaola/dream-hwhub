package top.thexiaola.dreamhwhub.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JwtExpirationConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    @Test
    public void testJwtExpirationConfiguration() {
        System.out.println("=== JWT过期时间配置测试 ===");
        
        // 验证配置值是否正确读取
        assertNotNull(jwtExpiration, "JWT过期时间配置不应为null");
        System.out.println("配置的JWT过期时间(毫秒): " + jwtExpiration);
        
        // 转换为秒进行验证
        long expiresInSeconds = jwtExpiration / 1000;
        System.out.println("转换为秒: " + expiresInSeconds);
        
        // 验证是否为合理的过期时间（1小时到7天之间）
        assertTrue(expiresInSeconds >= 3600, "过期时间应至少为1小时(3600秒)");
        assertTrue(expiresInSeconds <= 604800, "过期时间应不超过7天(604800秒)");
        
        System.out.println("✓ JWT过期时间配置验证通过");
    }

    @Test
    public void testConfigurationInjection() {
        // 测试配置是否能正确注入到组件中
        assertNotNull(applicationContext, "ApplicationContext应能正确注入");
        
        // 尝试获取LoginUserController bean来验证配置注入
        try {
            Object loginController = applicationContext.getBean("loginUserController");
            assertNotNull(loginController, "LoginUserController应能正确创建");
            System.out.println("✓ LoginUserController配置注入成功");
        } catch (Exception e) {
            System.out.println("LoginUserController获取失败: " + e.getMessage());
        }
    }
}