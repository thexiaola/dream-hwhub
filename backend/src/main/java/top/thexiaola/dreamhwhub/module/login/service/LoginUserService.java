package top.thexiaola.dreamhwhub.module.login.service;

import jakarta.servlet.http.HttpServletRequest;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;

/**
 * 用户登录服务接口
 */
public interface LoginUserService {

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 服务结果，包含用户信息或错误码
     */
    User login(LoginRequest loginRequest, jakarta.servlet.http.HttpServletRequest request);

    /**
     * 根据账号查找用户 (支持学号、用户名、邮箱)
     * @param account 账号
     * @return 用户信息
     */
    User findByAccount(String account);
    
    /**
     * 用户登出
     * @param userId 用户 ID
     * @param request HTTP 请求
     */
    void logout(Integer userId, HttpServletRequest request);
    
    /**
     * 获取当前登录用户
     * @param request HTTP 请求
     * @return 当前登录用户
     */
    User getCurrentUser(HttpServletRequest request);
}
