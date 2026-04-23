package top.thexiaola.dreamhwhub.module.login.service;

import top.thexiaola.dreamhwhub.module.login.dto.ModifyEmailRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyPasswordRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RetrievePasswordModifyRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;

public interface ModifyUserService {
    /**
     * 修改用户信息
     *
     * @param modifyUserInfoRequest 修改用户信息请求
     * @return 修改结果
     */
    User modifyUserInfo(ModifyUserInfoRequest modifyUserInfoRequest);
    
    /**
     * 修改用户邮箱
     *
     * @param modifyEmailRequest 修改邮箱请求
     * @return 修改后的用户对象
     */
    User modifyUserEmail(ModifyEmailRequest modifyEmailRequest);
    
    /**
     * 为换绑的目标邮箱发送验证码
     *
     * @param email 目标邮箱
     */
    void sendModifyCodeToNewEmail(String email);
    
    /**
     * 为换绑前的原邮箱发送验证码
     */
    void sendModifyCodeToOldEmail();
    
    /**
     * 修改用户密码
     *
     * @param modifyPasswordRequest 修改密码请求
     */
    void modifyUserPassword(ModifyPasswordRequest modifyPasswordRequest);
    
    /**
     * 发送找回密码验证码
     *
     * @param account 账号（学号/用户名/邮箱）
     * @return 用户对象
     */
    User sendRetrievePasswordCode(String account);
    
    /**
     * 找回密码（通过验证码修改密码）
     *
     * @param retrievePasswordModifyRequest 找回密码修改密码请求
     * @return 用户对象
     */
    User retrievePassword(RetrievePasswordModifyRequest retrievePasswordModifyRequest);
}
