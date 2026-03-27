package top.thexiaola.dreamhwhub.module.login.service;

import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyEmailRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;

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
}
