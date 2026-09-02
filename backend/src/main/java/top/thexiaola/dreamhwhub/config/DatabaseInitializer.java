package top.thexiaola.dreamhwhub.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据库初始化器
 * 服务启动时自动同步 classpath 下 SQL 脚本中定义的所有表：
 * 脚本中新增的表自动创建，已存在的表自动补齐缺失字段、清理多余字段，
 * 因此在脚本文件中任意新增表或字段均无需修改本类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;

    // 需要同步的 SQL 脚本文件，新增脚本文件只需在此登记
    private static final String[] SCHEMA_RESOURCES = {"user_schema.sql", "work_management.sql"};

    // CREATE TABLE 语句的表名提取（兼容有无反引号、是否带 IF NOT EXISTS）
    private static final Pattern CREATE_TABLE_NAME_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?\\s*\\(",
            Pattern.CASE_INSENSITIVE);

    // 应用启动后执行数据库初始化和字段校验
    @PostConstruct
    public void initializeDatabase() {
        try {
            // 检查是否为 H2 数据库（测试环境），如果是则跳过初始化
            try (java.sql.Connection connection = jdbcTemplate.getDataSource().getConnection()) {
                String databaseProductName = connection.getMetaData().getDatabaseProductName();
                if ("H2".equalsIgnoreCase(databaseProductName)) {
                    log.info("H2 database detected, skipping MySQL-specific database initialization...");
                    return;
                }
            }

            log.info("Starting database initialization and schema validation...");

            for (String resourceName : SCHEMA_RESOURCES) {
                syncSchemaFile(resourceName);
            }
        } catch (Exception e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 同步单个 SQL 脚本文件中定义的所有表
     * 建表/建库语句尽力执行，逐表创建或同步，单表失败不影响其他表
     *
     * @param resourceName classpath 下的脚本文件名
     */
    private void syncSchemaFile(String resourceName) {
        List<String> statements;
        try {
            statements = splitSqlStatements(readResourceAsString(resourceName));
        } catch (Exception e) {
            log.error("Failed to read schema file {}: {}", resourceName, e.getMessage(), e);
            return;
        }

        for (String statement : statements) {
            String upper = statement.toUpperCase();
            if (upper.startsWith("CREATE DATABASE") || upper.startsWith("CREATE SCHEMA")) {
                // 建库语句尽力执行，失败（如已存在）不影响后续流程
                try {
                    jdbcTemplate.execute(statement);
                } catch (Exception e) {
                    log.warn("Skipped statement in {}: {}", resourceName, e.getMessage());
                }
            } else if (upper.startsWith("USE ")) {
                // USE 语句仅作用于单个连接，默认库由数据源配置决定
                continue;
            } else if (upper.startsWith("CREATE TABLE")) {
                syncTable(resourceName, statement);
            }
        }
    }

    /**
     * 同步单张表：不存在则按脚本建表，存在则同步字段
     */
    private void syncTable(String resourceName, String createTableStatement) {
        String tableName = extractTableName(createTableStatement);
        if (tableName == null || isInvalidTableName(tableName)) {
            log.warn("Invalid or unparsable table definition skipped in {}: {}",
                    resourceName, StrUtil.subPre(createTableStatement, 80));
            return;
        }

        if (!checkTableExists(tableName)) {
            try {
                jdbcTemplate.execute(createTableStatement);
                log.info("Table '{}' not found, created it from {}.", tableName, resourceName);
            } catch (Exception e) {
                log.error("Failed to create table '{}': {}", tableName, e.getMessage(), e);
            }
            return;
        }

        syncTableColumns(tableName, createTableStatement);
    }

    /**
     * 对已存在的表按脚本中的字段定义进行同步：补齐缺失字段、清理多余字段
     */
    private void syncTableColumns(String tableName, String createTableStatement) {
        String tableBody = extractCreateTableBody(createTableStatement);
        if (StrUtil.isBlank(tableBody)) {
            log.warn("Failed to extract table body for '{}', skipping column sync", tableName);
            return;
        }

        List<ColumnDefinition> expectedColumns = parseColumnDefinitions(tableBody);
        if (expectedColumns.isEmpty()) {
            log.warn("No column definitions parsed for table '{}', skipping column sync", tableName);
            return;
        }

        Set<String> actualColumns = getActualColumns(tableName);
        Set<String> expectedColumnNames = new HashSet<>();
        for (ColumnDefinition column : expectedColumns) {
            expectedColumnNames.add(column.columnName.toLowerCase());
        }

        boolean needAlter = false;
        List<String> alterStatements = new ArrayList<>();

        // 补齐缺失字段
        for (ColumnDefinition expected : expectedColumns) {
            if (isInvalidColumnName(expected.columnName)) {
                log.warn("Invalid column name skipped: {}", expected.columnName);
                continue;
            }
            if (!actualColumns.contains(expected.columnName.toLowerCase())) {
                log.warn("Missing column '{}' in table '{}', will add it", expected.columnName, tableName);
                needAlter = true;
                alterStatements.add(generateAddColumnSql(tableName, expected));
            }
        }

        // 清理多余字段（系统保留列除外）
        for (String actualColumn : actualColumns) {
            if (!expectedColumnNames.contains(actualColumn) && !isSystemColumn(actualColumn)) {
                log.warn("Extra column '{}' in table '{}', will drop it", actualColumn, tableName);
                needAlter = true;
                alterStatements.add(String.format("ALTER TABLE `%s` DROP COLUMN `%s`", tableName, actualColumn));
            }
        }

        if (!needAlter) {
            log.info("Table '{}' structure is up-to-date. All {} columns present.", tableName, actualColumns.size());
            return;
        }

        log.info("Executing ALTER TABLE to sync '{}' table structure...", tableName);
        // 单条失败不影响后续语句
        for (String statement : alterStatements) {
            try {
                jdbcTemplate.execute(statement);
            } catch (Exception e) {
                log.error("Failed to execute statement '{}': {}", statement, e.getMessage());
            }
        }
        log.info("Table '{}' structure synced successfully!", tableName);
    }

    /**
     * 检查指定表是否存在
     *
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
     * 获取表的实际列名（统一转为小写便于比较）
     */
    private Set<String> getActualColumns(String tableName) {
        Set<String> columns = new HashSet<>();
        try {
            String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = ?";
            List<String> result = jdbcTemplate.queryForList(sql, String.class, tableName);
            for (String column : result) {
                columns.add(column.toLowerCase());
            }
        } catch (Exception e) {
            log.warn("Failed to get columns from table {}: {}", tableName, e.getMessage());
        }
        return columns;
    }

    /**
     * 读取 classpath 资源文件内容
     */
    private String readResourceAsString(String resourceName) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourceName);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 将 SQL 脚本拆分为独立语句
     * 自动剥离注释，正确处理引号字符串（含反斜杠与双写引号转义）与括号嵌套，
     * 避免注释或字符串中的分号、括号导致误切割
     */
    private List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        char inQuote = 0;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);

            // 字符串内部：整段原样保留
            if (inQuote != 0) {
                current.append(c);
                if (c == '\\' && (inQuote == '\'' || inQuote == '"') && i + 1 < script.length()) {
                    current.append(script.charAt(++i));
                } else if (c == inQuote) {
                    if (i + 1 < script.length() && script.charAt(i + 1) == inQuote) {
                        current.append(script.charAt(++i));
                    } else {
                        inQuote = 0;
                    }
                }
                continue;
            }

            // 行注释：-- 后跟空白或行尾
            if (c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-'
                    && (i + 2 >= script.length() || Character.isWhitespace(script.charAt(i + 2)))) {
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            // 行注释：#
            if (c == '#') {
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            // 块注释
            if (c == '/' && i + 1 < script.length() && script.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < script.length()
                        && !(script.charAt(i) == '*' && script.charAt(i + 1) == '/')) {
                    i++;
                }
                i++;
                continue;
            }
            // 引号开始
            if (c == '\'' || c == '"' || c == '`') {
                inQuote = c;
                current.append(c);
                continue;
            }
            // 括号嵌套
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
            } else if (c == ';' && parenDepth == 0) {
                // 语句边界
                String statement = current.toString().trim();
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
                continue;
            }
            current.append(c);
        }

        String last = current.toString().trim();
        if (!last.isEmpty()) {
            statements.add(last);
        }
        return statements;
    }

    /**
     * 从 CREATE TABLE 语句中提取表名
     */
    private String extractTableName(String createTableStatement) {
        Matcher matcher = CREATE_TABLE_NAME_PATTERN.matcher(createTableStatement);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 从 CREATE TABLE 语句中提取括号内的表体（含字段、索引定义）
     * 通过引号感知的括号配对定位，兼容 COMMENT 字符串中出现括号的情况
     */
    private String extractCreateTableBody(String createTableStatement) {
        int start = createTableStatement.indexOf('(');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        char inQuote = 0;

        for (int i = start; i < createTableStatement.length(); i++) {
            char c = createTableStatement.charAt(i);
            if (inQuote != 0) {
                if (c == '\\' && (inQuote == '\'' || inQuote == '"') && i + 1 < createTableStatement.length()) {
                    i++;
                } else if (c == inQuote) {
                    if (i + 1 < createTableStatement.length()
                            && createTableStatement.charAt(i + 1) == inQuote) {
                        i++;
                    } else {
                        inQuote = 0;
                    }
                }
                continue;
            }
            if (c == '\'' || c == '"' || c == '`') {
                inQuote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return createTableStatement.substring(start + 1, i);
                }
            }
        }
        return null;
    }

    /**
     * 解析 CREATE TABLE 表体中的字段定义
     */
    private List<ColumnDefinition> parseColumnDefinitions(String createTableBody) {
        List<ColumnDefinition> columns = new ArrayList<>();

        // 按逗号分割，跳过括号内与字符串内的逗号
        String[] parts = splitColumnDefinitions(createTableBody);

        for (String part : parts) {
            String trimmedPart = part.trim();

            // 跳过主键、唯一键、索引、约束等非字段定义
            String upperPart = trimmedPart.toUpperCase();
            if (upperPart.startsWith("PRIMARY KEY") ||
                    upperPart.startsWith("UNIQUE") ||
                    upperPart.startsWith("INDEX") ||
                    upperPart.startsWith("KEY") ||
                    upperPart.startsWith("CONSTRAINT") ||
                    upperPart.startsWith("FOREIGN KEY") ||
                    upperPart.startsWith("FULLTEXT") ||
                    upperPart.startsWith("SPATIAL") ||
                    upperPart.startsWith("CHECK")) {
                continue;
            }

            ColumnDefinition columnDef = parseColumnDefinition(trimmedPart);
            if (columnDef != null) {
                columns.add(columnDef);
            }
        }

        return columns;
    }

    /**
     * 分割字段定义（处理括号内的逗号与引号字符串内的逗号）
     */
    private String[] splitColumnDefinitions(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesesCount = 0;
        char inQuote = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inQuote != 0) {
                current.append(c);
                if (c == '\\' && (inQuote == '\'' || inQuote == '"') && i + 1 < sql.length()) {
                    current.append(sql.charAt(++i));
                } else if (c == inQuote) {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == inQuote) {
                        current.append(sql.charAt(++i));
                    } else {
                        inQuote = 0;
                    }
                }
                continue;
            }
            if (c == '\'' || c == '"' || c == '`') {
                inQuote = c;
            } else if (c == '(') {
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
        // 匹配字段名和类型定义：column_name TYPE(...) [NOT NULL] [DEFAULT ...] [COMMENT ...]
        String regex = "(`?)(\\w+)\\1\\s+(.+)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(columnDef.trim());

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
        // 表名与列名均已通过白名单验证，直接拼接是安全的
        return String.format("ALTER TABLE `%s` ADD COLUMN `%s` %s",
                tableName, columnDef.columnName, columnDef.fullDefinition);
    }

    /**
     * 验证表名是否合法（仅允许字母、数字、下划线，防止 SQL 注入）
     *
     * @param tableName 待验证的表名
     * @return true-表名非法，false-表名合法
     */
    private boolean isInvalidTableName(String tableName) {
        return tableName == null || !tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");
    }

    /**
     * 验证列名是否非法（包含非法字符）
     *
     * @param columnName 待验证的列名
     * @return true-列名非法，false-列名合法
     */
    private boolean isInvalidColumnName(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return true;
        }
        // 只允许字母、数字、下划线、连字符，且不能以数字开头
        return !columnName.matches("^[a-zA-Z_][a-zA-Z0-9_-]*$");
    }

    /**
     * 检查是否为系统保留列名（这些列不应被删除）
     *
     * @param columnName 列名
     * @return true-是系统列，false-不是系统列
     */
    private boolean isSystemColumn(String columnName) {
        // 常见的系统/保留列名
        String[] systemColumns = {
                "id", "created_at", "updated_at", "created_by", "updated_by",
                "deleted", "version", "tenant_id", "org_id", "dept_id"
        };
        for (String sys : systemColumns) {
            if (sys.equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
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
}
