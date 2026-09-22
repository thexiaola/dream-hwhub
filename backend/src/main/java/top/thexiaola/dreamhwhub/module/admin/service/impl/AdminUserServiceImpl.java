package top.thexiaola.dreamhwhub.module.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.admin.dto.AdminCreateUserRequest;
import top.thexiaola.dreamhwhub.module.admin.dto.AdminUpdateUserRequest;
import top.thexiaola.dreamhwhub.module.admin.service.AdminUserService;
import top.thexiaola.dreamhwhub.module.admin.vo.AdminUserVO;
import top.thexiaola.dreamhwhub.module.admin.vo.UserPermissionDetailVO;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.permission.entity.PermissionGroup;
import top.thexiaola.dreamhwhub.module.permission.service.PermissionService;
import top.thexiaola.dreamhwhub.module.work_management.entity.*;
import top.thexiaola.dreamhwhub.module.work_management.mapper.*;
import top.thexiaola.dreamhwhub.support.password.PasswordUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 管理员用户管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;
    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final ClassJoinApplicationMapper classJoinApplicationMapper;
    private final ClassUserInvitationMapper classUserInvitationMapper;
    private final ClassTeacherApprovalMapper classTeacherApprovalMapper;
    private final ClassInvitationMapper classInvitationMapper;
    private final PermissionService permissionService;
    private final PasswordUtil passwordUtil;

    @Override
    public Page<AdminUserVO> listUsers(String keyword, Integer pageNum, Integer pageSize) {
        QueryWrapper<User> query = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            query.and(q -> q.like("username", kw)
                    .or().like("email", kw));
        }
        query.orderByDesc("id");

        Page<User> userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), query);
        Page<AdminUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO createUser(AdminCreateUserRequest request) {
        if (isUsernameTaken(request.getUsername(), null)) {
            throw new BusinessException(BusinessErrorCode.USERNAME_EXISTS, "用户名已被占用", null);
        }
        if (isEmailTaken(request.getEmail(), null)) {
            throw new BusinessException(BusinessErrorCode.EMAIL_EXISTS, "邮箱已被占用", null);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setIsOp(false);
        user.setIsBanned(false);
        LocalDateTime now = LocalDateTime.now();
        user.setRegisterTime(now);
        user.setLastLoginTime(now);

        userMapper.insert(user);
        // 应用默认权限组（如有）
        permissionService.applyDefaultGroups(user.getId());

        log.info("Admin created user {} (id: {})", user.getUsername(), user.getId());
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO updateUser(Integer userId, AdminUpdateUserRequest request) {
        User user = requireUser(userId);

        if (StrUtil.isNotBlank(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
            if (isUsernameTaken(request.getUsername(), userId)) {
                throw new BusinessException(BusinessErrorCode.USERNAME_EXISTS, "用户名已被占用", null);
            }
            user.setUsername(request.getUsername());
        }
        if (StrUtil.isNotBlank(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (isEmailTaken(request.getEmail(), userId)) {
                throw new BusinessException(BusinessErrorCode.EMAIL_EXISTS, "邮箱已被占用", null);
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (StrUtil.isNotBlank(request.getPassword())) {
            user.setPassword(passwordUtil.encode(request.getPassword()));
        }

        userMapper.updateById(user);
        log.info("Admin updated user {} (id: {})", user.getUsername(), user.getId());
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Integer userId) {
        User user = requireUser(userId);

        if (userId.equals(UserUtils.getCurrentUserId())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "不能删除当前登录的账号", null);
        }
        if (Boolean.TRUE.equals(user.getIsOp())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "不能删除平台管理员账号，请先取消其平台管理员身份", null);
        }

        // 班级创建者不能直接删除，避免留下无主班级
        QueryWrapper<ClassInfo> ownerQuery = new QueryWrapper<>();
        ownerQuery.eq("owner_id", userId);
        if (classInfoMapper.selectCount(ownerQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "该用户仍是班级创建者，请先转让或解散其班级", null);
        }

        // 清理权限绑定与班级成员关系
        permissionService.setUserGroups(userId, List.of());
        permissionService.setUserDirectNodes(userId, List.of());

        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId);
        classMemberMapper.delete(memberQuery);

        // 清理该用户参与的邀请/申请等流程数据，避免留下孤儿记录
        QueryWrapper<ClassJoinApplication> joinQuery = new QueryWrapper<>();
        joinQuery.eq("applicant_id", userId);
        classJoinApplicationMapper.delete(joinQuery);

        QueryWrapper<ClassUserInvitation> userInvitationQuery = new QueryWrapper<>();
        userInvitationQuery.eq("inviter_id", userId).or().eq("invitee_id", userId);
        classUserInvitationMapper.delete(userInvitationQuery);

        QueryWrapper<ClassTeacherApproval> approvalQuery = new QueryWrapper<>();
        approvalQuery.eq("invitee_id", userId);
        classTeacherApprovalMapper.delete(approvalQuery);

        QueryWrapper<ClassInvitation> invitationQuery = new QueryWrapper<>();
        invitationQuery.eq("inviter_id", userId).or().eq("invitee_user_id", userId);
        classInvitationMapper.delete(invitationQuery);

        userMapper.deleteById(userId);
        log.info("Admin deleted user {} (id: {})", user.getUsername(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO setBanned(Integer userId, boolean banned, String reason) {
        User user = requireUser(userId);

        if (banned && userId.equals(UserUtils.getCurrentUserId())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "不能封禁当前登录的账号", null);
        }

        UpdateWrapper<User> update = new UpdateWrapper<>();
        update.eq("id", userId)
                .set("is_banned", banned)
                .set("ban_reason", banned ? reason : null);
        userMapper.update(null, update);

        user.setIsBanned(banned);
        user.setBanReason(banned ? reason : null);
        log.info("Admin set user {} (id: {}) banned={}", user.getUsername(), userId, banned);
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO setOp(Integer userId, boolean isOp) {
        User user = requireUser(userId);

        if (!isOp && userId.equals(UserUtils.getCurrentUserId())) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "不能取消自己的平台管理员身份", null);
        }

        permissionService.setOp(userId, isOp);
        user.setIsOp(isOp);
        log.info("Admin set user {} (id: {}) isOp={}", user.getUsername(), userId, isOp);
        return toVO(user);
    }

    @Override
    public UserPermissionDetailVO getPermissionDetail(Integer userId) {
        return buildPermissionDetail(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserPermissionDetailVO setUserGroups(Integer userId, Collection<Integer> groupIds) {
        User user = requireUser(userId);
        permissionService.setUserGroups(userId, groupIds);
        return buildPermissionDetail(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserPermissionDetailVO setUserNodes(Integer userId, List<String> nodes) {
        User user = requireUser(userId);
        permissionService.setUserDirectNodes(userId, nodes);
        return buildPermissionDetail(user);
    }

    /**
     * 组装用户权限明细
     */
    private UserPermissionDetailVO buildPermissionDetail(User user) {
        UserPermissionDetailVO detail = new UserPermissionDetailVO();
        detail.setUserId(user.getId());
        detail.setUsername(user.getUsername());
        detail.setIsOp(Boolean.TRUE.equals(user.getIsOp()));
        detail.setGroups(permissionService.getUserGroupVOs(user.getId()));
        detail.setDirectNodes(permissionService.getUserDirectNodes(user.getId()));
        detail.setPermissions(permissionService.getPermissionNodes(user.getId()));
        return detail;
    }

    /**
     * 用户实体转 VO（附带权限组与生效节点）
     */
    private AdminUserVO toVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setIsOp(Boolean.TRUE.equals(user.getIsOp()));
        vo.setIsBanned(Boolean.TRUE.equals(user.getIsBanned()));
        vo.setBanReason(user.getBanReason());
        vo.setRegisterTime(user.getRegisterTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setGroupNames(permissionService.getUserGroups(user.getId()).stream()
                .map(PermissionGroup::getName)
                .toList());
        vo.setPermissions(permissionService.getPermissionNodes(user.getId()));
        return vo;
    }

    /**
     * 查询用户，不存在则抛异常
     */
    private User requireUser(Integer userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }
        return user;
    }

    /**
     * 用户名是否已被占用（不区分大小写）
     *
     * @param excludeUserId 排除的用户 ID（编辑时排除自身），新增传 null
     */
    private boolean isUsernameTaken(String username, Integer excludeUserId) {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.apply("LOWER(username) = LOWER({0})", username);
        if (excludeUserId != null) {
            query.ne("id", excludeUserId);
        }
        return userMapper.selectCount(query) > 0;
    }

    /**
     * 邮箱是否已被占用
     *
     * @param excludeUserId 排除的用户 ID（编辑时排除自身），新增传 null
     */
    private boolean isEmailTaken(String email, Integer excludeUserId) {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("email", email);
        if (excludeUserId != null) {
            query.ne("id", excludeUserId);
        }
        return userMapper.selectCount(query) > 0;
    }
}
