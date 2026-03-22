package top.thexiaola.dreamhwhub.module.login.service;

import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;

public interface ModifyUserService {
    /**
     * 修改用户信息
     *
     * @param modifyUserInfoRequest 修改用户信息请求
     * @return 修改结果
     */
    ServiceResult<User> modifyUserInfo(ModifyUserInfoRequest modifyUserInfoRequest);
}
