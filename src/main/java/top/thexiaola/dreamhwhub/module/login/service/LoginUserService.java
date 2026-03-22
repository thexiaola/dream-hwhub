package top.thexiaola.dreamhwhub.module.login.service;

import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;

/**
 * 用户登录服务接口
 */
public interface LoginUserService {

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 服务结果，包含用户信息或错误码
     */
    User login(LoginRequest loginRequest);

    /**
     * 根据账号查找用户 (支持学号、用户名、邮箱)
     * @param account 账号
     * @return 用户信息
     */
    User findByAccount(String account);
}
