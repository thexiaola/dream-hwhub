package top.thexiaola.dreamhwhub.support.sql;

/**
 * SQL LIKE 通配符转义工具
 * <p>
 * 转义后的值需配合 SQL 中的 {@code ESCAPE '!'} 使用，避免用户输入的
 * {@code %}、{@code _}、{@code !} 被当作通配符。
 */
public final class SqlLikeUtil {

    private SqlLikeUtil() {
    }

    /**
     * 转义值中的 LIKE 通配符
     */
    public static String escape(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * 转义并补上前后模糊匹配的通配符，得到可直接用于 {@code LIKE} 的模式串
     */
    public static String containsPattern(String value) {
        return "%" + escape(value) + "%";
    }
}
