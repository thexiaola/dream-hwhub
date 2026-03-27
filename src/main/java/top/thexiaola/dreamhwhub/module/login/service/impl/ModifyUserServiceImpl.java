package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyEmailRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.util.UserUtils;

@Service
public class ModifyUserServiceImpl implements ModifyUserService {
    private final UserMapper userMapper;
    private final EmailService emailService;

    public ModifyUserServiceImpl(UserMapper userMapper, EmailService emailService) {
        this.userMapper = userMapper;
        this.emailService = emailService;
    }

    @Override
    public User modifyUserInfo(ModifyUserInfoRequest modifyUserInfoRequest) {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 新字段数据
        String newUserNo = modifyUserInfoRequest.getUserNo();
        String newUsername = modifyUserInfoRequest.getUsername();
        String newIdName = modifyUserInfoRequest.getIdName();
        String newPhone = modifyUserInfoRequest.getPhone();

        // 验证字段
        if (newUserNo == null || newUserNo.trim().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.USER_NO_REQUIRED, "学号不能为空", null);
        }
        if (newUsername == null || newUsername.trim().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.USERNAME_REQUIRED, "用户名不能为空", null);
        }
        
        // 验证学号唯一性（排除自己）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", newUserNo);
        queryWrapper.ne("id", user.getId());
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            throw new BusinessException(BusinessErrorCode.USER_NO_EXISTS, "学号已存在", null);
        }
        
        // 验证用户名唯一性（排除自己）
        QueryWrapper<User> usernameQueryWrapper = new QueryWrapper<>();
        usernameQueryWrapper.eq("username", newUsername);
        usernameQueryWrapper.ne("id", user.getId());
        User existingUsernameUser = userMapper.selectOne(usernameQueryWrapper);
        if (existingUsernameUser != null) {
            throw new BusinessException(BusinessErrorCode.USERNAME_EXISTS, "用户名已存在", null);
        }
        
        user.setUserNo(newUserNo);
        user.setUsername(newUsername);

        user.setIdName(newIdName);
        user.setPhone(newPhone);

        // 传入数据库
        userMapper.updateById(user);

        // 返回成功
        return user;
    }
    
    @Override
    public User modifyUserEmail(ModifyEmailRequest modifyEmailRequest) {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        String beforeCode = modifyEmailRequest.getBeforeCode();
        String newEmail = modifyEmailRequest.getNewEmail();
        String afterCode = modifyEmailRequest.getAfterCode();

        // 验证原邮箱验证码
        String oldEmail = user.getEmail();
        boolean isBeforeCodeValid = emailService.verifyModifyCode(oldEmail, beforeCode, user.getUserNo(), user.getUsername());
        if (!isBeforeCodeValid) {
            throw new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID, "原邮箱验证码错误", null);
        }

        // 检查新邮箱是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", newEmail);
        queryWrapper.ne("id", user.getId());
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            throw new BusinessException(BusinessErrorCode.EMAIL_EXISTS, "新邮箱已被使用", null);
        }

        // 验证新邮箱验证码（使用新邮箱作为 key）
        boolean isAfterCodeValid = emailService.verifyModifyCode(newEmail, afterCode, user.getUserNo(), user.getUsername());
        if (!isAfterCodeValid) {
            throw new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID, "新邮箱验证码错误", null);
        }

        // 更新邮箱
        user.setEmail(newEmail);
        userMapper.updateById(user);

        return user;
    }
    
    @Override
    public void sendModifyCodeToNewEmail(String email) {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 验证邮箱参数
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.INVALID_EMAIL_FORMAT, "新邮箱不能为空", null);
        }

        // 验证邮箱格式
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BusinessException(BusinessErrorCode.INVALID_EMAIL_FORMAT, "邮箱格式不正确", null);
        }

        // 检查新邮箱是否与原邮箱相同
        if (email.equals(user.getEmail())) {
            throw new BusinessException(BusinessErrorCode.SAME_EMAIL, "新邮箱不能与原邮箱相同", null);
        }

        // 发送验证码到新邮箱
        emailService.sendModifyEmailCode(email, user.getUserNo(), user.getUsername());
    }
    
    @Override
    public void sendModifyCodeToOldEmail() {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 发送验证码到原邮箱（当前邮箱）
        emailService.sendModifyEmailCode(user.getEmail(), user.getUserNo(), user.getUsername());
    }
}