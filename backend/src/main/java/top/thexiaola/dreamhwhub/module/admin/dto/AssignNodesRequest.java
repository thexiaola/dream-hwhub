package top.thexiaola.dreamhwhub.module.admin.dto;

import lombok.Data;

import java.util.List;

/**
 * 为用户直接授予权限节点请求（覆盖式：传入即为最终节点集合）
 */
@Data
public class AssignNodesRequest {

    // 权限节点列表
    private List<String> nodes;
}
