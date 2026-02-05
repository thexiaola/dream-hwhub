package top.thexiaola.dreamhwhub.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DatabaseAutoCreateTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testTablesExist() {
        // 测试用户表是否存在
        boolean userTableExists = tableExists("user");
        assertTrue(userTableExists, "用户表应该存在");

        // 测试邀请码表是否存在
        boolean invitationTableExists = tableExists("invitation_code");
        assertTrue(invitationTableExists, "邀请码表应该存在");
    }

    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}