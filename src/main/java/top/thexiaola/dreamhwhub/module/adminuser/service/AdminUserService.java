package top.thexiaola.dreamhwhub.module.adminuser.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.dto.PageResult;
import top.thexiaola.dreamhwhub.dto.UserQueryRequest;

public interface AdminUserService extends IService<User> {
    // 管理员功能
    User adminCreateUser(User currentUser, User user);
    User adminUpdateUser(User currentUser, Integer id, User user);
    boolean adminDeleteUser(User currentUser, Integer id);
    PageResult<User> adminListUsers(User currentUser, UserQueryRequest queryRequest);
}