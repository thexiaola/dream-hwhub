package top.thexiaola.dreamhwhub.module.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 权限组新增/编辑请求
 */
@Data
public class PermissionGroupSaveRequest {

    // 权限组标识（仅新增时使用，创建后不可修改）
    @Size(max = 64, message = "权限组标识长度不能超过 64 位")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$",
            message = "权限组标识只能由字母、数字、下划线、连字符组成，且以字母开头")
    private String code;

    // 权限组名称
    @NotBlank(message = "权限组名称不能为空")
    @Size(max = 64, message = "权限组名称长度不能超过 64 位")
    private String name;

    // 权限组描述
    @Size(max = 255, message = "权限组描述长度不能超过 255 位")
    private String description;

    // 是否为新用户默认加入
    private Boolean isDefault;
}
