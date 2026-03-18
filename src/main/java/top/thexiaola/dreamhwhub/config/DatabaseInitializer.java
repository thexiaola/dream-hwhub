package top.thexiaola.dreamhwhub.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 数据库初始化器
 * 服务启动时自动检查并创建必要的数据库表
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 应用启动后执行数据库初始化
    @PostConstruct
    public void initializeDatabase() {
        try {
            log.info("Starting database initialization...");
            
            // 检查user表是否存在
            boolean tableExists = checkTableExists("user");
            
            if (!tableExists) {
                log.info("User table not found, creating tables from schema.sql...");
                executeSchemaSql();
                log.info("Database initialization completed successfully.");
            } else {
                log.info("User table already exists, skipping initialization.");
            }
            
        } catch (Exception e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查指定表是否存在
     * @param tableName 表名
     * @return 是否存在
     */
    private boolean checkTableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Failed to check table existence: {}", e.getMessage());
            return false;
        }
    }

    // 执行schema.sql脚本
    private void executeSchemaSql() {
        try {
            ClassPathResource resource = new ClassPathResource("schema.sql");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String sqlScript = reader.lines()
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("--") && !line.trim().startsWith("USE "))
                    .collect(Collectors.joining("\n"));
                
                // 清理SQL脚本中的多余逗号
                sqlScript = sqlScript.replaceAll(",\\s*\\)", ")");
                
                // 分割SQL语句并执行
                String[] statements = sqlScript.split(";");
                for (String statement : statements) {
                    String trimmedStatement = statement.trim();
                    if (!trimmedStatement.isEmpty()) {
                        try {
                            jdbcTemplate.execute(trimmedStatement);
                        } catch (Exception e) {
                            // 忽略已存在的表错误
                            if (!e.getMessage().contains("already exists")) {
                                log.warn("Failed to execute statement: {} - Error: {}", trimmedStatement, e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute schema.sql: {}", e.getMessage(), e);
            throw new RuntimeException("Database schema initialization failed", e);
        }
    }
}