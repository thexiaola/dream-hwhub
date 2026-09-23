package top.thexiaola.dreamhwhub.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户高级搜索中的单个条件
 */
@Data
public class AdminUserSearchCondition {

    /**
     * 搜索字段：username-用户名，email-邮箱，school-学校，staffNo-学工号，realName-姓名，className-班级
     */
    @NotBlank(message = "请选择搜索字段")
    @Pattern(regexp = "^(username|email|school|staffNo|realName|className)$", message = "不支持的搜索字段")
    private String field;

    /**
     * 匹配方式：contains-模糊（包含即命中），equals-精确（完全相等）；默认 contains
     */
    @Pattern(regexp = "^(contains|equals)$", message = "匹配方式只能是 contains 或 equals")
    private String matchType = "contains";

    /**
     * 搜索内容
     */
    @NotBlank(message = "请输入搜索内容")
    @Size(max = 100, message = "搜索内容长度不能超过 100 位")
    private String value;

    /**
     * 与上一个条件的连接方式：and-并且（默认），or-或者；第一个条件忽略该值
     */
    @Pattern(regexp = "^(and|or)$", message = "连接方式只能是 and 或 or")
    private String connector = "and";

    /**
     * 是否为「或者」连接
     */
    public boolean isOrConnector() {
        return "or".equalsIgnoreCase(connector);
    }

    /**
     * 是否为精确匹配
     */
    public boolean isEqualsMatch() {
        return "equals".equalsIgnoreCase(matchType);
    }
}
