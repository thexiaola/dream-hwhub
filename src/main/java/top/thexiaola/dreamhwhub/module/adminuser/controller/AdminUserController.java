package top.thexiaola.dreamhwhub.module.adminuser.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import top.thexiaola.dreamhwhub.shared.ApiResponse;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.adminuser.dto.AdminUserRequest;
import top.thexiaola.dreamhwhub.dto.PageResult;
import top.thexiaola.dreamhwhub.dto.UserQueryRequest;
import top.thexiaola.dreamhwhub.module.adminuser.service.AdminUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);
    
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 管理员创建用户
     */
    @PostMapping
    public ApiResponse<User> createUser(@RequestBody AdminUserRequest adminUserRequest,
                                       @RequestAttribute("currentUser") User currentUser,
                                       HttpServletRequest request) {
        LogUtil.info(logger, "收到管理员创建用户请求", currentUser, request);
        
        try {
            // 将AdminUserRequest转换为User对象
            User user = new User();
            user.setUserNo(adminUserRequest.getUserNo());
            user.setUsername(adminUserRequest.getUsername());
            user.setEmail(adminUserRequest.getEmail());
            user.setPassword(adminUserRequest.getPassword());
            user.setPermission(adminUserRequest.getPermission());
            
            User createdUser = adminUserService.adminCreateUser(currentUser, user);
            LogUtil.info(logger, "管理员创建用户成功", currentUser, request);
            return ApiResponse.success(createdUser, "用户创建成功");
        } catch (Exception e) {
            LogUtil.error(logger, "管理员创建用户失败: " + e.getMessage(), currentUser, request);
            return ApiResponse.error(500, "创建用户失败: " + e.getMessage());
        }
    }

    /**
     * 管理员更新用户
     */
    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Integer id,
                                       @RequestBody AdminUserRequest adminUserRequest,
                                       @RequestAttribute("currentUser") User currentUser,
                                       HttpServletRequest request) {
        LogUtil.info(logger, "收到管理员更新用户请求，用户ID: " + id, currentUser, request);
        
        try {
            // 将AdminUserRequest转换为User对象
            User user = new User();
            user.setUserNo(adminUserRequest.getUserNo());
            user.setUsername(adminUserRequest.getUsername());
            user.setEmail(adminUserRequest.getEmail());
            user.setPassword(adminUserRequest.getPassword());
            user.setPermission(adminUserRequest.getPermission());
            
            User updatedUser = adminUserService.adminUpdateUser(currentUser, id, user);
            LogUtil.info(logger, "管理员更新用户成功", currentUser, request);
            return ApiResponse.success(updatedUser, "用户更新成功");
        } catch (Exception e) {
            LogUtil.error(logger, "管理员更新用户失败: " + e.getMessage(), currentUser, request);
            return ApiResponse.error(500, "更新用户失败: " + e.getMessage());
        }
    }

    /**
     * 管理员删除用户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Integer id,
                                         @RequestAttribute("currentUser") User currentUser,
                                         HttpServletRequest request) {
        LogUtil.info(logger, "收到管理员删除用户请求，用户ID: " + id, currentUser, request);
        
        try {
            boolean result = adminUserService.adminDeleteUser(currentUser, id);
            if (result) {
                LogUtil.info(logger, "管理员删除用户成功", currentUser, request);
                return ApiResponse.success(null, "用户删除成功");
            } else {
                LogUtil.warn(logger, "管理员删除用户失败", currentUser, request);
                return ApiResponse.error(500, "删除用户失败");
            }
        } catch (Exception e) {
            LogUtil.error(logger, "管理员删除用户失败: " + e.getMessage(), currentUser, request);
            return ApiResponse.error(500, "删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 管理员查询用户列表（支持分页和搜索）
     */
    @PostMapping("/list")
    public ApiResponse<PageResult<User>> listUsers(@RequestBody UserQueryRequest queryRequest,
                                                  @RequestAttribute("currentUser") User currentUser,
                                                  HttpServletRequest request) {
        LogUtil.info(logger, "收到管理员查询用户列表请求", currentUser, request);
        
        try {
            // 设置默认分页参数
            if (queryRequest.getPage() == null || queryRequest.getPage() < 1) {
                queryRequest.setPage(1);
            }
            if (queryRequest.getSize() == null || queryRequest.getSize() < 1) {
                queryRequest.setSize(30);
            }
            if (queryRequest.getSize() > 100) {
                queryRequest.setSize(100); // 限制最大每页100条
            }
            
            PageResult<User> pageResult = adminUserService.adminListUsers(currentUser, queryRequest);
            LogUtil.info(logger, "管理员查询用户列表成功，总记录数: " + pageResult.getTotal(), currentUser, request);
            return ApiResponse.success(pageResult, "查询成功");
        } catch (Exception e) {
            LogUtil.error(logger, "管理员查询用户列表失败: " + e.getMessage(), currentUser, request);
            return ApiResponse.error(500, "查询用户列表失败: " + e.getMessage());
        }
    }
}