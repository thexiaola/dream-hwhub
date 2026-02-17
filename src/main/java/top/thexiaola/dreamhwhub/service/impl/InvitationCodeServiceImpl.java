package top.thexiaola.dreamhwhub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.thexiaola.dreamhwhub.domain.InvitationCode;
import top.thexiaola.dreamhwhub.domain.User;
import top.thexiaola.dreamhwhub.mapper.InvitationCodeMapper;
import top.thexiaola.dreamhwhub.service.IInvitationCodeService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationCodeServiceImpl extends ServiceImpl<InvitationCodeMapper, InvitationCode> implements IInvitationCodeService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvitationCodeServiceImpl.class);
    
    private static final int PERMISSION_THRESHOLD = 50;

    @Override
    public List<String> generateInvitationCodes(User currentUser, Integer count, Integer expireDays) {
        logger.info("批量生成邀请码请求，操作用户ID: {}, 数量: {}, 有效期: {}天",
                   currentUser.getId(), count, expireDays);
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法生成邀请码，操作用户ID: {}, 权限等级: {}", 
                       currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        List<String> codes = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // 生成唯一邀请码
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            
            InvitationCode invitationCode = new InvitationCode();
            invitationCode.setCode(code);
            invitationCode.setCreatorId(currentUser.getId());
            invitationCode.setUsedCount(0);
            invitationCode.setMaxUsage(1); // 每个邀请码只能用一次
            invitationCode.setCreatedTime(LocalDateTime.now());
            invitationCode.setExpireTime(LocalDateTime.now().plusDays(expireDays));
            invitationCode.setIsActive(true);
            
            boolean result = this.save(invitationCode);
            if (result) {
                codes.add(code);
                logger.info("邀请码生成成功，邀请码: {}, ID: {}", code, invitationCode.getId());
            } else {
                logger.error("邀请码生成失败，创建者ID: {}", currentUser.getId());
                throw new RuntimeException("邀请码生成失败");
            }
        }
        
        logger.info("批量生成邀请码完成，共生成 {} 个", codes.size());
        return codes;
    }

    @Override
    public boolean validateInvitationCode(String code) {
        logger.debug("验证邀请码，邀请码: {}", code);
        
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        InvitationCode invitationCode = this.getOne(queryWrapper);
        
        if (invitationCode == null) {
            logger.warn("邀请码验证失败：邀请码不存在，邀请码: {}", code);
            return false;
        }
        
        if (!invitationCode.getIsActive()) {
            logger.warn("邀请码验证失败：邀请码已被禁用，邀请码: {}", code);
            return false;
        }
        
        // 每个邀请码只能使用一次
        if (invitationCode.getUsedCount() >= 1) {
            logger.warn("邀请码验证失败：邀请码已使用过，邀请码: {}", code);
            return false;
        }
        
        if (LocalDateTime.now().isAfter(invitationCode.getExpireTime())) {
            logger.warn("邀请码验证失败：邀请码已过期，邀请码: {}", code);
            return false;
        }
        
        logger.info("邀请码验证成功，邀请码: {}", code);
        return true;
    }

    @Override
    public boolean useInvitationCode(String code) {
        logger.info("使用邀请码，邀请码: {}", code);
        
        if (!validateInvitationCode(code)) {
            return false;
        }
        
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        InvitationCode invitationCode = this.getOne(queryWrapper);
        
        invitationCode.setUsedCount(invitationCode.getUsedCount() + 1);
        
        boolean result = this.updateById(invitationCode);
        if (result) {
            logger.info("邀请码使用成功，邀请码: {}, 当前使用次数: {}", code, invitationCode.getUsedCount());
            return true;
        } else {
            logger.error("邀请码使用失败，邀请码: {}", code);
            return false;
        }
    }

    @Override
    public boolean deleteInvitationCode(User currentUser, String code) {
        logger.info("删除邀请码，操作用户ID: {}, 邀请码: {}", currentUser.getId(), code);
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法删除邀请码，操作用户ID: {}, 权限等级: {}", 
                       currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        boolean result = this.remove(queryWrapper);
        
        if (result) {
            logger.info("邀请码删除成功，邀请码: {}", code);
        } else {
            logger.warn("邀请码删除失败：邀请码不存在，邀请码: {}", code);
        }
        
        return result;
    }

    @Override
    public List<InvitationCode> getAllInvitationCodes(User currentUser) {
        logger.info("获取所有邀请码，操作用户ID: {}", currentUser.getId());
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法查看所有邀请码，操作用户ID: {}, 权限等级: {}", 
                       currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        QueryWrapper<InvitationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("created_time");
        
        List<InvitationCode> codes = this.list(queryWrapper);
        logger.info("获取到 {} 个邀请码", codes.size());
        return codes;
    }

}