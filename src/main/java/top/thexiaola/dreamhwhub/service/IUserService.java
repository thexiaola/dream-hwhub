package top.thexiaola.dreamhwhub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.thexiaola.dreamhwhub.domain.User;

public interface IUserService extends IService<User> {
    User login(User user);
    User register(User user, String verificationCode, String invitationCode);
    User checkUserExists(String userNo, String email);
    String sendVerificationCode(String userNo, String email);
}