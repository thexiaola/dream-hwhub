package top.thexiaola.dreamhwhub.module.admin.dto;

import lombok.Data;

import java.util.List;

/**
 * 为用户分配权限组请求（覆盖式：传入即为最终归属）
 */
@Data
public class AssignGroupsRequest {

    // 权限组 ID 列表
    private List<Integer> groupIds;
}
