package top.thexiaola.dreamhwhub.support.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.module.permission.service.PermissionService;
import top.thexiaola.dreamhwhub.module.school.constant.SchoolMemberRole;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.support.security.dto.UpdateSensitiveOperationsRequest;
import top.thexiaola.dreamhwhub.support.security.entity.UserSensitiveOperation;
import top.thexiaola.dreamhwhub.support.security.mapper.UserSensitiveOperationMapper;
import top.thexiaola.dreamhwhub.support.security.vo.SensitiveOperationSettingVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 敏感操作验证设置服务
 * <p>
 * 维护「用户 → 需要二次验证的敏感操作」关系。采用「只存关闭项」模型：
 * <ul>
 *   <li>默认所有可用操作都要求二次验证；</li>
 *   <li>用户关闭某操作的验证 → 插入一行；重新开启 → 删除该行；</li>
 *   <li>用户对某操作不再可用（如失去老师身份）→ 删除该行，自动「重置为默认（启用验证）」，</li>
 *   <li>前端只展示对当前用户可用的操作开关。</li>
 * </ul>
 * 只读路径（列出设置）顺带清理不再可用的历史行，避免残留脏数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveOperationSettingsService {

    private final UserSensitiveOperationMapper userSensitiveOperationMapper;
    private final PermissionService permissionService;
    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final SchoolMemberMapper schoolMemberMapper;

    /**
     * 查询该用户已关闭二次验证的操作标识集合（供拦截器判断是否跳过验证）
     *
     * @param userId 用户 ID
     * @return 已关闭的操作标识集合
     */
    public Set<String> getDisabledKeys(Integer userId) {
        if (userId == null) {
            return Set.of();
        }
        QueryWrapper<UserSensitiveOperation> query = new QueryWrapper<>();
        query.eq("user_id", userId).select("operation_key");
        return userSensitiveOperationMapper.selectList(query).stream()
                .map(UserSensitiveOperation::getOperationKey)
                .collect(Collectors.toSet());
    }

    /**
     * 判断某操作对该用户是否仍需二次验证
     * <p>
     * 与 {@link #listSettings} 同一口径：该操作对用户**已不可用**时（如老师资格被取消后
     * 的班级操作），即便库中还残留关闭行，也一律按默认「需要验证」处理——从而确保
     * 「失去权限 = 重置为默认（启用验证）」在**执行点**即生效，而不依赖用户再次打开设置页。
     *
     * @param userId 用户 ID
     * @param key    操作标识
     * @return true-需要验证（默认）；false-用户已关闭且该操作仍对其可用
     */
    public boolean isVerificationRequired(Integer userId, String key) {
        if (key == null || !SensitiveOperations.exists(key)) {
            return true;
        }
        // 操作对该用户不再可用：忽略残留关闭行，按默认（需要验证）处理
        if (!isAvailable(userId, key)) {
            return true;
        }
        return !getDisabledKeys(userId).contains(key);
    }

    /**
     * 列出该用户可见的敏感操作及其验证开关（并清理不再可用的历史行）
     *
     * @param userId 用户 ID
     * @return 操作设置列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SensitiveOperationSettingVO> listSettings(Integer userId) {
        Set<String> disabled = getDisabledKeys(userId);

        List<SensitiveOperationSettingVO> result = new ArrayList<>();
        List<String> unavailableKeys = new ArrayList<>();
        for (SensitiveOperations.Operation op : SensitiveOperations.ALL) {
            boolean available = isAvailable(userId, op.key());
            if (!available) {
                // 不可用的操作：无需展示开关，且关闭状态应被清掉以「重置为默认」
                if (disabled.contains(op.key())) {
                    unavailableKeys.add(op.key());
                }
                result.add(new SensitiveOperationSettingVO(op.key(), op.name(), op.description(), false, false));
                continue;
            }
            boolean enabled = !disabled.contains(op.key());
            result.add(new SensitiveOperationSettingVO(op.key(), op.name(), op.description(), enabled, true));
        }

        if (!unavailableKeys.isEmpty()) {
            QueryWrapper<UserSensitiveOperation> delete = new QueryWrapper<>();
            delete.eq("user_id", userId).in("operation_key", unavailableKeys);
            int removed = userSensitiveOperationMapper.delete(delete);
            log.info("Cleared {} unavailable sensitive-operation setting(s) for user {}", removed, userId);
        }
        return result;
    }

    /**
     * 更新用户对各敏感操作的验证开关（仅允许配置对该用户可用的操作）
     *
     * @param userId  用户 ID
     * @param request 逐项目标状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSettings(Integer userId, UpdateSensitiveOperationsRequest request) {
        if (request == null || request.getSettings() == null || request.getSettings().isEmpty()) {
            throw new BusinessException(BusinessErrorCode.SECURITY_VERIFICATION_SETTING_INVALID,
                    "请至少提交一项操作验证设置", null);
        }
        for (UpdateSensitiveOperationsRequest.Item item : request.getSettings()) {
            if (item == null || item.getKey() == null || item.getEnabled() == null) {
                throw new BusinessException(BusinessErrorCode.SECURITY_VERIFICATION_SETTING_INVALID,
                        "操作验证设置项不完整", null);
            }
            String key = item.getKey();
            if (!SensitiveOperations.exists(key)) {
                throw new BusinessException(BusinessErrorCode.SENSITIVE_OPERATION_NOT_AVAILABLE,
                        "未登记的敏感操作：" + key, null);
            }
            // 越权防护：只能配置对当前用户可用的操作
            if (!isAvailable(userId, key)) {
                throw new BusinessException(BusinessErrorCode.SENSITIVE_OPERATION_NOT_AVAILABLE,
                        "该操作对你的账号不可用，无法配置：" + SensitiveOperations.nameOf(key), null);
            }

            if (Boolean.TRUE.equals(item.getEnabled())) {
                // 开启（默认态）：删除关闭行
                QueryWrapper<UserSensitiveOperation> delete = new QueryWrapper<>();
                delete.eq("user_id", userId).eq("operation_key", key);
                userSensitiveOperationMapper.delete(delete);
            } else {
                // 关闭：插入关闭行（幂等，已存在则跳过）
                QueryWrapper<UserSensitiveOperation> exists = new QueryWrapper<>();
                exists.eq("user_id", userId).eq("operation_key", key);
                if (userSensitiveOperationMapper.selectCount(exists) == 0) {
                    UserSensitiveOperation entity = new UserSensitiveOperation();
                    entity.setUserId(userId);
                    entity.setOperationKey(key);
                    userSensitiveOperationMapper.insert(entity);
                }
            }
        }
        log.info("User {} updated sensitive-operation verification settings", userId);
    }

    /**
     * 判断某敏感操作对用户是否可用：即该用户当前是否具备执行该操作的资格
     * （权限节点或成员身份），不可用时前端不展示其开关。
     *
     * @param userId 用户 ID
     * @param key    操作标识
     * @return true-可用
     */
    public boolean isAvailable(Integer userId, String key) {
        if (userId == null || key == null) {
            return false;
        }
        return switch (key) {
            // 解散班级：拥有解散权限，或自己创建了仍可管理的（未冻结）班级
            case SensitiveOperations.CLASS_DISSOLVE ->
                    hasNode(userId, PermissionNodes.CLASS_DISSOLVE) || ownsActiveClass(userId);
            // 踢出成员：拥有踢出权限，或担任某班老师/仍可管理的班级创建者
            case SensitiveOperations.CLASS_KICK_MEMBER ->
                    hasNode(userId, PermissionNodes.CLASS_MEMBER_KICK)
                            || ownsActiveClass(userId) || isTeacherInAnyClass(userId);
            // 退出班级：是某个（非自己创建的）班级的成员
            case SensitiveOperations.CLASS_LEAVE -> isMemberOfAnyClass(userId);
            // 转让班级：自己创建了仍可管理的（未冻结）班级
            case SensitiveOperations.CLASS_TRANSFER -> ownsActiveClass(userId);
            // 退出学校：是某个学校的（非管理员）成员
            case SensitiveOperations.SCHOOL_LEAVE -> isMemberOfAnySchool(userId);
            // 解散学校
            case SensitiveOperations.SCHOOL_DISSOLVE -> hasNode(userId, PermissionNodes.SCHOOL_DISSOLVE);
            // 指派学校管理员
            case SensitiveOperations.SCHOOL_ASSIGN_ADMIN -> hasNode(userId, PermissionNodes.SCHOOL_ADMIN_ASSIGN);
            // 删除权限组
            case SensitiveOperations.PERMISSION_GROUP_DELETE -> hasNode(userId, PermissionNodes.PERMISSION_GROUP_DELETE);
            // 设置权限组节点
            case SensitiveOperations.PERMISSION_GROUP_SET_NODES -> hasNode(userId, PermissionNodes.PERMISSION_GROUP_EDIT);
            // 分配权限组
            case SensitiveOperations.PERMISSION_GROUP_ASSIGN -> hasNode(userId, PermissionNodes.PERMISSION_GROUP_ASSIGN);
            // 分配权限节点
            case SensitiveOperations.PERMISSION_USER_ASSIGN -> hasNode(userId, PermissionNodes.PERMISSION_USER_ASSIGN);
            // 删除用户
            case SensitiveOperations.USER_DELETE -> hasNode(userId, PermissionNodes.USER_DELETE);
            // 封禁/解封用户
            case SensitiveOperations.USER_BAN -> hasNode(userId, PermissionNodes.USER_BAN);
            // 设置平台管理员身份
            case SensitiveOperations.USER_SET_OP -> hasNode(userId, PermissionNodes.USER_SET_OP);
            // 撤回提交：是某个（非自己创建的）班级的成员，可提交/管理作业
            case SensitiveOperations.SUBMISSION_WITHDRAW -> isMemberOfAnyClass(userId);
            default -> false;
        };
    }

    private boolean hasNode(Integer userId, String node) {
        return permissionService.hasPermission(userId, node);
    }

    /**
     * 用户是否创建了至少一个「仍可管理」的班级。
     * <p>
     * 与 {@code ClassAccessResolver.isClassFrozen} 同口径：创建者必须是平台管理员，
     * 或在班级所属学校仍持有老师及以上的身份；否则班级视为已冻结，原创建者不再能管理，
     * 相应的「解散班级 / 踢出成员 / 转让班级」等开关应对其隐藏。
     */
    private boolean ownsActiveClass(Integer userId) {
        QueryWrapper<ClassInfo> query = new QueryWrapper<>();
        query.eq("owner_id", userId).select("id", "school_id");
        List<ClassInfo> owned = classInfoMapper.selectList(query);
        if (owned.isEmpty()) {
            return false;
        }
        // 平台管理员持有的班级不因学校身份变化而冻结
        if (permissionService.isOp(userId)) {
            return true;
        }
        // 未关联学校的班级按可管理处理（与单班判定一致）
        List<Integer> schoolIds = owned.stream()
                .map(ClassInfo::getSchoolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (schoolIds.size() < owned.size()) {
            return true;
        }
        // 只要在任一所属学校仍具老师及以上身份，即视为仍可管理
        QueryWrapper<SchoolMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId)
                .in("school_id", schoolIds)
                .ge("role", SchoolMemberRole.TEACHER)
                .select("id")
                .last("LIMIT 1");
        return schoolMemberMapper.selectCount(memberQuery) > 0;
    }

    /** 用户是否创建了至少一个班级 */
    private boolean ownsAnyClass(Integer userId) {
        QueryWrapper<ClassInfo> query = new QueryWrapper<>();
        query.eq("owner_id", userId).select("id").last("LIMIT 1");
        return classInfoMapper.selectCount(query) > 0;
    }

    /** 用户是否在至少一个班级中担任老师（班级成员 role=1） */
    private boolean isTeacherInAnyClass(Integer userId) {
        QueryWrapper<ClassMember> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("role", 1).select("id").last("LIMIT 1");
        return classMemberMapper.selectCount(query) > 0;
    }

    /**
     * 用户是否是某个（非自己创建的）班级的成员。
     * 创建者在成员表中 role=1，但不应视为「可退出班级 / 可撤回提交」，故需排除其创建的班级。
     */
    private boolean isMemberOfAnyClass(Integer userId) {
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId).select("class_id");
        List<Integer> memberClassIds = classMemberMapper.selectList(memberQuery).stream()
                .map(ClassMember::getClassId)
                .distinct()
                .toList();
        if (memberClassIds.isEmpty()) {
            return false;
        }
        QueryWrapper<ClassInfo> ownerQuery = new QueryWrapper<>();
        ownerQuery.eq("owner_id", userId).in("id", memberClassIds).select("id");
        Set<Integer> ownedIds = classInfoMapper.selectList(ownerQuery).stream()
                .map(ClassInfo::getId)
                .collect(Collectors.toSet());
        return memberClassIds.stream().anyMatch(id -> !ownedIds.contains(id));
    }

    /** 用户是否是某个学校的非管理员成员（学生或老师） */
    private boolean isMemberOfAnySchool(Integer userId) {
        QueryWrapper<SchoolMember> query = new QueryWrapper<>();
        query.eq("user_id", userId).ne("role", SchoolMemberRole.ADMIN).select("id").last("LIMIT 1");
        return schoolMemberMapper.selectCount(query) > 0;
    }
}
