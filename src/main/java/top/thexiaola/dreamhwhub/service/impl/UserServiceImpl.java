package top.thexiaola.dreamhwhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import top.thexiaola.dreamhwhub.domain.User;
import top.thexiaola.dreamhwhub.mapper.UserMapper;
import top.thexiaola.dreamhwhub.service.EmailService;
import top.thexiaola.dreamhwhub.service.IInvitationCodeService;
import top.thexiaola.dreamhwhub.service.IUserService;
import top.thexiaola.dreamhwhub.util.LogUtil;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final EmailService emailService;
    private final IInvitationCodeService invitationCodeService;

    public UserServiceImpl(EmailService emailService, IInvitationCodeService invitationCodeService) {
        this.emailService = emailService;
        this.invitationCodeService = invitationCodeService;
    }

    @Override
    public User login(User user) {
        logger.info("用户登录请求，学号: {}, 邮箱: {}", user.getUserNo(), user.getEmail());
        
        if ((user.getUserNo() == null || user.getUserNo().isEmpty()) && 
            (user.getEmail() == null || user.getEmail().isEmpty())) {
            logger.warn("登录失败：学号和邮箱均为空");
            throw new RuntimeException("学号或邮箱不能为空");
        }
        
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            logger.warn("登录失败：密码为空");
            throw new RuntimeException("密码不能为空");
        }
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 根据学号或邮箱查找用户
        if (user.getUserNo() != null && !user.getUserNo().isEmpty()) {
            queryWrapper.eq("user_no", user.getUserNo());
        }
        
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            if (!queryWrapper.getSqlSegment().isEmpty()) {
                queryWrapper.or().eq("email", user.getEmail());
            } else {
                queryWrapper.eq("email", user.getEmail());
            }
        }
        
        User dbUser = this.getOne(queryWrapper);
        
        if (dbUser != null) {
            // 对输入的密码进行加密后比较
            String encryptedInputPassword = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
            if (dbUser.getPassword().equals(encryptedInputPassword)) {
                logger.info("用户登录成功，用户ID: {}", dbUser.getId());
                return dbUser;
            } else {
                logger.warn("用户登录失败：密码错误，学号/邮箱: {}/{}", user.getUserNo(), user.getEmail());
                throw new RuntimeException("密码错误");
            }
        }
        
        logger.warn("用户登录失败：用户不存在，学号/邮箱: {}/{}", user.getUserNo(), user.getEmail());
        throw new RuntimeException("用户不存在");
    }

    @Override
    public User register(User user, String verificationCode, String invitationCode) {
        logger.info("用户注册请求，学号: {}, 邮箱: {}", user.getUserNo(), user.getEmail());
        
        // 验证必填字段
        if (user.getUserNo() == null || user.getUserNo().isEmpty()) {
            logger.warn("注册失败：学号为空");
            throw new RuntimeException("学号不能为空");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            logger.warn("注册失败：邮箱为空");
            throw new RuntimeException("邮箱不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            logger.warn("注册失败：密码为空");
            throw new RuntimeException("密码不能为空");
        }
        if (verificationCode == null || verificationCode.isEmpty()) {
            logger.warn("注册失败：验证码为空");
            throw new RuntimeException("邮箱验证码不能为空");
        }
        
        // 验证用户是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", user.getUserNo()).or().eq("email", user.getEmail());
        User existingUser = this.getOne(queryWrapper);
        
        if (existingUser != null) {
            if (existingUser.getUserNo().equals(user.getUserNo())) {
                logger.warn("注册失败：学号已被注册，学号: {}", user.getUserNo());
                throw new RuntimeException("学号已被注册");
            } else if (existingUser.getEmail().equals(user.getEmail())) {
                logger.warn("注册失败：邮箱已被注册，邮箱: {}", user.getEmail());
                throw new RuntimeException("邮箱已被注册");
            }
        }
        
        // 验证邮箱验证码
        if (!emailService.verifyCode(user.getEmail(), verificationCode)) {
            logger.warn("注册失败：邮箱验证码错误，邮箱: {}", user.getEmail());
            throw new RuntimeException("邮箱验证码错误");
        }
        
        // 验证邀请码
        if (invitationCode != null && !invitationCode.isEmpty()) {
            logger.info("验证邀请码，邀请码: {}", invitationCode);
            if (!invitationCodeService.validateInvitationCode(invitationCode)) {
                logger.warn("注册失败：邀请码无效，邀请码: {}", invitationCode);
                throw new RuntimeException("邀请码无效或已过期");
            }
            // 使用邀请码
            invitationCodeService.useInvitationCode(invitationCode);
            logger.info("邀请码验证通过并已使用，邀请码: {}", invitationCode);
        }
        
        // 对密码进行加密
        String encryptedPassword = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
        user.setPassword(encryptedPassword);
        
        boolean result = this.save(user);
        if (result) {
            logger.info("用户注册成功，用户ID: {}", user.getId());
        } else {
            logger.error("用户注册失败，保存数据库失败，邮箱: {}", user.getEmail());
            throw new RuntimeException("用户注册失败");
        }
        
        return user;
    }
    
    @Override
    public User checkUserExists(String userNo, String email) {
        logger.debug("检查用户是否存在，学号: {}, 邮箱: {}", userNo, email);
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", userNo).or().eq("email", email);
        User result = this.getOne(queryWrapper);
        
        if (result != null) {
            logger.info("用户已存在，学号: {}, 邮箱: {}", result.getUserNo(), result.getEmail());
        } else {
            logger.debug("用户不存在，学号: {}, 邮箱: {}", userNo, email);
        }
        
        return result;
    }
    
    @Override
    public String sendVerificationCode(String userNo, String email) {
        logger.debug("发送验证码请求，学号: {}, 邮箱: {}", userNo, email);
        
        // 验证学号和邮箱是否已存在
        User existingUser = this.checkUserExists(userNo, email);
        if (existingUser != null) {
            if (existingUser.getUserNo().equals(userNo)) {
                logger.warn("发送验证码失败：学号已被注册，{}", LogUtil.getUserInfo(existingUser));
                throw new RuntimeException("学号已被注册");
            } else if (existingUser.getEmail().equals(email)) {
                logger.warn("发送验证码失败：邮箱已被注册，{}", LogUtil.getUserInfo(existingUser));
                throw new RuntimeException("邮箱已被注册");
            }
        }
        
        // 发送验证码
        emailService.sendVerificationCode(email);
        logger.debug("验证码发送成功，邮箱: {}", email);
        return "验证码已发送";
    }
}