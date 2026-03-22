package top.thexiaola.dreamhwhub.module.login.service.impl;

import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ServiceResult;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.util.UserUtils;

@Service
public class ModifyUserServiceImpl implements ModifyUserService {
    private final UserMapper userMapper;

    public ModifyUserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public ServiceResult<User> modifyUserInfo(ModifyUserInfoRequest modifyUserInfoRequest) {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            return ServiceResult.failure(BusinessErrorCode.USER_NOT_LOGGED_IN);
        }

        // 新字段数据
        String newUserNo = modifyUserInfoRequest.getUserNo();
        String newUsername = modifyUserInfoRequest.getUsername();
        String newIdName = modifyUserInfoRequest.getIdName();
        String newPhone = modifyUserInfoRequest.getPhone();

        // 更新字段
        if(newUserNo == null) {
            return ServiceResult.failure(BusinessErrorCode.USER_NO_REQUIRED);
        }
        user.setUserNo(newUserNo);

        if(newUsername == null) {
            return ServiceResult.failure(BusinessErrorCode.USERNAME_REQUIRED);
        }
        user.setUsername(newUsername);

        user.setIdName(newIdName);
        user.setPhone(newPhone);

        // 传入数据库
        userMapper.updateById(user);

        // 返回成功
        return ServiceResult.success(user);
    }
}