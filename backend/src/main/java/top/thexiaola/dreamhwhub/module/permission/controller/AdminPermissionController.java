package top.thexiaola.dreamhwhub.module.permission.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.admin.dto.AssignNodesRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.permission.annotation.RequirePermission;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionRegistry;
import top.thexiaola.dreamhwhub.module.permission.dto.PermissionGroupSaveRequest;
import top.thexiaola.dreamhwhub.module.permission.service.PermissionService;
import top.thexiaola.dreamhwhub.module.permission.vo.PermissionGroupVO;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.util.List;

/**
 * 管理员权限管理控制器
 * <p>
 * 权限节点由代码内置注册表（{@link PermissionRegistry}）定义，本控制器仅管理权限组与绑定关系。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final PermissionService permissionService;

    /**
     * 查询全部权限节点（按模块分组，供前端渲染勾选树）
     */
    @GetMapping("/nodes")
    @RequirePermission(PermissionNodes.PERMISSION_VIEW)
    public ApiResponse<List<PermissionRegistry.Group>> listNodes() {
        User currentUser = UserUtils.getCurrentUser();
        log.info("User {} queried permission nodes", LogUtil.getUserInfo(currentUser));
        return ApiResponse.success(PermissionRegistry.GROUPS);
    }

    /**
     * 查询全部权限组（含所含节点与组内用户数）
     */
    @GetMapping("/groups")
    @RequirePermission(PermissionNodes.PERMISSION_VIEW)
    public ApiResponse<List<PermissionGroupVO>> listGroups() {
        User currentUser = UserUtils.getCurrentUser();
        List<PermissionGroupVO> groups = permissionService.listGroupVOs();
        log.info("User {} queried {} permission groups", LogUtil.getUserInfo(currentUser), groups.size());
        return ApiResponse.success(groups);
    }

    /**
     * 创建权限组
     */
    @PostMapping("/groups")
    @RequirePermission(PermissionNodes.PERMISSION_GROUP_ADD)
    public ApiResponse<PermissionGroupVO> createGroup(@Valid @RequestBody PermissionGroupSaveRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        var created = permissionService.createGroup(request.getCode(), request.getName(),
                request.getDescription(), request.getIsDefault());
        log.info("User {} created permission group {}", LogUtil.getUserInfo(currentUser), created.getCode());
        return ApiResponse.success(permissionService.getGroupVO(created.getId()), "权限组创建成功");
    }

    /**
     * 编辑权限组（标识与所含节点不在此接口修改）
     */
    @PutMapping("/groups/{groupId}")
    @RequirePermission(PermissionNodes.PERMISSION_GROUP_EDIT)
    public ApiResponse<PermissionGroupVO> updateGroup(@PathVariable(value = "groupId") Integer groupId,
            @Valid @RequestBody PermissionGroupSaveRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        permissionService.updateGroup(groupId, request.getName(), request.getDescription(), request.getIsDefault());
        log.info("User {} updated permission group {}", LogUtil.getUserInfo(currentUser), groupId);
        return ApiResponse.success(permissionService.getGroupVO(groupId), "权限组更新成功");
    }

    /**
     * 删除权限组
     */
    @DeleteMapping("/groups/{groupId}")
    @RequirePermission(PermissionNodes.PERMISSION_GROUP_DELETE)
    public ApiResponse<Void> deleteGroup(@PathVariable(value = "groupId") Integer groupId) {
        User currentUser = UserUtils.getCurrentUser();
        permissionService.deleteGroup(groupId);
        log.info("User {} deleted permission group {}", LogUtil.getUserInfo(currentUser), groupId);
        return ApiResponse.success(null, "权限组已删除");
    }

    /**
     * 覆盖设置权限组拥有的权限节点
     */
    @PutMapping("/groups/{groupId}/nodes")
    @RequirePermission(PermissionNodes.PERMISSION_GROUP_EDIT)
    public ApiResponse<PermissionGroupVO> setGroupNodes(@PathVariable(value = "groupId") Integer groupId,
            @RequestBody AssignNodesRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        permissionService.setGroupNodes(groupId, request.getNodes());
        log.info("User {} set nodes of permission group {} to {}",
                LogUtil.getUserInfo(currentUser), groupId, request.getNodes());
        return ApiResponse.success(permissionService.getGroupVO(groupId), "权限节点已更新");
    }
}
