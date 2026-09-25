package top.thexiaola.dreamhwhub.module.login.service;

import top.thexiaola.dreamhwhub.module.login.dto.DeleteAccountRequest;

/**
 * 账号注销服务
 * <p>
 * 注销为不可逆操作，须由账号所有者本人凭登录密码确认；
 * 注销后自动退出全部班级与学校、删除其作业提交，并清理好友/私信/站内信/权限等关联数据。
 */
public interface AccountDeletionService {

    /**
     * 注销当前登录账号
     *
     * @param request 注销请求（含用于验证身份的登录密码）
     */
    void deleteMyAccount(DeleteAccountRequest request);
}
