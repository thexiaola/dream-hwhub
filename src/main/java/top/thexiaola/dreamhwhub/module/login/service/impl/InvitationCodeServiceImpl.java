package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.module.login.domain.InvitationCode;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.mapper.InvitationCodeMapper;
import top.thexiaola.dreamhwhub.module.login.service.InvitationCodeService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationCodeServiceImpl extends ServiceImpl<InvitationCodeMapper, InvitationCode> implements InvitationCodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvitationCodeServiceImpl.class);
    private static final int PERMISSION_THRESHOLD = 50;

    @Override
    public List<String> generateInvitationCodes(User currentUser, Integer count, Integer expireDays) {
        logger.info("生成邀请码请求，操作用户ID: {}, 数量: {}, 有效期: {}天", currentUser.getId(), count, expireDays);
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法生成邀请码，操作用户ID: {}, 权限等级: {}", currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        // 参数验证
        if (count == null || count <= 0) {
            logger.warn("生成邀请码失败：数量必须大于0");
            throw new RuntimeException("生成数量必须大于0");
        }
        if (count > 100) {
            logger.warn("生成邀请码失败：单次生成数量不能超过100");
            throw new RuntimeException("单次生成数量不能超过100");
        }
        if (expireDays == null || expireDays <= 0) {
            logger.warn("生成邀请码失败：有效期必须大于0");
            throw new RuntimeException("有效期必须大于0");
        }
        if (expireDays > 365) {
            logger.warn("生成邀请码失败：有效期不能超过365天");
            throw new RuntimeException("有效期不能超过365天");
        }
        
        List<String> generatedCodes = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // 生成唯一邀请码
            String code;
            int attempts = 0;
            do {
                code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
                attempts++;
                if (attempts > 10) {
                    logger.error("生成唯一邀请码失败，尝试次数过多");
                    throw new RuntimeException("生成邀请码失败，请稍后重试");
                }
            } while (isCodeExists(code));
            
            // 创建邀请码实体
            InvitationCode invitationCode = new InvitationCode();
            invitationCode.setCode(code);
            invitationCode.setCreatorId(currentUser.getId());
            invitationCode.setUsedCount(0);
            invitationCode.setMaxUsage(1);
            invitationCode.setCreatedTime(LocalDateTime.now());
            invitationCode.setExpireTime(LocalDateTime.now().plusDays(expireDays));
            invitationCode.setIsActive(true);
            
            boolean result = this.save(invitationCode);
            if (result) {
                generatedCodes.add(code);
                logger.debug("邀请码生成成功: {}", code);
            } else {
                logger.error("邀请码保存失败: {}", code);
                throw new RuntimeException("邀请码生成失败");
            }
        }
        
        logger.info("邀请码生成成功，共生成 {} 个邀请码，操作用户ID: {}", generatedCodes.size(), currentUser.getId());
        return generatedCodes;
    }

    @Override
    public boolean validateInvitationCode(String code) {
        logger.debug("验证邀请码有效性，邀请码: {}", code);
        
        if (code == null || code.isEmpty()) {
            logger.warn("验证邀请码失败：邀请码为空");
            return false;
        }
        
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        InvitationCode invitationCode = this.getOne(queryWrapper);
        
        if (invitationCode == null) {
            logger.debug("验证邀请码失败：邀请码不存在，邀请码: {}", code);
            return false;
        }
        
        if (!invitationCode.getIsActive()) {
            logger.debug("验证邀请码失败：邀请码已停用，邀请码: {}", code);
            return false;
        }
        
        if (invitationCode.getUsedCount() >= invitationCode.getMaxUsage()) {
            logger.debug("验证邀请码失败：邀请码已达最大使用次数，邀请码: {}", code);
            return false;
        }
        
        if (LocalDateTime.now().isAfter(invitationCode.getExpireTime())) {
            logger.debug("验证邀请码失败：邀请码已过期，邀请码: {}", code);
            return false;
        }
        
        logger.debug("邀请码验证通过，邀请码: {}", code);
        return true;
    }

    @Override
    public boolean useInvitationCode(String code) {
        logger.info("使用邀请码，邀请码: {}", code);
        
        if (code == null || code.isEmpty()) {
            logger.warn("使用邀请码失败：邀请码为空");
            return false;
        }
        
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        InvitationCode invitationCode = this.getOne(queryWrapper);
        
        if (invitationCode == null) {
            logger.warn("使用邀请码失败：邀请码不存在，邀请码: {}", code);
            return false;
        }
        
        if (!invitationCode.getIsActive()) {
            logger.warn("使用邀请码失败：邀请码已停用，邀请码: {}", code);
            return false;
        }
        
        if (invitationCode.getUsedCount() >= invitationCode.getMaxUsage()) {
            logger.warn("使用邀请码失败：邀请码已达最大使用次数，邀请码: {}", code);
            return false;
        }
        
        if (LocalDateTime.now().isAfter(invitationCode.getExpireTime())) {
            logger.warn("使用邀请码失败：邀请码已过期，邀请码: {}", code);
            return false;
        }
        
        // 增加使用次数
        invitationCode.setUsedCount(invitationCode.getUsedCount() + 1);
        boolean result = this.updateById(invitationCode);
        
        if (result) {
            logger.info("邀请码使用成功，邀请码: {}, 当前使用次数: {}", code, invitationCode.getUsedCount());
            return true;
        } else {
            logger.error("邀请码使用失败，更新数据库失败，邀请码: {}", code);
            return false;
        }
    }

    @Override
    public boolean deleteInvitationCode(User currentUser, String code) {
        logger.info("删除邀请码请求，操作用户ID: {}, 邀请码: {}", currentUser.getId(), code);
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法删除邀请码，操作用户ID: {}, 权限等级: {}", currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        if (code == null || code.isEmpty()) {
            logger.warn("删除邀请码失败：邀请码为空");
            throw new RuntimeException("邀请码不能为空");
        }
        
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        InvitationCode invitationCode = this.getOne(queryWrapper);
        
        if (invitationCode == null) {
            logger.warn("删除邀请码失败：邀请码不存在，邀请码: {}", code);
            throw new RuntimeException("邀请码不存在");
        }
        
        boolean result = this.removeById(invitationCode.getId());
        if (result) {
            logger.info("删除邀请码成功，邀请码: {}, 操作用户ID: {}", code, currentUser.getId());
            return true;
        } else {
            logger.error("删除邀请码失败，操作用户ID: {}", currentUser.getId());
            throw new RuntimeException("删除邀请码失败");
        }
    }

    @Override
    public List<InvitationCode> getAllInvitationCodes(User currentUser) {
        logger.info("获取所有邀请码请求，操作用户ID: {}", currentUser.getId());
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法获取邀请码列表，操作用户ID: {}, 权限等级: {}", currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_time");
        
        List<InvitationCode> codes = this.list(queryWrapper);
        logger.info("获取邀请码列表成功，共 {} 条记录，操作用户ID: {}", codes.size(), currentUser.getId());
        return codes;
    }
    
    /**
     * 检查邀请码是否已存在
     */
    private boolean isCodeExists(String code) {
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        return this.count(queryWrapper) > 0;
    }
}