package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyEmailRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyPasswordRequest;
import top.thexiaola.dreamhwhub.module.login.dto.ModifyUserInfoRequest;
import top.thexiaola.dreamhwhub.module.login.dto.RetrievePasswordModifyRequest;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.EmailService;
import top.thexiaola.dreamhwhub.module.login.service.ModifyUserService;
import top.thexiaola.dreamhwhub.support.encryption.AESEncryptionUtil;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.SessionManager;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import static top.thexiaola.dreamhwhub.module.login.service.impl.LoginUserServiceImpl.getUser;

@Service
public class ModifyUserServiceImpl implements ModifyUserService {
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final AESEncryptionUtil aesEncryptionUtil;

    public ModifyUserServiceImpl(UserMapper userMapper, EmailService emailService, AESEncryptionUtil aesEncryptionUtil) {
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.aesEncryptionUtil = aesEncryptionUtil;
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

        // 更新数据库
        User updatedUser = userMapper.selectById(user.getId());
        if (updatedUser != null) {
            // 同步更新 Session 中的用户信息
            HttpServletRequest request = LogUtil.getRequest();
            if (request != null) {
                request.getSession().setAttribute("user", updatedUser);
                SessionManager.updateUserSession(user.getId(), updatedUser);
            }
        }

        // 返回成功
        return updatedUser;
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
        
        // 同步更新 Session 中的用户信息
        HttpServletRequest request = LogUtil.getRequest();
        if (request != null) {
            request.getSession().setAttribute("user", user);
            SessionManager.updateUserSession(user.getId(), user);
        }

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
    
    @Override
    public void modifyUserPassword(ModifyPasswordRequest modifyPasswordRequest) {
        // 获取当前用户
        User user = UserUtils.getCurrentUser();
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        String oldPassword = modifyPasswordRequest.getOldPassword();
        String newPassword = modifyPasswordRequest.getNewPassword();

        // 验证原密码是否正确
        if (!aesEncryptionUtil.verifyPassword(oldPassword, user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.INVALID_OLD_PASSWORD, "原密码错误", null);
        }

        // 检查新密码是否与原密码相同
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(BusinessErrorCode.NEW_PASSWORD_SAME_AS_OLD, "新密码不能与原密码相同", null);
        }

        // 加密新密码
        String encryptedNewPassword = aesEncryptionUtil.encrypt(newPassword);

        // 更新密码
        user.setPassword(encryptedNewPassword);
        userMapper.updateById(user);

        // 使该用户的所有 Session 失效
        SessionManager.invalidateSession(user.getId());
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
        emailService.sendRetrievePasswordEmailCode(user.getEmail(), user.getUserNo(), user.getUsername());
        
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
            user.getEmail(), code, user.getUserNo(), user.getUsername()
        );
        if (!isCodeValid) {
            throw new BusinessException(BusinessErrorCode.VERIFICATION_CODE_INVALID, "验证码错误", null);
        }
        
        // 加密新密码
        String encryptedNewPassword = aesEncryptionUtil.encrypt(newPassword);
        
        // 更新密码
        user.setPassword(encryptedNewPassword);
        userMapper.updateById(user);
        
        // 使该用户的所有 Session 失效（强制重新登录）
        SessionManager.invalidateSession(user.getId());
        
        return user;
    }
    
    /**
     * 根据账号查找用户（支持学号、用户名、邮箱）
     */
    private User getUserByAccount(String account) {
        return getUser(account, userMapper);
    }
}