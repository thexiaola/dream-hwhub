package top.thexiaola.dreamhwhub.module.login.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.dto.DeleteAccountRequest;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.login.service.AccountDeletionService;
import top.thexiaola.dreamhwhub.module.message.entity.PrivateMessage;
import top.thexiaola.dreamhwhub.module.message.entity.SiteMessage;
import top.thexiaola.dreamhwhub.module.message.entity.StrangerMessageQuota;
import top.thexiaola.dreamhwhub.module.message.entity.UserFriend;
import top.thexiaola.dreamhwhub.module.message.mapper.PrivateMessageMapper;
import top.thexiaola.dreamhwhub.module.message.mapper.SiteMessageMapper;
import top.thexiaola.dreamhwhub.module.message.mapper.StrangerMessageQuotaMapper;
import top.thexiaola.dreamhwhub.module.message.mapper.UserFriendMapper;
import top.thexiaola.dreamhwhub.module.permission.entity.UserPermissionGroup;
import top.thexiaola.dreamhwhub.module.permission.entity.UserPermissionNode;
import top.thexiaola.dreamhwhub.module.permission.mapper.UserPermissionGroupMapper;
import top.thexiaola.dreamhwhub.module.permission.mapper.UserPermissionNodeMapper;
import top.thexiaola.dreamhwhub.module.school.constant.SchoolMemberRole;
import top.thexiaola.dreamhwhub.module.school.entity.School;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolJoinApplication;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolJoinApplicationMapper;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMapper;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.entity.*;
import top.thexiaola.dreamhwhub.module.work_management.mapper.*;
import top.thexiaola.dreamhwhub.module.work_management.service.support.WorkSubmissionCleaner;
import top.thexiaola.dreamhwhub.support.password.PasswordUtil;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;
import top.thexiaola.dreamhwhub.support.storage.AvatarStorageService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 账号注销服务实现类
 * <p>
 * 注销流程（单事务）：
 * <ol>
 *   <li>凭登录密码验证账号所有者身份；</li>
 *   <li>校验是否存在阻断注销的身份（平台管理员、班级创建者、学校管理员）；</li>
 *   <li>退出全部班级与学校，并软删除其作业提交与附件（与「被踢出班级」同一套清理规则）；</li>
 *   <li>清理待处理的邀请/申请、好友、私信、站内信、权限绑定与头像文件；</li>
 *   <li>最后删除用户记录。</li>
 * </ol>
 * 前端随后清除本地 Token 即完成登出（JWT 无状态）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionServiceImpl implements AccountDeletionService {

    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final AvatarStorageService avatarStorageService;
    private final UserLookupSupport userLookup;

    private final WorkSubmissionCleaner workSubmissionCleaner;

    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final ClassJoinApplicationMapper classJoinApplicationMapper;
    private final ClassUserInvitationMapper classUserInvitationMapper;
    private final ClassInvitationMapper classInvitationMapper;
    private final ClassTeacherApprovalMapper classTeacherApprovalMapper;
    private final ClassTakeoverApplicationMapper classTakeoverApplicationMapper;

    private final SchoolMapper schoolMapper;
    private final SchoolMemberMapper schoolMemberMapper;
    private final SchoolJoinApplicationMapper schoolJoinApplicationMapper;

    private final UserPermissionGroupMapper userPermissionGroupMapper;
    private final UserPermissionNodeMapper userPermissionNodeMapper;

    private final SiteMessageMapper siteMessageMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final StrangerMessageQuotaMapper strangerMessageQuotaMapper;
    private final UserFriendMapper userFriendMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyAccount(DeleteAccountRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        // 回库取最新用户，确保密码哈希为最新值
        User user = userMapper.selectById(currentUser.getId());
        if (user == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_FOUND, "用户不存在", null);
        }
        Integer userId = user.getId();

        // 1. 验证账号所有者身份：必须提供正确的登录密码
        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(BusinessErrorCode.INVALID_OLD_PASSWORD, "密码错误，无法验证身份", null);
        }

        // 2. 阻断性身份校验
        if (Boolean.TRUE.equals(user.getIsOp())) {
            throw new BusinessException(BusinessErrorCode.ACCOUNT_DELETION_FORBIDDEN,
                    "平台管理员账号不能注销，请先由其他平台管理员取消平台管理员身份", null);
        }
        requireNoOwnedClasses(userId);
        requireNoManagedSchools(userId);

        // 3. 退出全部班级与学校，并清理作业提交
        leaveAllClassesAndSchools(userId);

        // 4. 清理流程类数据、好友、私信、站内信与权限绑定
        clearPendingRequests(userId);
        clearSocialData(userId);
        clearPermissionBindings(userId);

        // 5. 删除头像文件与用户记录
        String avatar = user.getAvatar();
        userMapper.deleteById(userId);
        avatarStorageService.delete(avatar);

        log.info("User {} (id: {}) deleted their own account", user.getUsername(), userId);
    }

    /**
     * 班级创建者不能注销：否则会留下无主班级，需先转让或解散其班级
     */
    private void requireNoOwnedClasses(Integer userId) {
        QueryWrapper<ClassInfo> ownerQuery = new QueryWrapper<>();
        ownerQuery.eq("owner_id", userId).select("id", "class_name");
        List<ClassInfo> ownedClasses = classInfoMapper.selectList(ownerQuery);
        if (!ownedClasses.isEmpty()) {
            String names = ownedClasses.stream()
                    .map(ClassInfo::getClassName)
                    .filter(Objects::nonNull)
                    .limit(3)
                    .reduce((a, b) -> a + "、\"" + b + "\"")
                    .orElse("");
            throw new BusinessException(BusinessErrorCode.ACCOUNT_DELETION_FORBIDDEN,
                    "你仍是班级\"" + names + "\"的创建者，请先转让或解散其班级后再注销", null);
        }
    }

    /**
     * 学校管理员不能注销：其身份由平台管理员指派，需先解除
     */
    private void requireNoManagedSchools(Integer userId) {
        QueryWrapper<SchoolMember> adminQuery = new QueryWrapper<>();
        adminQuery.eq("user_id", userId).eq("role", SchoolMemberRole.ADMIN);
        if (schoolMemberMapper.selectCount(adminQuery) > 0) {
            throw new BusinessException(BusinessErrorCode.ACCOUNT_DELETION_FORBIDDEN,
                    "你仍是学校管理员，请先联系平台管理员解除该身份后再注销", null);
        }
    }

    /**
     * 退出用户所在的全部班级与学校，并软删除其在相关班级的作业提交与附件。
     * <p>
     * 学校成员身份会随其所在学校一并退出；同一用户可能属于多所学校，故按学校逐个清理。
     */
    private void leaveAllClassesAndSchools(Integer userId) {
        // 用户加入的全部学校
        QueryWrapper<SchoolMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("user_id", userId);
        List<SchoolMember> memberships = schoolMemberMapper.selectList(memberQuery);
        Set<Integer> schoolIds = new LinkedHashSet<>();
        for (SchoolMember m : memberships) {
            if (m.getSchoolId() != null) {
                schoolIds.add(m.getSchoolId());
            }
        }

        // 收集需要清理作业提交的班级：学校下的全部班级 + 用户直接加入的班级
        Set<Integer> classIds = new LinkedHashSet<>();
        if (!schoolIds.isEmpty()) {
            QueryWrapper<ClassInfo> schoolClassQuery = new QueryWrapper<>();
            schoolClassQuery.in("school_id", schoolIds).select("id");
            classInfoMapper.selectList(schoolClassQuery)
                    .forEach(c -> classIds.add(c.getId()));
        }
        QueryWrapper<ClassMember> classMemberQuery = new QueryWrapper<>();
        classMemberQuery.eq("user_id", userId).select("class_id");
        classMemberMapper.selectList(classMemberQuery)
                .forEach(cm -> { if (cm.getClassId() != null) classIds.add(cm.getClassId()); });

        // 软删除该用户在这些班级的全部作业提交与附件（与「被踢出班级」同一套规则，一次 SQL）
        workSubmissionCleaner.cleanupClassSubmissions(classIds, userId);

        // 删除成员记录：一次性删除其在所有学校与所有班级的记录
        if (!schoolIds.isEmpty()) {
            QueryWrapper<SchoolMember> deleteSchoolMembers = new QueryWrapper<>();
            deleteSchoolMembers.eq("user_id", userId);
            schoolMemberMapper.delete(deleteSchoolMembers);
        }
        QueryWrapper<ClassMember> deleteClassMembers = new QueryWrapper<>();
        deleteClassMembers.eq("user_id", userId);
        classMemberMapper.delete(deleteClassMembers);
    }

    /**
     * 清理与用户相关的待处理流程数据（邀请、申请、审核、接管）
     */
    private void clearPendingRequests(Integer userId) {
        // 入班申请
        classJoinApplicationMapper.delete(new QueryWrapper<ClassJoinApplication>().eq("applicant_id", userId));
        // 用户邀请（作为邀请人或被邀请人）
        classUserInvitationMapper.delete(new QueryWrapper<ClassUserInvitation>()
                .eq("inviter_id", userId).or().eq("invitee_id", userId));
        // 教师邀请（作为邀请人或被邀请人）
        classInvitationMapper.delete(new QueryWrapper<ClassInvitation>()
                .eq("inviter_id", userId).or().eq("invitee_user_id", userId));
        // 教师审核（被邀请人）
        classTeacherApprovalMapper.delete(new QueryWrapper<ClassTeacherApproval>().eq("invitee_id", userId));
        // 班级接管申请
        classTakeoverApplicationMapper.delete(new QueryWrapper<ClassTakeoverApplication>().eq("applicant_id", userId));
        // 入校申请
        schoolJoinApplicationMapper.delete(new QueryWrapper<SchoolJoinApplication>().eq("applicant_id", userId));
    }

    /**
     * 清理社交数据：站内信、私信、陌生私信配额、好友关系
     */
    private void clearSocialData(Integer userId) {
        // 我收到的站内信
        siteMessageMapper.delete(new QueryWrapper<SiteMessage>().eq("recipient_id", userId));
        // 与我相关的私信（我发出或我收到，任一方向）
        privateMessageMapper.delete(new QueryWrapper<PrivateMessage>()
                .eq("sender_id", userId).or().eq("receiver_id", userId));
        // 我作为发送方或接收方的陌生私信配额
        strangerMessageQuotaMapper.delete(new QueryWrapper<StrangerMessageQuota>()
                .eq("sender_id", userId).or().eq("receiver_id", userId));
        // 我参与的好友关系（我发起或我接收）
        userFriendMapper.delete(new QueryWrapper<UserFriend>()
                .eq("requester_id", userId).or().eq("addressee_id", userId));
    }

    /**
     * 清理权限绑定（权限组与直接授权节点）
     */
    private void clearPermissionBindings(Integer userId) {
        userPermissionGroupMapper.delete(new QueryWrapper<UserPermissionGroup>().eq("user_id", userId));
        userPermissionNodeMapper.delete(new QueryWrapper<UserPermissionNode>().eq("user_id", userId));
    }
}
