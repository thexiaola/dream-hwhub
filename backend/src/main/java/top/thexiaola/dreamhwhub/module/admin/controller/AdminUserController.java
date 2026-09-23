package top.thexiaola.dreamhwhub.module.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.module.admin.dto.*;
import top.thexiaola.dreamhwhub.module.admin.service.AdminUserService;
import top.thexiaola.dreamhwhub.module.admin.vo.AdminUserVO;
import top.thexiaola.dreamhwhub.module.admin.vo.UserPermissionDetailVO;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.permission.annotation.RequirePermission;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

/**
 * 管理员用户管理控制器
 * <p>
 * 每个操作对应一个权限节点，由 {@link RequirePermission} 注解声明。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户，支持按用户名、邮箱、学校、学工号、姓名、班级的高级组合检索
     */
    @PostMapping("/search")
    @RequirePermission(PermissionNodes.USER_VIEW)
    public ApiResponse<Page<AdminUserVO>> listUsers(
            @Valid @RequestBody AdminUserSearchRequest searchRequest,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        User currentUser = UserUtils.getCurrentUser();
        Page<AdminUserVO> users = adminUserService.listUsers(searchRequest, pageNum, pageSize);
        log.info("User {} queried {} users, conditionCount={}, page={}",
                LogUtil.getUserInfo(currentUser), users.getTotal(),
                searchRequest.getConditions() == null ? 0 : searchRequest.getConditions().size(), pageNum);
        return ApiResponse.success(users);
    }

    /**
     * 新增用户
     */
    @PostMapping
    @RequirePermission(PermissionNodes.USER_ADD)
    public ApiResponse<AdminUserVO> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        AdminUserVO created = adminUserService.createUser(request);
        log.info("User {} created user {}", LogUtil.getUserInfo(currentUser), created.getUsername());
        return ApiResponse.success(created, "用户创建成功");
    }

    /**
     * 编辑用户
     */
    @PutMapping("/{userId}")
    @RequirePermission(PermissionNodes.USER_EDIT)
    public ApiResponse<AdminUserVO> updateUser(@PathVariable(value = "userId") Integer userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        AdminUserVO updated = adminUserService.updateUser(userId, request);
        log.info("User {} updated user {}", LogUtil.getUserInfo(currentUser), updated.getUsername());
        return ApiResponse.success(updated, "用户信息更新成功");
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    @RequirePermission(PermissionNodes.USER_DELETE)
    public ApiResponse<Void> deleteUser(@PathVariable(value = "userId") Integer userId) {
        User currentUser = UserUtils.getCurrentUser();
        adminUserService.deleteUser(userId);
        log.info("User {} deleted user {}", LogUtil.getUserInfo(currentUser), userId);
        return ApiResponse.success(null, "用户已删除");
    }

    /**
     * 封禁/解封用户
     */
    @PutMapping("/{userId}/ban")
    @RequirePermission(PermissionNodes.USER_BAN)
    public ApiResponse<AdminUserVO> setBanned(@PathVariable(value = "userId") Integer userId,
            @Valid @RequestBody AdminBanUserRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        AdminUserVO updated = adminUserService.setBanned(userId, Boolean.TRUE.equals(request.getBanned()),
                request.getReason());
        log.info("User {} set user {} banned={}", LogUtil.getUserInfo(currentUser), userId, request.getBanned());
        return ApiResponse.success(updated, Boolean.TRUE.equals(request.getBanned()) ? "用户已封禁" : "用户已解封");
    }

    /**
     * 设置/取消用户的平台管理员（OP）身份
     */
    @PutMapping("/{userId}/op")
    @RequirePermission(PermissionNodes.USER_SET_OP)
    public ApiResponse<AdminUserVO> setOp(@PathVariable(value = "userId") Integer userId,
            @Valid @RequestBody AdminSetOpRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        AdminUserVO updated = adminUserService.setOp(userId, Boolean.TRUE.equals(request.getIsOp()));
        log.info("User {} set user {} isOp={}", LogUtil.getUserInfo(currentUser), userId, request.getIsOp());
        return ApiResponse.success(updated,
                Boolean.TRUE.equals(request.getIsOp()) ? "已设为平台管理员" : "已取消平台管理员身份");
    }

    /**
     * 查询用户的权限明细（权限组 + 直接节点 + 生效节点）
     */
    @GetMapping("/{userId}/permissions")
    @RequirePermission({PermissionNodes.USER_VIEW, PermissionNodes.PERMISSION_VIEW})
    public ApiResponse<UserPermissionDetailVO> getPermissionDetail(
            @PathVariable(value = "userId") Integer userId) {
        User currentUser = UserUtils.getCurrentUser();
        UserPermissionDetailVO detail = adminUserService.getPermissionDetail(userId);
        log.info("User {} queried permission detail of user {}", LogUtil.getUserInfo(currentUser), userId);
        return ApiResponse.success(detail);
    }

    /**
     * 覆盖设置用户所属的权限组
     */
    @PutMapping("/{userId}/groups")
    @RequirePermission(PermissionNodes.PERMISSION_GROUP_ASSIGN)
    public ApiResponse<UserPermissionDetailVO> setUserGroups(
            @PathVariable(value = "userId") Integer userId,
            @RequestBody AssignGroupsRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        UserPermissionDetailVO detail = adminUserService.setUserGroups(userId, request.getGroupIds());
        log.info("User {} set permission groups of user {} to {}",
                LogUtil.getUserInfo(currentUser), userId, request.getGroupIds());
        return ApiResponse.success(detail, "权限组已更新");
    }

    /**
     * 覆盖设置用户被直接授予的权限节点
     */
    @PutMapping("/{userId}/nodes")
    @RequirePermission(PermissionNodes.PERMISSION_USER_ASSIGN)
    public ApiResponse<UserPermissionDetailVO> setUserNodes(
            @PathVariable(value = "userId") Integer userId,
            @RequestBody AssignNodesRequest request) {
        User currentUser = UserUtils.getCurrentUser();
        UserPermissionDetailVO detail = adminUserService.setUserNodes(userId, request.getNodes());
        log.info("User {} set direct permission nodes of user {} to {}",
                LogUtil.getUserInfo(currentUser), userId, request.getNodes());
        return ApiResponse.success(detail, "权限节点已更新");
    }
}
