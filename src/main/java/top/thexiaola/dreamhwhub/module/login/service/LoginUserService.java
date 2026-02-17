package top.thexiaola.dreamhwhub.module.login.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.thexiaola.dreamhwhub.module.login.domain.User;

public interface LoginUserService extends IService<User> {
    User login(User user);
    User register(User user, String verificationCode, String invitationCode);
    User checkUserExists(String userNo, String email);
    String sendVerificationCode(String userNo, String email);
}