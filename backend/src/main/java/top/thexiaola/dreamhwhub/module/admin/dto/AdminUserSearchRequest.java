package top.thexiaola.dreamhwhub.module.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 平台管理员高级搜索用户的条件
 * 采用与 CNKI 高级检索一致的表达方式：可添加多行条件，每行指定字段、匹配方式与内容，
 * 行间用「并且」或「或者」连接；连续的「并且」归为一组，组间用「或者」。
 */
@Data
public class AdminUserSearchRequest {

    /**
     * 搜索条件行；为空表示不加任何筛选
     */
    @Valid
    @Size(max = 10, message = "最多支持 10 个检索条件")
    private List<AdminUserSearchCondition> conditions = new ArrayList<>();
}
