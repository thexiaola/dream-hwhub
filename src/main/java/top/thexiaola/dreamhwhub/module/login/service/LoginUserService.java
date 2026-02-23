package top.thexiaola.dreamhwhub.module.login.service;

import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.LoginRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RegisterRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;

/**
 * 用户登录服务接口
 */
public interface LoginUserService {

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 服务结果，包含用户信息或错误码
     */
    ServiceResult<User> register(RegisterRequest registerRequest);

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 服务结果，包含用户信息或错误码
     */
    ServiceResult<User> login(LoginRequest loginRequest);

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @param userNo 学号
     * @param username 用户名
     * @return 服务结果，包含操作状态或错误码
     */
    ServiceResult<Void> sendEmailCode(String email, String userNo, String username);

    /**
     * 验证邮箱验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证通过
     */
    boolean verifyEmailCode(String email, String code);

    /**
     * 检查学号是否已存在
     * @param userNo 学号/工号
     * @return 是否存在
     */
    boolean isUserNoExists(String userNo);

    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean isUsernameExists(String username);

    /**
     * 检查邮箱是否已存在
     * @param email 邮箱
     * @return 是否存在
     */
    boolean isEmailExists(String email);

    /**
     * 根据账号查找用户（支持学号、用户名、邮箱）
     * @param account 账号
     * @return 用户信息
     */
    User findByAccount(String account);
}