package top.thexiaola.dreamhwhub.module.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.thexiaola.dreamhwhub.module.admin.dto.AdminCreateUserRequest;
import top.thexiaola.dreamhwhub.module.admin.dto.AdminUpdateUserRequest;
import top.thexiaola.dreamhwhub.module.admin.dto.AdminUserSearchRequest;
import top.thexiaola.dreamhwhub.module.admin.vo.AdminUserVO;
import top.thexiaola.dreamhwhub.module.admin.vo.UserPermissionDetailVO;

import java.util.Collection;
import java.util.List;

/**
 * 管理员用户管理服务接口
 */
public interface AdminUserService {

    /**
     * 分页查询用户
     *
     * @param request  搜索条件（用户名/邮箱/学校/学号/姓名/班级，及和与或的组合方式）
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 用户分页结果
     */
    Page<AdminUserVO> listUsers(AdminUserSearchRequest request, Integer pageNum, Integer pageSize);

    /**
     * 新增用户
     *
     * @param request 新增用户请求
     * @return 新增的用户
     */
    AdminUserVO createUser(AdminCreateUserRequest request);

    /**
     * 编辑用户
     *
     * @param userId  用户 ID
     * @param request 编辑请求（为空字段不修改）
     * @return 更新后的用户
     */
    AdminUserVO updateUser(Integer userId, AdminUpdateUserRequest request);

    /**
     * 修改指定用户的头像
     *
     * @param userId 用户 ID
     * @param file   头像文件
     * @return 更新后的用户
     */
    AdminUserVO updateUserAvatar(Integer userId, org.springframework.web.multipart.MultipartFile file);

    /**
     * 清除指定用户的头像
     *
     * @param userId 用户 ID
     * @return 更新后的用户
     */
    AdminUserVO removeUserAvatar(Integer userId);

    /**
     * 删除用户及其权限、班级成员数据
     *
     * @param userId 用户 ID
     */
    void deleteUser(Integer userId);

    /**
     * 封禁/解封用户
     *
     * @param userId 用户 ID
     * @param banned true-封禁，false-解封
     * @param reason 封禁原因
     * @return 更新后的用户
     */
    AdminUserVO setBanned(Integer userId, boolean banned, String reason);

    /**
     * 设置/取消用户的平台管理员（OP）身份
     *
     * @param userId 用户 ID
     * @param isOp   true-授予，false-取消
     * @return 更新后的用户
     */
    AdminUserVO setOp(Integer userId, boolean isOp);

    /**
     * 查询用户的权限明细
     *
     * @param userId 用户 ID
     * @return 权限明细
     */
    UserPermissionDetailVO getPermissionDetail(Integer userId);

    /**
     * 覆盖设置用户所属的权限组
     *
     * @param userId   用户 ID
     * @param groupIds 权限组 ID 集合
     * @return 更新后的权限明细
     */
    UserPermissionDetailVO setUserGroups(Integer userId, Collection<Integer> groupIds);

    /**
     * 覆盖设置用户被直接授予的权限节点
     *
     * @param userId 用户 ID
     * @param nodes  权限节点集合
     * @return 更新后的权限明细
     */
    UserPermissionDetailVO setUserNodes(Integer userId, List<String> nodes);
}
