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
import top.thexiaola.dreamhwhub.module.admin.dto.AdminUserSearchCondition;
import top.thexiaola.dreamhwhub.module.admin.dto.AdminUserSearchRequest;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

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

    /**
     * 模糊匹配片段
     */
    private static final String LIKE_FUZZY = "LIKE {0} ESCAPE '!'";

    /**
     * 精确匹配片段
     */
    private static final String EXACT_MATCH = "= {0}";

    @Override
    public Page<AdminUserVO> listUsers(AdminUserSearchRequest request, Integer pageNum, Integer pageSize) {
        QueryWrapper<User> query = new QueryWrapper<>();
        // 只查询展示所需字段，不把密码等敏感列带到内存
        query.select("id", "username", "email", "phone", "is_op", "is_banned", "ban_reason",
                "register_time", "last_login_time");

        // 按 CNKI 语义分组：连续的「并且」归为一组，组间用「或者」，整体在数据库中执行
        List<List<AdminUserSearchCondition>> groups = groupConditions(request.getConditions());
        if (groups.size() == 1) {
            // 单组无需额外嵌套，直接挂条件
            groups.get(0).forEach(condition -> applyCondition(query, condition));
        } else if (groups.size() > 1) {
            query.and(outer -> {
                for (int g = 0; g < groups.size(); g++) {
                    List<AdminUserSearchCondition> group = groups.get(g);
                    // 组内条件默认以 AND 连接；组间用 OR 起头，直接带出整组，
                    // 不能先调 or() 再调 and(...)，相邻连接符会被去重成 AND
                    Consumer<QueryWrapper<User>> groupPredicate = inner -> {
                        for (AdminUserSearchCondition condition : group) {
                            applyCondition(inner, condition);
                        }
                    };
                    if (g == 0) {
                        outer.and(groupPredicate);
                    } else {
                        outer.or(groupPredicate);
                    }
                }
            });
        }
        query.orderByDesc("id");

        Page<User> userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), query);
        Page<AdminUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 把条件行按 CNKI 语义分组：连续的「并且」为一组，遇到「或者」则开新组
     */
    private List<List<AdminUserSearchCondition>> groupConditions(List<AdminUserSearchCondition> conditions) {
        List<List<AdminUserSearchCondition>> groups = new ArrayList<>();
        if (conditions == null) {
            return groups;
        }
        boolean carryOr = false;
        for (AdminUserSearchCondition condition : conditions) {
            if (condition == null || StrUtil.isBlank(condition.getValue())) {
                // 被跳过的空条件若带「或者」，其连接意图需留给下一个有效条件，
                // 避免 A 或 (空) 且 C 被当成 A 且 C
                if (condition != null && condition.isOrConnector()) {
                    carryOr = true;
                }
                continue;
            }
            if (groups.isEmpty() || condition.isOrConnector() || carryOr) {
                groups.add(new ArrayList<>());
            }
            carryOr = false;
            groups.get(groups.size() - 1).add(condition);
        }
        return groups;
    }

    /**
     * 把单个条件翻译成 SQL 谓词：模糊匹配走 LIKE，精确匹配走等值，全部使用参数化占位符
     */
    private void applyCondition(QueryWrapper<User> wrapper, AdminUserSearchCondition condition) {
        boolean equalsMatch = condition.isEqualsMatch();
        String value = condition.getValue().trim();
        String field = condition.getField();
        if (field == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "不支持的搜索字段", null);
        }
        switch (field) {
            case "username" -> addColumnPredicate(wrapper, "username", equalsMatch, value);
            case "email" -> addColumnPredicate(wrapper, "email", equalsMatch, value);
            case "staffNo" -> addSubQueryPredicate(wrapper,
                    "id IN (SELECT user_id FROM school_member WHERE staff_no %s)", equalsMatch, value);
            case "realName" -> addSubQueryPredicate(wrapper,
                    "id IN (SELECT user_id FROM school_member WHERE real_name %s)", equalsMatch, value);
            case "school" -> addSubQueryPredicate(wrapper,
                    "id IN (SELECT sm.user_id FROM school_member sm JOIN school s ON s.id = sm.school_id "
                            + "WHERE s.school_name %s)",
                    equalsMatch, value);
            case "className" -> addSubQueryPredicate(wrapper,
                    "id IN (SELECT cm.user_id FROM class_member cm JOIN class_info c ON c.id = cm.class_id "
                            + "WHERE c.class_name %s)",
                    equalsMatch, value);
            default -> throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "不支持的搜索字段", null);
        }
    }

    /**
     * 主表字段的条件：精确匹配直接用列比较，username/email 的 collation 本就不区分大小写，
     * 无需 LOWER()，以便用上唯一索引
     */
    private void addColumnPredicate(QueryWrapper<User> wrapper, String column, boolean equalsMatch, String value) {
        String operator = equalsMatch ? EXACT_MATCH : LIKE_FUZZY;
        wrapper.apply(column + " " + operator, equalsMatch ? value : likePattern(value));
    }

    /**
     * 跨表字段的条件，用子查询命中主表 id
     *
     * @param subQueryPattern 代码内的固定子查询片段，含一个 %s 位置留给匹配片段；
     *                        用户输入只作为参数占位传入，不进入 SQL 片段
     */
    private void addSubQueryPredicate(QueryWrapper<User> wrapper, String subQueryPattern,
                                      boolean equalsMatch, String value) {
        String operator = equalsMatch ? EXACT_MATCH : LIKE_FUZZY;
        wrapper.apply(String.format(subQueryPattern, operator), equalsMatch ? value : likePattern(value));
    }

    /**
     * 转义 LIKE 通配符并补上前后模糊匹配的通配符，避免用户输入的 %、_、! 被当作通配符
     */
    private String likePattern(String value) {
        return "%" + value.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";
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
