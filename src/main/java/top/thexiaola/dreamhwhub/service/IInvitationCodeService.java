package top.thexiaola.dreamhwhub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.thexiaola.dreamhwhub.domain.InvitationCode;
import top.thexiaola.dreamhwhub.domain.User;

import java.util.List;

public interface IInvitationCodeService extends IService<InvitationCode> {
    
    /**
     * 生成邀请码（管理员功能，带权限检查）
     * @param currentUser 当前操作用户
     * @param count 生成数量
     * @param expireDays 有效期天数
     * @return 生成的邀请码列表
     */
    List<String> generateInvitationCodes(User currentUser, Integer count, Integer expireDays);
    
    /**
     * 验证邀请码是否有效
     * @param code 邀请码
     * @return 是否有效
     */
    boolean validateInvitationCode(String code);
    
    /**
     * 使用邀请码（增加使用次数）
     * @param code 邀请码
     * @return 是否使用成功
     */
    boolean useInvitationCode(String code);
    
    /**
     * 删除邀请码（管理员功能，带权限检查）
     * @param currentUser 当前操作用户
     * @param code 邀请码
     * @return 是否删除成功
     */
    boolean deleteInvitationCode(User currentUser, String code);
    
    /**
     * 获取所有邀请码（管理员功能，带权限检查）
     * @param currentUser 当前操作用户
     * @return 邀请码列表
     */
    List<InvitationCode> getAllInvitationCodes(User currentUser);
    

}