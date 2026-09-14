package top.thexiaola.dreamhwhub.module.permission.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 权限组 VO
 */
@Data
public class PermissionGroupVO {

    // 权限组 ID
    private Integer id;

    // 权限组标识（唯一）
    private String code;

    // 权限组名称
    private String name;

    // 权限组描述
    private String description;

    // 是否为新用户默认加入
    private Boolean isDefault;

    // 该组拥有的权限节点
    private Set<String> nodes;

    // 归属于该组的用户数
    private Long userCount;

    // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
