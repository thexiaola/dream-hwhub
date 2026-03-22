package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

        // 验证字段
        if (newUserNo == null || newUserNo.trim().isEmpty()) {
            return ServiceResult.failure(BusinessErrorCode.USER_NO_REQUIRED);
        }
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ServiceResult.failure(BusinessErrorCode.USERNAME_REQUIRED);
        }
        
        // 验证学号唯一性（排除自己）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", newUserNo);
        queryWrapper.ne("id", user.getId());
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            return ServiceResult.failure(BusinessErrorCode.USER_NO_EXISTS);
        }
        
        // 验证用户名唯一性（排除自己）
        QueryWrapper<User> usernameQueryWrapper = new QueryWrapper<>();
        usernameQueryWrapper.eq("username", newUsername);
        usernameQueryWrapper.ne("id", user.getId());
        User existingUsernameUser = userMapper.selectOne(usernameQueryWrapper);
        if (existingUsernameUser != null) {
            return ServiceResult.failure(BusinessErrorCode.USERNAME_EXISTS);
        }
        
        user.setUserNo(newUserNo);
        user.setUsername(newUsername);

        user.setIdName(newIdName);
        user.setPhone(newPhone);

        // 传入数据库
        userMapper.updateById(user);

        // 返回成功
        return ServiceResult.success(user);
    }
}