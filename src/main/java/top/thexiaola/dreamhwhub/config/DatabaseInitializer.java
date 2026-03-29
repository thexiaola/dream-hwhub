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
import java.util.List;
import java.util.Set;
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

    // 应用启动后执行数据库初始化和字段校验
    @PostConstruct
    public void initializeDatabase() {
        try {
            log.info("Starting database initialization and schema validation...");

            // 检查 user 表是否存在
            boolean tableExists = checkTableExists("user");

            if (!tableExists) {
                log.info("User table not found, creating tables from user_schema.sql...");
                executeSchemaSql();
                log.info("User table created successfully.");
            } else {
                log.info("User table exists, validating columns...");
                validateAndSyncUserTableFromSchema();
            }

            // 检查 work_info 表是否存在（判断作业管理模块是否需要初始化）
            boolean workTableExists = checkTableExists("work_info");
            if (!workTableExists) {
                log.info("Work table not found, creating tables from work_management.sql...");
                executeWorkManagementSql();
                log.info("Work management tables created successfully.");
            } else {
                log.info("Work management tables exist, validating columns...");
                validateAndSyncWorkTablesFromSchema();
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

    /**
     * 获取表的实际列名
     */
    private Set<String> getActualColumns(String tableName) {
        Set<String> columns = new java.util.HashSet<>();
        try {
            String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = ?";
            List<String> result = jdbcTemplate.queryForList(sql, String.class, tableName);
            columns.addAll(result);
        } catch (Exception e) {
            log.warn("Failed to get columns from table {}: {}", tableName, e.getMessage());
        }
        return columns;
    }

    /**
     * 从 user_schema.sql 动态解析并同步 user 表字段
     */
    private void validateAndSyncUserTableFromSchema() {
        try {
            // 从 user_schema.sql 解析期望的字段定义
            List<ColumnDefinition> expectedColumns = parseSchemaSql("user_schema.sql", "user");

            if (expectedColumns.isEmpty()) {
                log.warn("No column definitions found in user_schema.sql for user table");
                return;
            }

            Set<String> actualColumns = getActualColumns("user");

            boolean needAlter = false;
            StringBuilder alterSql = new StringBuilder();

            // 检查缺少的字段
            for (ColumnDefinition expected : expectedColumns) {
                if (!actualColumns.contains(expected.columnName)) {
                    log.warn("Missing column '{}' in user table, will add it", expected.columnName);
                    needAlter = true;
                    alterSql.append(generateAddColumnSql("user", expected)).append(";");
                }
            }

            // 如果有缺失字段，执行 ALTER TABLE
            if (needAlter) {
                log.info("Executing ALTER TABLE to sync user table structure...");
                try {
                    String[] statements = alterSql.toString().split(";");
                    for (String stmt : statements) {
                        if (!stmt.trim().isEmpty()) {
                            jdbcTemplate.execute(stmt.trim());
                        }
                    }
                    log.info("User table structure synced successfully!");
                } catch (Exception e) {
                    log.error("Failed to sync user table structure: {}", e.getMessage(), e);
                }
            } else {
                log.info("User table structure is up-to-date. All {} columns present.", actualColumns.size());
            }
        } catch (Exception e) {
            log.error("Failed to parse user_schema.sql: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 work_management.sql 动态解析并同步 work 和 work_submission 表字段
     */
    private void validateAndSyncWorkTablesFromSchema() {
        try {
            // 同步 work_info 表
            validateAndSyncWorkTable("work_info");
            // 同步 work_submission 表
            validateAndSyncWorkTable("work_submission");
        } catch (Exception e) {
            log.error("Failed to sync work management tables: {}", e.getMessage(), e);
        }
    }

    /**
     * 从 work_management.sql 解析并同步指定表的字段
     */
    private void validateAndSyncWorkTable(String tableName) {
        try {
            // 从 work_management.sql 解析期望的字段定义
            List<ColumnDefinition> expectedColumns = parseSchemaSql("work_management.sql", tableName);

            if (expectedColumns.isEmpty()) {
                log.warn("No column definitions found in work_management.sql for table '{}'", tableName);
                return;
            }

            Set<String> actualColumns = getActualColumns(tableName);

            boolean needAlter = false;
            StringBuilder alterSql = new StringBuilder();

            // 检查缺少的字段
            for (ColumnDefinition expected : expectedColumns) {
                if (!actualColumns.contains(expected.columnName)) {
                    log.warn("Missing column '{}' in {} table, will add it", expected.columnName, tableName);
                    needAlter = true;
                    alterSql.append(generateAddColumnSql(tableName, expected)).append(";");
                }
            }

            // 如果有缺失字段，执行 ALTER TABLE
            if (needAlter) {
                log.info("Executing ALTER TABLE to sync {} table structure...", tableName);
                try {
                    String[] statements = alterSql.toString().split(";");
                    for (String stmt : statements) {
                        if (!stmt.trim().isEmpty()) {
                            jdbcTemplate.execute(stmt.trim());
                        }
                    }
                    log.info("{} table structure synced successfully!", tableName);
                } catch (Exception e) {
                    log.error("Failed to sync {} table structure: {}", tableName, e.getMessage(), e);
                }
            } else {
                log.info("{} table structure is up-to-date. All {} columns present.", tableName, actualColumns.size());
            }
        } catch (Exception e) {
            log.error("Failed to parse work_management.sql for table {}: {}", tableName, e.getMessage(), e);
        }
    }

    /**
     * 从 SQL 脚本中解析指定表的字段定义
     */
    private List<ColumnDefinition> parseSchemaSql(String resourceName, String tableName) {
        try {
            ClassPathResource resource = new ClassPathResource(resourceName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String sqlScript = reader.lines()
                    .collect(Collectors.joining("\n"));

                // 提取 CREATE TABLE 语句
                String createTableSql = extractCreateTableSql(sqlScript, tableName);
                if (createTableSql == null || createTableSql.isEmpty()) {
                    log.warn("CREATE TABLE statement for '{}' not found in {}", tableName, resourceName);
                    return List.of();
                }

                // 解析字段定义
                return parseColumnDefinitions(createTableSql);
            }
        } catch (Exception e) {
            log.error("Error parsing {}: {}", resourceName, e.getMessage());
            return List.of();
        }
    }

    /**
     * 从 SQL 脚本中提取指定表的 CREATE TABLE 语句
     */
    private String extractCreateTableSql(String sqlScript, String tableName) {
        // 使用正则表达式匹配 CREATE TABLE IF NOT EXISTS `work_info` (...) 语句
        // 支持带反引号和不带反引号的表名
        String regex = "CREATE TABLE\\s+(?:IF NOT EXISTS\\s+)?(?:`)?" + tableName + "(?:`)?\\s*\\(([^;]+)\\)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(sqlScript);

        if (matcher.find()) {
            return matcher.group(1); // 返回括号内的内容
        }
        return null;
    }

    /**
     * 解析 CREATE TABLE 语句中的字段定义
     */
    private List<ColumnDefinition> parseColumnDefinitions(String createTableBody) {
        List<ColumnDefinition> columns = new java.util.ArrayList<>();

        // 按逗号分割，但要考虑括号内的逗号（如 VARCHAR(100)）
        String[] parts = splitColumnDefinitions(createTableBody);

        for (String part : parts) {
            String trimmedPart = part.trim();

            // 跳过 PRIMARY KEY、UNIQUE INDEX、CONSTRAINT、FOREIGN KEY 等非字段定义
            if (trimmedPart.toUpperCase().startsWith("PRIMARY KEY") ||
                trimmedPart.toUpperCase().startsWith("UNIQUE") ||
                trimmedPart.toUpperCase().startsWith("INDEX") ||
                trimmedPart.toUpperCase().startsWith("KEY") ||
                trimmedPart.toUpperCase().startsWith("CONSTRAINT") ||
                trimmedPart.toUpperCase().startsWith("FOREIGN KEY")) {
                continue;
            }

            // 解析字段名和类型定义
            ColumnDefinition columnDef = parseColumnDefinition(trimmedPart);
            if (columnDef != null) {
                columns.add(columnDef);
            }
        }

        return columns;
    }

    /**
     * 分割字段定义（处理括号内的逗号）
     */
    private String[] splitColumnDefinitions(String sql) {
        List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesesCount = 0;

        for (char c : sql.toCharArray()) {
            if (c == '(') {
                parenthesesCount++;
            } else if (c == ')') {
                parenthesesCount--;
            } else if (c == ',' && parenthesesCount == 0) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }

        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }

        return result.toArray(new String[0]);
    }

    /**
     * 解析单个字段定义
     */
    private ColumnDefinition parseColumnDefinition(String columnDef) {
        // 匹配字段名和类型：column_name TYPE(...) [NOT NULL] [DEFAULT ...] [COMMENT ...]
        String regex = "(`?)(\\w+)\\1\\s+(.+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(columnDef.trim());

        if (matcher.matches()) {
            String columnName = matcher.group(2);
            String fullDefinition = matcher.group(3).trim();
            return new ColumnDefinition(columnName, fullDefinition);
        }

        return null;
    }

    /**
     * 根据解析出的字段定义生成 ADD COLUMN SQL
     */
    private String generateAddColumnSql(String tableName, ColumnDefinition columnDef) {
        return String.format("ALTER TABLE %s ADD COLUMN %s %s",
            tableName, columnDef.columnName, columnDef.fullDefinition);
    }

    /**
     * 字段定义内部类
     */
    private static class ColumnDefinition {
        String columnName;
        String fullDefinition; // 完整的类型定义，包括类型、约束、默认值、注释等

        ColumnDefinition(String columnName, String fullDefinition) {
            this.columnName = columnName;
            this.fullDefinition = fullDefinition;
        }
    }

    /**
     * 执行 SQL 脚本文件
     * @param resourceName SQL 资源文件名
     * @param skipUseStatement 是否跳过 USE 语句
     */
    private void executeSqlScript(String resourceName, boolean skipUseStatement) {
        try {
            ClassPathResource resource = new ClassPathResource(resourceName);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String sqlScript = reader.lines()
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("--")
                        && !(skipUseStatement && line.trim().startsWith("USE ")))
                    .collect(Collectors.joining("\n"));

                // 清理 SQL 脚本中的多余逗号
                sqlScript = sqlScript.replaceAll(",\\s*\\)", ")");

                // 分割 SQL 语句并执行
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
            log.error("Failed to execute {}: {}", resourceName, e.getMessage(), e);
            throw new RuntimeException(resourceName + " initialization failed", e);
        }
    }

    // 执行 user_schema.sql 脚本
    private void executeSchemaSql() {
        executeSqlScript("user_schema.sql", true);
    }

    // 执行 work_management.sql 脚本
    private void executeWorkManagementSql() {
        executeSqlScript("work_management.sql", false);
    }
}