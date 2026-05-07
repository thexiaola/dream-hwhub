package top.thexiaola.dreamhwhub.support.validation;

import lombok.extern.slf4j.Slf4j;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;

import java.util.regex.Pattern;

/**
 * XSS攻击防护工具类
 */
@Slf4j
public class XssValidator {

    // XSS攻击常见模式
    private static final Pattern[] XSS_PATTERNS = {
        // Script标签
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        
        // 事件处理器
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        
        // iframe和object标签
        Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // eval和expression
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // URL协议注入
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:", Pattern.CASE_INSENSITIVE),
        
        // HTML实体编码绕过
        Pattern.compile("&#[xX]?[0-9a-fA-F]+;", Pattern.CASE_INSENSITIVE),
        
        // CSS注入
        Pattern.compile("url\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("@import", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 检查字符串是否包含XSS攻击代码
     *
     * @param input 待检查的字符串
     * @throws BusinessException 如果检测到XSS攻击
     */
    public static void validateNoXss(String input, String fieldName) {
        if (input == null || input.isEmpty()) {
            return;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("Detected potential XSS attack in field {}: {}", fieldName, input);
                throw new BusinessException(
                    BusinessErrorCode.PARAMETER_ERROR,
                    fieldName + " 包含非法字符，不允许包含脚本或HTML标签",
                    null
                );
            }
        }
    }

    /**
     * 清理字符串中的潜在XSS代码（可选，用于显示时）
     *
     * @param input 原始字符串
     * @return 清理后的字符串
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        
        // 转义HTML特殊字符
        result = result.replace("&", "&amp;");
        result = result.replace("<", "&lt;");
        result = result.replace(">", "&gt;");
        result = result.replace("\"", "&quot;");
        result = result.replace("'", "&#39;");
        
        return result;
    }
}
