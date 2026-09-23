package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyEmailRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyPasswordRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RetrievePasswordModifyRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.support.password.PasswordUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;
import top.thexiaola.dreamhwhub.support.storage.AvatarStorageService;

import static top.thexiaola.dreamhwhub.module.login.service.impl.LoginUserServiceImpl.getUser;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModifyUserServiceImpl implements ModifyUserService {
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final PasswordUtil passwordUtil;
    private final AvatarStorageService avatarStorageService;

    @Override
    public User modifyUserInfo(ModifyUserInfoRequest modifyUserInfoRequest) {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 新字段数据
        String newUsername = modifyUserInfoRequest.getUsername();
        // 手机号留空（null 或纯空白）表示删除手机号
        String rawPhone = modifyUserInfoRequest.getPhone();
        String newPhone = (rawPhone == null || rawPhone.trim().isEmpty()) ? null : rawPhone.trim();

        // 用户名不区分大小写唯一（排除自己）
        QueryWrapper<User> usernameQueryWrapper = new QueryWrapper<>();
        usernameQueryWrapper.ne("id", user.getId());
        usernameQueryWrapper.apply("LOWER(username) = LOWER({0})", newUsername);
        User existingUsernameUser = userMapper.selectOne(usernameQueryWrapper);
        if (existingUsernameUser != null) {
            throw new BusinessException(BusinessErrorCode.USERNAME_EXISTS, "用户名已存在", null);
        }

        // updateById 默认忽略 null 字段，手机号需删除时必须用 UpdateWrapper 显式置空
        userMapper.update(null, new UpdateWrapper<User>()
                .eq("id", user.getId())
                .set("username", newUsername)
                .set("phone", newPhone));

        return userMapper.selectById(user.getId());
    }

    @Override
    public User modifyUserAvatar(MultipartFile file) {
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        String oldAvatar = user.getAvatar();
        String newAvatar = avatarStorageService.save(file, user.getId());

        try {
            user.setAvatar(newAvatar);
            userMapper.updateById(user);
        } catch (RuntimeException e) {
            // 入库失败时清理刚写入的文件，避免残留孤儿文件
            avatarStorageService.delete(newAvatar);
            throw e;
        }

        // 更新成功后再删除旧头像文件
        avatarStorageService.delete(oldAvatar);
        log.info("User {} updated avatar to {}", user.getUsername(), newAvatar);
        return userMapper.selectById(user.getId());
    }

    @Override
    public User removeUserAvatar() {
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        String oldAvatar = user.getAvatar();
        // updateById 默认忽略 null 字段，需用 UpdateWrapper 显式把 avatar 置空
        userMapper.update(null, new UpdateWrapper<User>()
                .eq("id", user.getId())
                .set("avatar", null));
        avatarStorageService.delete(oldAvatar);
        log.info("User {} removed avatar", user.getUsername());
        return userMapper.selectById(user.getId());
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
        boolean isBeforeCodeValid = emailService.verifyModifyCode(oldEmail, beforeCode, user.getUsername());
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
        boolean isAfterCodeValid = emailService.verifyModifyCode(newEmail, afterCode, user.getUsername());
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

        // 检查新邮箱是否与原邮箱相同
        if (email.equals(user.getEmail())) {
            throw new BusinessException(BusinessErrorCode.SAME_EMAIL, "新邮箱不能与原邮箱相同", null);
        }

        // 发送验证码到新邮箱
        emailService.sendModifyEmailCode(email, user.getUsername());
    }
    
    @Override
    public void sendModifyCodeToOldEmail() {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 发送验证码到原邮箱（当前邮箱）
        emailService.sendModifyEmailCode(user.getEmail(), user.getUsername());
    }
    
    @Override
    public void modifyUserPassword(ModifyPasswordRequest modifyPasswordRequest) {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        String oldPassword = modifyPasswordRequest.getOldPassword();
        String newPassword = modifyPasswordRequest.getNewPassword();

        // 验证原密码是否正确(使用BCrypt)
        if (!passwordUtil.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.INVALID_OLD_PASSWORD, "原密码错误", null);
        }

        // 检查新密码是否与原密码相同
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(BusinessErrorCode.NEW_PASSWORD_SAME_AS_OLD, "新密码不能与原密码相同", null);
        }

        // 使用BCrypt加密新密码
        String encryptedNewPassword = passwordUtil.encode(newPassword);

        // 更新密码
        user.setPassword(encryptedNewPassword);
        userMapper.updateById(user);

        // JWT是无状态的，客户端删除Token即可
    }
    
    @Override
    public User sendRetrievePasswordCode(String account) {
        // 根据账号查找用户
        User user = getUserByAccount(account);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "账号不存在", null);
        }
        
        // 检查用户是否被封禁
        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new BusinessException(BusinessErrorCode.USER_BANNED, "用户已被封禁", null);
        }
        
        // 发送验证码到用户邮箱
        emailService.sendRetrievePasswordEmailCode(user.getEmail(), user.getUsername());
        
        return user;
    }
    
    @Override
    public User retrievePassword(RetrievePasswordModifyRequest request) {
        String account = request.getAccount();
        String code = request.getCode();
        String newPassword = request.getNewPassword();
        
        // 根据账号查找用户
        User user = getUserByAccount(account);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "账号不存在", null);
        }
        
        // 验证验证码
        boolean isCodeValid = emailService.verifyRetrievePasswordCode(
            user.getEmail(), code, user.getUsername()
        );
        if (!isCodeValid) {
            throw new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID, "验证码错误", null);
        }
        
        // 使用BCrypt加密新密码
        String encryptedNewPassword = passwordUtil.encode(newPassword);
        
        // 更新密码
        user.setPassword(encryptedNewPassword);
        userMapper.updateById(user);
        
        // JWT是无状态的，客户端删除Token即可
        
        return user;
    }
    
    /**
     * 根据账号查找用户（支持学号、用户名、邮箱）
     */
    private User getUserByAccount(String account) {
        return getUser(account, userMapper);
    }
}