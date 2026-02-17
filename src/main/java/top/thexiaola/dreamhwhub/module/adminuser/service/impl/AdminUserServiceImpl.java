package top.thexiaola.dreamhwhub.module.adminuser.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.dto.PageResult;
import top.thexiaola.dreamhwhub.dto.UserQueryRequest;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.adminuser.service.AdminUserService;

@Service
public class AdminUserServiceImpl extends ServiceImpl<UserMapper, User> implements AdminUserService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    private static final int PERMISSION_THRESHOLD = 50;

    @Override
    public User adminCreateUser(User currentUser, User user) {
        logger.info("管理员创建用户请求，操作用户ID: {}, 目标用户学号: {}", currentUser.getId(), user.getUserNo());
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法创建用户，操作用户ID: {}, 权限等级: {}", currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        // 验证目标用户信息
        if (user.getUserNo() == null || user.getUserNo().isEmpty()) {
            logger.warn("创建用户失败：学号为空");
            throw new RuntimeException("学号不能为空");
        }
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            logger.warn("创建用户失败：邮箱为空");
            throw new RuntimeException("邮箱不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            logger.warn("创建用户失败：密码为空");
            throw new RuntimeException("密码不能为空");
        }
        
        // 检查用户是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_no", user.getUserNo()).or().eq("email", user.getEmail());
        User existingUser = this.getOne(queryWrapper);
        
        if (existingUser != null) {
            if (existingUser.getUserNo().equals(user.getUserNo())) {
                logger.warn("创建用户失败：学号已被注册，学号: {}", user.getUserNo());
                throw new RuntimeException("学号已被注册");
            } else if (existingUser.getEmail().equals(user.getEmail())) {
                logger.warn("创建用户失败：邮箱已被注册，邮箱: {}", user.getEmail());
                throw new RuntimeException("邮箱已被注册");
            }
        }
        
        // 对密码进行加密
        String encryptedPassword = DigestUtils.md5DigestAsHex(user.getPassword().getBytes());
        user.setPassword(encryptedPassword);
        
        boolean result = this.save(user);
        if (result) {
            logger.info("管理员创建用户成功，用户ID: {}, 操作用户ID: {}", user.getId(), currentUser.getId());
            return user;
        } else {
            logger.error("管理员创建用户失败，保存数据库失败，操作用户ID: {}", currentUser.getId());
            throw new RuntimeException("创建用户失败");
        }
    }

    @Override
    public User adminUpdateUser(User currentUser, Integer id, User user) {
        logger.info("管理员更新用户请求，操作用户ID: {}, 目标用户ID: {}", currentUser.getId(), id);
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法更新用户，操作用户ID: {}, 权限等级: {}", currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        User existingUser = this.getById(id);
        if (existingUser == null) {
            logger.warn("更新用户失败：用户不存在，用户ID: {}", id);
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户信息
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            // 检查邮箱是否已被其他用户使用
            QueryWrapper<User> emailQuery = new QueryWrapper<>();
            emailQuery.eq("email", user.getEmail()).ne("id", id);
            if (this.count(emailQuery) > 0) {
                logger.warn("更新用户失败：邮箱已被其他用户使用，邮箱: {}", user.getEmail());
                throw new RuntimeException("邮箱已被其他用户使用");
            }
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPermission() != null) {
            existingUser.setPermission(user.getPermission());
        }
        
        boolean result = this.updateById(existingUser);
        if (result) {
            logger.info("管理员更新用户成功，用户ID: {}, 操作用户ID: {}", id, currentUser.getId());
            return existingUser;
        } else {
            logger.error("管理员更新用户失败，操作用户ID: {}", currentUser.getId());
            throw new RuntimeException("更新用户失败");
        }
    }

    @Override
    public boolean adminDeleteUser(User currentUser, Integer id) {
        logger.info("管理员删除用户请求，操作用户ID: {}, 目标用户ID: {}", currentUser.getId(), id);
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法删除用户，操作用户ID: {}, 权限等级: {}", currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        // 不能删除自己
        if (currentUser.getId().equals(id)) {
            logger.warn("删除用户失败：不能删除自己，用户ID: {}", id);
            throw new RuntimeException("不能删除自己的账户");
        }
        
        User userToDelete = this.getById(id);
        if (userToDelete == null) {
            logger.warn("删除用户失败：用户不存在，用户ID: {}", id);
            throw new RuntimeException("用户不存在");
        }
        
        boolean result = this.removeById(id);
        if (result) {
            logger.info("管理员删除用户成功，用户ID: {}, 操作用户ID: {}", id, currentUser.getId());
            return true;
        } else {
            logger.error("管理员删除用户失败，操作用户ID: {}", currentUser.getId());
            throw new RuntimeException("删除用户失败");
        }
    }

    @Override
    public PageResult<User> adminListUsers(User currentUser, UserQueryRequest queryRequest) {
        logger.info("管理员获取用户列表请求，操作用户ID: {}, 查询条件: {}", currentUser.getId(), queryRequest);
        
        // 权限检查
        if (currentUser.getPermission() <= PERMISSION_THRESHOLD) {
            logger.warn("权限不足，无法获取用户列表，操作用户ID: {}, 权限等级: {}", currentUser.getId(), currentUser.getPermission());
            throw new RuntimeException("权限不足，需要权限等级大于" + PERMISSION_THRESHOLD);
        }
        
        // 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        // 关键词搜索
        if (queryRequest.getKeyword() != null && !queryRequest.getKeyword().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like("user_no", queryRequest.getKeyword())
                .or().like("username", queryRequest.getKeyword())
                .or().like("email", queryRequest.getKeyword()));
        }
        
        // 权限过滤
        if (queryRequest.getPermission() != null) {
            queryWrapper.eq("permission", queryRequest.getPermission());
        }
        
        // 排序
        queryWrapper.orderByDesc("id");
        
        // 分页查询
        Page<User> page = new Page<>(queryRequest.getPage(), queryRequest.getSize());
        Page<User> resultPage = this.page(page, queryWrapper);
        
        PageResult<User> pageResult = new PageResult<>();
        pageResult.setRecords(resultPage.getRecords());
        pageResult.setTotal(resultPage.getTotal());
        pageResult.setPage(Math.toIntExact(resultPage.getCurrent()));
        pageResult.setSize(Math.toIntExact(resultPage.getSize()));
        pageResult.setPages(Math.toIntExact(resultPage.getPages()));
        
        logger.info("管理员获取用户列表成功，返回 {} 条记录，操作用户ID: {}", resultPage.getRecords().size(), currentUser.getId());
        return pageResult;
    }
}