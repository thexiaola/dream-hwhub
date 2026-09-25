package top.thexiaola.dreamhwhub.module.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.message.entity.MessagePolicy;
import top.thexiaola.dreamhwhub.module.message.mapper.MessagePolicyMapper;
import top.thexiaola.dreamhwhub.module.message.service.MessagePolicyService;
import top.thexiaola.dreamhwhub.module.message.vo.MessagePolicyInfo;
import top.thexiaola.dreamhwhub.module.permission.constant.PermissionNodes;
import top.thexiaola.dreamhwhub.module.school.entity.School;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMapper;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;

import java.time.LocalDateTime;

/**
 * 私信策略服务实现类
 * <p>
 * 全站默认策略存于 message_policy（单行 id=1）；学校可在 school 表覆盖（null 表示继承）。
 * 缺省值（未落库/未覆盖）为「3 条 / 24 小时」。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePolicyServiceImpl implements MessagePolicyService {

    /** 内置默认：向陌生用户发送私信的条数上限 */
    private static final int DEFAULT_STRANGER_LIMIT = 3;
    /** 内置默认：配额重置小时数 */
    private static final int DEFAULT_RESET_HOURS = 24;
    /** 允许的取值范围 */
    private static final int MIN_LIMIT = 0;
    private static final int MAX_LIMIT = 1000;
    private static final int MIN_RESET_HOURS = 1;
    private static final int MAX_RESET_HOURS = 24 * 365;

    private final MessagePolicyMapper messagePolicyMapper;
    private final SchoolMapper schoolMapper;
    private final SchoolService schoolService;
    private final UserLookupSupport userLookup;

    @Override
    public MessagePolicyInfo getGlobalPolicy() {
        MessagePolicy policy = messagePolicyMapper.selectById(MessagePolicy.GLOBAL_ID);
        int limit = policy != null && policy.getStrangerLimit() != null
                ? policy.getStrangerLimit() : DEFAULT_STRANGER_LIMIT;
        int hours = policy != null && policy.getResetHours() != null
                ? policy.getResetHours() : DEFAULT_RESET_HOURS;
        return new MessagePolicyInfo(limit, hours, false, limit, hours);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessagePolicyInfo updateGlobalPolicy(Integer strangerLimit, Integer resetHours) {
        User currentUser = userLookup.requireCurrentUser();
        // 全站策略仅平台管理员可改：这是强校验，不能用可授予的权限节点替代
        if (!userLookup.isPlatformAdmin(currentUser)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有平台管理员可以修改全站私信策略", null);
        }
        int limit = validateLimit(strangerLimit);
        int hours = validateResetHours(resetHours);

        MessagePolicy policy = messagePolicyMapper.selectById(MessagePolicy.GLOBAL_ID);
        if (policy == null) {
            policy = new MessagePolicy();
            policy.setId(MessagePolicy.GLOBAL_ID);
            policy.setStrangerLimit(limit);
            policy.setResetHours(hours);
            policy.setUpdateTime(LocalDateTime.now());
            messagePolicyMapper.insert(policy);
        } else {
            policy.setStrangerLimit(limit);
            policy.setResetHours(hours);
            policy.setUpdateTime(LocalDateTime.now());
            messagePolicyMapper.updateById(policy);
        }
        MessagePolicyInfo info = getGlobalPolicy();
        log.info("User {} updated global message policy: limit={}, resetHours={}",
                currentUser.getId(), limit, hours);
        return info;
    }

    @Override
    public MessagePolicyInfo getSchoolPolicy(Integer schoolId) {
        requireSchoolExists(schoolId);
        User currentUser = userLookup.requireCurrentUser();
        // 生效策略需该校学校管理员或平台管理员可读；其余成员也能看到「是否好友可发」的提示，故放宽为校内成员可读
        // 但展示用信息不含敏感内容，直接返回生效值即可
        MessagePolicyInfo global = getGlobalPolicy();
        School school = schoolMapper.selectById(schoolId);
        Integer overrideLimit = school != null ? school.getStrangerMessageLimit() : null;
        Integer overrideHours = school != null ? school.getStrangerMessageResetHours() : null;
        boolean overridden = overrideLimit != null || overrideHours != null;
        int limit = overrideLimit != null ? overrideLimit : global.getStrangerLimit();
        int hours = overrideHours != null ? overrideHours : global.getResetHours();
        return new MessagePolicyInfo(limit, hours, overridden,
                global.getStrangerLimit(), global.getResetHours());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessagePolicyInfo updateSchoolPolicy(Integer schoolId, Integer strangerLimit, Integer resetHours) {
        User currentUser = userLookup.requireCurrentUser();
        School school = requireSchoolExists(schoolId);
        // 学校管理员或平台管理员可设置本校覆盖；平台管理员经 school:update 放行
        if (!userLookup.hasPermission(currentUser, PermissionNodes.SCHOOL_UPDATE)
                && !schoolService.isSchoolManager(schoolId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有学校管理员可以设置本校私信策略", null);
        }

        // 传 null 表示清除覆盖、继承全站默认。
        // 注意：MyBatis-Plus 的 updateById 会跳过 null 字段，无法把列清空，
        // 因此这里用 UpdateWrapper.set(...) 显式写入（含 null）
        Integer limit = strangerLimit == null ? null : validateLimit(strangerLimit);
        Integer hours = resetHours == null ? null : validateResetHours(resetHours);
        UpdateWrapper<School> update = new UpdateWrapper<>();
        update.eq("id", schoolId)
                .set("stranger_message_limit", limit)
                .set("stranger_message_reset_hours", hours);
        schoolMapper.update(null, update);
        log.info("User {} updated school {} message policy: limit={}, resetHours={}",
                currentUser.getId(), schoolId, limit, hours);
        return getSchoolPolicy(schoolId);
    }

    @Override
    public int resolveStrangerLimit(Integer schoolId) {
        if (schoolId == null) {
            return getGlobalPolicy().getStrangerLimit();
        }
        School school = schoolMapper.selectById(schoolId);
        if (school != null && school.getStrangerMessageLimit() != null) {
            return school.getStrangerMessageLimit();
        }
        return getGlobalPolicy().getStrangerLimit();
    }

    @Override
    public int resolveResetHours(Integer schoolId) {
        if (schoolId == null) {
            return getGlobalPolicy().getResetHours();
        }
        School school = schoolMapper.selectById(schoolId);
        if (school != null && school.getStrangerMessageResetHours() != null) {
            return school.getStrangerMessageResetHours();
        }
        return getGlobalPolicy().getResetHours();
    }

    // ===== 私有辅助 =====

    private School requireSchoolExists(Integer schoolId) {
        School school = schoolId == null ? null : schoolMapper.selectById(schoolId);
        if (school == null) {
            throw new BusinessException(BusinessErrorCode.SCHOOL_NOT_FOUND, "学校不存在", null);
        }
        return school;
    }

    private int validateLimit(Integer value) {
        if (value == null || value < MIN_LIMIT || value > MAX_LIMIT) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "条数上限需在 " + MIN_LIMIT + " ~ " + MAX_LIMIT + " 之间", null);
        }
        return value;
    }

    private int validateResetHours(Integer value) {
        if (value == null || value < MIN_RESET_HOURS || value > MAX_RESET_HOURS) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                    "重置小时数需在 " + MIN_RESET_HOURS + " ~ " + MAX_RESET_HOURS + " 之间", null);
        }
        return value;
    }
}
