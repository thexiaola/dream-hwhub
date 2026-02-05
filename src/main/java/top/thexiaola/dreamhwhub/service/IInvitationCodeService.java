package top.thexiaola.dreamhwhub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.thexiaola.dreamhwhub.domain.InvitationCode;

import java.util.List;

public interface IInvitationCodeService extends IService<InvitationCode> {
    
    /**
     * 生成邀请码（管理员功能）
     * @param creatorId 创建者ID
     * @param count 生成数量
     * @param expireDays 有效期天数
     * @return 生成的邀请码列表
     */
    List<String> generateInvitationCodes(Integer creatorId, Integer count, Integer expireDays);
    
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
     * 删除邀请码
     * @param code 邀请码
     * @return 是否删除成功
     */
    boolean deleteInvitationCode(String code);
    
    /**
     * 获取所有邀请码（管理员功能）
     * @return 邀请码列表
     */
    List<InvitationCode> getAllInvitationCodes();
    

}