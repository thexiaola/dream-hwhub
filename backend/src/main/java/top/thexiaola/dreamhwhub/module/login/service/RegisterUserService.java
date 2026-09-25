package top.thexiaola.dreamhwhub.module.login.service;

import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;

/**
 * 用户注册服务接口
 */
public interface RegisterUserService {

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 服务结果，包含用户信息或错误码
     */
    User register(RegisterRequest registerRequest);

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @param username 用户名
     */
    void sendEmailCode(String email, String username);
}
