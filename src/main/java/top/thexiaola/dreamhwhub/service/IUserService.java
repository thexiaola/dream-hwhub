package top.thexiaola.dreamhwhub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.thexiaola.dreamhwhub.domain.User;
import top.thexiaola.dreamhwhub.dto.PageResult;
import top.thexiaola.dreamhwhub.dto.UserQueryRequest;

public interface IUserService extends IService<User> {
    User login(User user);
    User register(User user, String verificationCode, String invitationCode);
    User checkUserExists(String userNo, String email);
    String sendVerificationCode(String userNo, String email);
    
    // 管理员功能
    User adminCreateUser(User user);
    User adminUpdateUser(Integer id, User user);
    boolean adminDeleteUser(Integer id);
    PageResult<User> adminListUsers(UserQueryRequest queryRequest);
}