package top.thexiaola.dreamhwhub.support.session;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.permission.service.PermissionService;

/**
 * 用户查询支撑类
 * <p>
 * 提供服务层通用的「取当前登录用户」「按账号查用户」「权限节点判定」，
 * 统一空值校验与账号匹配规则，避免各 Service 重复实现。
 */
@Component
@RequiredArgsConstructor
public class UserLookupSupport {

    private final UserMapper userMapper;
    private final PermissionService permissionService;

    /**
     * 获取当前登录用户，未登录则抛出业务异常
     *
     * @return 当前登录用户
     */
    public User requireCurrentUser() {
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }
        return currentUser;
    }

    /**
     * 按用户名或邮箱查询用户（用户名区分大小写），不存在则抛出业务异常
     *
     * @param userAccount 用户名或邮箱
     * @return 匹配到的用户
     */
    public User findByAccountOrThrow(String userAccount) {
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.and(q -> q.apply("BINARY username = BINARY {0}", userAccount).or().eq("email", userAccount));
        User targetUser = userMapper.selectOne(userQuery);
        if (targetUser == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "用户不存在，请确认用户名或邮箱是否正确", null);
        }
        return targetUser;
    }

    /**
     * 判断用户是否拥有指定的权限节点（平台管理员 OP 恒定拥有）
     *
     * @param user 用户对象，可为 null
     * @param node 权限节点
     * @return true-拥有该权限节点
     */
    public boolean hasPermission(User user, String node) {
        return user != null && permissionService.hasPermission(user.getId(), node);
    }
}
