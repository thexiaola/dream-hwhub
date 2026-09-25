package top.thexiaola.dreamhwhub.module.login.service;

import top.thexiaola.dreamhwhub.module.login.dto.ModifyEmailRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyPasswordRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RetrievePasswordModifyRequest;
import top.thexiaola.dreamhwhub.module.login.dto.SecurityVerificationSettings;
import top.thexiaola.dreamhwhub.module.login.dto.UpdateSecurityVerificationRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface ModifyUserService {
    /**
     * 修改用户信息
     *
     * @param modifyUserInfoRequest 修改用户信息请求
     * @return 修改结果
     */
    User modifyUserInfo(ModifyUserInfoRequest modifyUserInfoRequest);

    /**
     * 更新当前用户头像
     *
     * @param file 头像文件
     * @return 更新后的用户对象
     */
    User modifyUserAvatar(MultipartFile file);

    /**
     * 清除当前用户头像
     *
     * @return 更新后的用户对象
     */
    User removeUserAvatar();
    
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

    /**
     * 查询当前用户的危险操作安全验证设置
     *
     * @return 设置（两个开关）
     */
    SecurityVerificationSettings getSecurityVerificationSettings();

    /**
     * 更新当前用户的危险操作安全验证设置。
     * <p>
     * 变更本身属于高危操作：**每个被改动的开关都必须用该方式自身的凭据验证身份**——
     * 改动了密码验证 → 校验登录密码；改动了邮箱验证码验证 → 校验邮箱验证码；两者都改动则两者都校验。
     * 未改动的方式无需凭据。
     *
     * @param request 新设置 + 对应凭据
     * @return 更新后的设置
     */
    SecurityVerificationSettings updateSecurityVerificationSettings(UpdateSecurityVerificationRequest request);
}
