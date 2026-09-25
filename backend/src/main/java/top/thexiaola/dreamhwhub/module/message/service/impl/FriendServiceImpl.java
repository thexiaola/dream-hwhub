package top.thexiaola.dreamhwhub.module.message.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.message.entity.UserFriend;
import top.thexiaola.dreamhwhub.module.message.mapper.UserFriendMapper;
import top.thexiaola.dreamhwhub.module.message.service.FriendService;
import top.thexiaola.dreamhwhub.module.message.vo.AddableUserInfo;
import top.thexiaola.dreamhwhub.module.message.vo.FriendInfo;
import top.thexiaola.dreamhwhub.module.message.vo.FriendRequestInfo;
import top.thexiaola.dreamhwhub.module.school.constant.SchoolMemberRole;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.mapper.SchoolMemberMapper;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 好友服务实现类
 * <p>
 * 好友按学校隔离，只允许添加同校用户；所有筛选/查询都下推数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserFriendMapper userFriendMapper;
    private final UserMapper userMapper;
    private final SchoolMemberMapper schoolMemberMapper;
    private final SchoolService schoolService;
    private final UserLookupSupport userLookup;

    @Override
    public List<FriendInfo> listFriends(Integer schoolId) {
        User currentUser = userLookup.requireCurrentUser();
        requireSameSchool(schoolId, currentUser.getId());

        // 已接受的好友：我可能是 requester 或 addressee
        QueryWrapper<UserFriend> query = new QueryWrapper<>();
        query.eq("school_id", schoolId)
                .eq("status", 1)
                .and(w -> w.eq("requester_id", currentUser.getId())
                        .or().eq("addressee_id", currentUser.getId()))
                .orderByDesc("update_time");
        List<UserFriend> relations = userFriendMapper.selectList(query);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        // 好友用户 ID（取关系中的另一方）
        List<Integer> friendIds = relations.stream()
                .map(r -> Objects.equals(r.getRequesterId(), currentUser.getId())
                        ? r.getAddresseeId() : r.getRequesterId())
                .distinct()
                .toList();

        Map<Integer, User> userMap = loadUsers(friendIds);
        Map<Integer, SchoolMember> memberMap = schoolService.getMembersByUserIds(schoolId, friendIds);

        return relations.stream()
                .map(r -> {
                    Integer friendId = Objects.equals(r.getRequesterId(), currentUser.getId())
                            ? r.getAddresseeId() : r.getRequesterId();
                    User friend = userMap.get(friendId);
                    SchoolMember member = memberMap.get(friendId);
                    return new FriendInfo(
                            r.getId(),
                            schoolId,
                            friendId,
                            friend != null ? friend.getUsername() : null,
                            friend != null ? friend.getAvatar() : null,
                            member != null ? member.getRealName() : null,
                            member != null ? member.getStaffNo() : null,
                            member != null ? SchoolMemberRole.nameOf(member.getRole()) : null,
                            r.getUpdateTime() != null ? r.getUpdateTime() : r.getCreateTime());
                })
                .toList();
    }

    @Override
    public List<FriendRequestInfo> listPendingRequests(Integer schoolId) {
        User currentUser = userLookup.requireCurrentUser();
        requireSameSchool(schoolId, currentUser.getId());

        QueryWrapper<UserFriend> query = new QueryWrapper<>();
        query.eq("school_id", schoolId)
                .eq("addressee_id", currentUser.getId())
                .eq("status", 0)
                .orderByDesc("create_time");
        List<UserFriend> relations = userFriendMapper.selectList(query);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> requesterIds = relations.stream()
                .map(UserFriend::getRequesterId)
                .distinct()
                .toList();
        Map<Integer, User> userMap = loadUsers(requesterIds);
        Map<Integer, SchoolMember> memberMap = schoolService.getMembersByUserIds(schoolId, requesterIds);
        String schoolName = schoolService.getSchoolNames(List.of(schoolId)).get(schoolId);

        return relations.stream()
                .map(r -> {
                    User requester = userMap.get(r.getRequesterId());
                    SchoolMember member = memberMap.get(r.getRequesterId());
                    return new FriendRequestInfo(
                            r.getId(),
                            schoolId,
                            schoolName,
                            r.getRequesterId(),
                            requester != null ? requester.getUsername() : null,
                            member != null ? member.getRealName() : null,
                            member != null ? member.getStaffNo() : null,
                            requester != null ? requester.getAvatar() : null,
                            r.getCreateTime());
                })
                .toList();
    }

    @Override
    public List<AddableUserInfo> searchAddableUsers(Integer schoolId, String keyword) {
        User currentUser = userLookup.requireCurrentUser();
        requireSameSchool(schoolId, currentUser.getId());

        // 校内用户查询：按学校成员表过滤（可带姓名/学工号/用户名关键字），排除自己；
        // 条件全部下推数据库
        QueryWrapper<SchoolMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("school_id", schoolId).ne("user_id", currentUser.getId());
        if (StrUtil.isNotBlank(keyword)) {
            String kw = escapeLike(keyword.trim());
            memberQuery.and(w -> w.apply("real_name LIKE {0} ESCAPE '!'", "%" + kw + "%")
                    .or().apply("staff_no LIKE {0} ESCAPE '!'", "%" + kw + "%")
                    .or().apply("user_id IN (SELECT id FROM user WHERE username LIKE {0} ESCAPE '!')",
                            "%" + kw + "%"));
        }
        memberQuery.orderByAsc("id");
        List<SchoolMember> members = schoolMemberMapper.selectList(memberQuery);
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> userIds = members.stream().map(SchoolMember::getUserId).distinct().toList();
        Map<Integer, User> userMap = loadUsers(userIds);

        // 我与这些用户的关系（一次查询，避免 N+1）
        Map<Integer, UserFriend> relationMap = loadRelations(schoolId, currentUser.getId(), userIds);

        return members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    UserFriend relation = relationMap.get(member.getUserId());
                    return new AddableUserInfo(
                            member.getUserId(),
                            schoolId,
                            user != null ? user.getUsername() : null,
                            user != null ? user.getAvatar() : null,
                            member.getRealName(),
                            member.getStaffNo(),
                            SchoolMemberRole.nameOf(member.getRole()),
                            relationOf(relation, currentUser.getId()));
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestFriend(Integer schoolId, Integer targetUserId) {
        User currentUser = userLookup.requireCurrentUser();
        if (targetUserId == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "请选择要添加的用户", null);
        }
        if (Objects.equals(targetUserId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.FRIEND_SELF, "不能添加自己为好友", null);
        }
        if (!schoolService.isSchoolMember(schoolId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_SCHOOL, "你不是该学校的成员", null);
        }
        if (!schoolService.isSchoolMember(schoolId, targetUserId)) {
            throw new BusinessException(BusinessErrorCode.FRIEND_SCHOOL_MISMATCH,
                    "只能添加同一学校内的用户为好友", null);
        }

        // 是否已存在关系（任一方向）
        QueryWrapper<UserFriend> existQuery = new QueryWrapper<>();
        existQuery.eq("school_id", schoolId)
                .and(w -> w.eq("requester_id", currentUser.getId()).eq("addressee_id", targetUserId)
                        .or().eq("requester_id", targetUserId).eq("addressee_id", currentUser.getId()));
        UserFriend exist = userFriendMapper.selectOne(existQuery);
        if (exist != null) {
            if (Integer.valueOf(1).equals(exist.getStatus())) {
                throw new BusinessException(BusinessErrorCode.FRIEND_ALREADY, "你们已经是好友", null);
            }
            if (Integer.valueOf(0).equals(exist.getStatus())) {
                // 若对方已向我发起申请，视为直接同意
                boolean incoming = Objects.equals(exist.getRequesterId(), targetUserId);
                if (incoming) {
                    exist.setStatus(1);
                    userFriendMapper.updateById(exist);
                    return;
                }
                throw new BusinessException(BusinessErrorCode.FRIEND_REQUEST_PENDING, "已存在待处理的好友申请", null);
            }
            // 之前被拒绝：复用该关系记录重新发起
            exist.setRequesterId(currentUser.getId());
            exist.setAddresseeId(targetUserId);
            exist.setStatus(0);
            userFriendMapper.updateById(exist);
            return;
        }

        UserFriend relation = new UserFriend();
        relation.setSchoolId(schoolId);
        relation.setRequesterId(currentUser.getId());
        relation.setAddresseeId(targetUserId);
        relation.setStatus(0);
        userFriendMapper.insert(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void respondRequest(Integer relationId, Boolean accepted) {
        User currentUser = userLookup.requireCurrentUser();
        UserFriend relation = userFriendMapper.selectById(relationId);
        if (relation == null || !Objects.equals(relation.getAddresseeId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.FRIEND_NOT_FOUND, "好友关系不存在", null);
        }
        if (!Integer.valueOf(0).equals(relation.getStatus())) {
            throw new BusinessException(BusinessErrorCode.FRIEND_NOT_FOUND, "该申请已处理", null);
        }
        relation.setStatus(Boolean.TRUE.equals(accepted) ? 1 : 2);
        userFriendMapper.updateById(relation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFriend(Integer relationId) {
        User currentUser = userLookup.requireCurrentUser();
        UserFriend relation = userFriendMapper.selectById(relationId);
        if (relation == null
                || (!Objects.equals(relation.getRequesterId(), currentUser.getId())
                    && !Objects.equals(relation.getAddresseeId(), currentUser.getId()))) {
            throw new BusinessException(BusinessErrorCode.FRIEND_NOT_FOUND, "好友关系不存在", null);
        }
        userFriendMapper.deleteById(relationId);
    }

    // ===== 私有辅助 =====

    private void requireSameSchool(Integer schoolId, Integer userId) {
        if (schoolId == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "请选择学校", null);
        }
        if (!schoolService.isSchoolMember(schoolId, userId)) {
            throw new BusinessException(BusinessErrorCode.NOT_IN_SCHOOL, "你不是该学校的成员", null);
        }
    }

    /** 批量加载用户 */
    private Map<Integer, User> loadUsers(Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<User> query = new QueryWrapper<>();
        query.in("id", userIds).select("id", "username", "avatar");
        return userMapper.selectList(query).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    /** 一次查询取出我与目标用户集合的关系（任一方向），key 为对方用户 ID */
    private Map<Integer, UserFriend> loadRelations(Integer schoolId, Integer me, Collection<Integer> others) {
        if (others == null || others.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<UserFriend> query = new QueryWrapper<>();
        query.eq("school_id", schoolId)
                .and(w -> w.eq("requester_id", me).in("addressee_id", others)
                        .or().in("requester_id", others).eq("addressee_id", me));
        Map<Integer, UserFriend> map = new HashMap<>();
        for (UserFriend r : userFriendMapper.selectList(query)) {
            Integer peer = Objects.equals(r.getRequesterId(), me) ? r.getAddresseeId() : r.getRequesterId();
            map.put(peer, r);
        }
        return map;
    }

    /** 我与某用户的关系描述 */
    private String relationOf(UserFriend relation, Integer me) {
        if (relation == null) {
            return "none";
        }
        if (Integer.valueOf(1).equals(relation.getStatus())) {
            return "friend";
        }
        if (Integer.valueOf(0).equals(relation.getStatus())) {
            return Objects.equals(relation.getRequesterId(), me) ? "pending_out" : "pending_in";
        }
        return "none";
    }

    /** 转义 LIKE 通配符（配合 SQL 的 ESCAPE '!'） */
    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
