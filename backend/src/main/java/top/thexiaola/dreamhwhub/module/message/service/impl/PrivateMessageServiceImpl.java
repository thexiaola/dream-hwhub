package top.thexiaola.dreamhwhub.module.message.service.impl;

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
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.message.entity.PrivateMessage;
import top.thexiaola.dreamhwhub.module.message.entity.StrangerMessageQuota;
import top.thexiaola.dreamhwhub.module.message.entity.UserFriend;
import top.thexiaola.dreamhwhub.module.message.mapper.PrivateMessageMapper;
import top.thexiaola.dreamhwhub.module.message.mapper.StrangerMessageQuotaMapper;
import top.thexiaola.dreamhwhub.module.message.mapper.UserFriendMapper;
import top.thexiaola.dreamhwhub.module.message.service.MessagePolicyService;
import top.thexiaola.dreamhwhub.module.message.service.PrivateMessageService;
import top.thexiaola.dreamhwhub.module.message.vo.ConversationInfo;
import top.thexiaola.dreamhwhub.module.message.vo.PrivateMessageInfo;
import top.thexiaola.dreamhwhub.module.school.constant.SchoolMemberRole;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 私信服务实现类
 * <p>
 * 私信按学校隔离；非好友互发时施行陌生私信配额（条数上限 + 重置时间）。
 * 会话聚合、未读计数等全部下推数据库，不在内存加工。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl implements PrivateMessageService {

    private final PrivateMessageMapper privateMessageMapper;
    private final UserFriendMapper userFriendMapper;
    private final StrangerMessageQuotaMapper quotaMapper;
    private final UserMapper userMapper;
    private final SchoolService schoolService;
    private final MessagePolicyService messagePolicyService;
    private final UserLookupSupport userLookup;

    @Override
    public List<ConversationInfo> listConversations(Integer schoolId) {
        User currentUser = userLookup.requireCurrentUser();
        requireSameSchool(schoolId, currentUser.getId());

        // 会话聚合在数据库完成（GROUP BY 对方 + 最近一条 + 未读数）
        List<Map<String, Object>> rows = privateMessageMapper.selectConversationRows(schoolId, currentUser.getId());
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集对方用户 ID、其最近消息 ID、我的未读数（peerId → lastId）
        List<Integer> peerIds = new ArrayList<>();
        Map<Integer, Integer> peerLastId = new HashMap<>();
        Map<Integer, Long> unreadMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer peerId = asInt(row.get("peer_id"));
            Integer lastId = asInt(row.get("last_id"));
            if (peerId == null || lastId == null) {
                continue;
            }
            peerIds.add(peerId);
            peerLastId.put(peerId, lastId);
            unreadMap.put(peerId, asLong(row.get("unread")));
        }
        if (peerIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, User> userMap = loadUsers(peerIds);
        Map<Integer, SchoolMember> memberMap = schoolService.getMembersByUserIds(schoolId, peerIds);
        // 最近消息：按 SQL 已算好的 last_id 一次批量取回，按 id 建索引
        Map<Integer, PrivateMessage> lastById = privateMessageMapper.selectByIds(peerLastId.values()).stream()
                .collect(Collectors.toMap(PrivateMessage::getId, m -> m, (a, b) -> a));
        Set<Integer> friendIds = loadFriendIds(schoolId, currentUser.getId(), peerIds);
        String schoolName = schoolService.getSchoolNames(List.of(schoolId)).get(schoolId);

        List<ConversationInfo> result = new ArrayList<>(peerIds.size());
        for (Integer peerId : peerIds) {
            User peer = userMap.get(peerId);
            SchoolMember member = memberMap.get(peerId);
            PrivateMessage last = lastById.get(peerLastId.get(peerId));
            result.add(new ConversationInfo(
                    schoolId,
                    schoolName,
                    peerId,
                    peer != null ? peer.getUsername() : null,
                    peer != null ? peer.getAvatar() : null,
                    member != null ? member.getRealName() : null,
                    member != null ? member.getStaffNo() : null,
                    member != null ? SchoolMemberRole.nameOf(member.getRole()) : null,
                    friendIds.contains(peerId),
                    last != null ? last.getContent() : null,
                    last != null ? last.getCreateTime() : null,
                    unreadMap.getOrDefault(peerId, 0L)));
        }
        return result;
    }

    @Override
    public Page<PrivateMessageInfo> listMessages(Integer schoolId, Integer peerId, Integer pageNum, Integer pageSize) {
        User currentUser = userLookup.requireCurrentUser();
        requireSameSchool(schoolId, currentUser.getId());
        if (peerId == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "请指定会话对方", null);
        }

        // 会话双方之间的往来消息（两个方向），条件下推数据库
        QueryWrapper<PrivateMessage> query = new QueryWrapper<>();
        query.eq("school_id", schoolId)
                .and(w -> w.eq("sender_id", currentUser.getId()).eq("receiver_id", peerId)
                        .or().eq("sender_id", peerId).eq("receiver_id", currentUser.getId()))
                .orderByDesc("id");

        Page<PrivateMessage> page = privateMessageMapper.selectPage(new Page<>(pageNum, pageSize), query);
        Page<PrivateMessageInfo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(m -> toInfo(m, currentUser.getId()))
                .toList());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivateMessageInfo send(Integer schoolId, Integer receiverId, String content) {
        User currentUser = userLookup.requireCurrentUser();
        requireSameSchool(schoolId, currentUser.getId());
        if (receiverId == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "请选择接收人", null);
        }
        if (Objects.equals(receiverId, currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PRIVATE_MESSAGE_SELF, "不能给自己发送私信", null);
        }
        if (!schoolService.isSchoolMember(schoolId, receiverId)) {
            throw new BusinessException(BusinessErrorCode.PRIVATE_MESSAGE_SCHOOL_MISMATCH,
                    "只能给同一学校内的用户发送私信", null);
        }
        String text = StrUtil.trim(content);
        if (StrUtil.isBlank(text)) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "消息内容不能为空", null);
        }

        // 非好友时校验陌生私信配额（条数上限 + 重置时间）
        if (!isFriend(schoolId, currentUser.getId(), receiverId)) {
            consumeStrangerQuota(schoolId, currentUser.getId(), receiverId);
        }

        PrivateMessage message = new PrivateMessage();
        message.setSchoolId(schoolId);
        message.setSenderId(currentUser.getId());
        message.setReceiverId(receiverId);
        message.setContent(text);
        message.setIsRead(false);
        privateMessageMapper.insert(message);

        return toInfo(message, currentUser.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markConversationRead(Integer schoolId, Integer peerId) {
        User currentUser = userLookup.requireCurrentUser();
        requireSameSchool(schoolId, currentUser.getId());
        if (peerId == null) {
            return;
        }
        UpdateWrapper<PrivateMessage> update = new UpdateWrapper<>();
        update.eq("school_id", schoolId)
                .eq("sender_id", peerId)
                .eq("receiver_id", currentUser.getId())
                .eq("is_read", false)
                .set("is_read", true)
                .set("read_time", LocalDateTime.now());
        privateMessageMapper.update(null, update);
    }

    @Override
    public List<UnreadCount> countUnreadBySchool() {
        User currentUser = userLookup.requireCurrentUser();
        List<Map<String, Object>> rows = privateMessageMapper.countUnreadBySchool(currentUser.getId());
        List<UnreadCount> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Integer schoolId = asInt(row.get("school_id"));
            if (schoolId != null) {
                result.add(new UnreadCount(schoolId, asLong(row.get("cnt"))));
            }
        }
        return result;
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

    /** 是否好友（status=1，任一方向） */
    private boolean isFriend(Integer schoolId, Integer a, Integer b) {
        QueryWrapper<UserFriend> query = new QueryWrapper<>();
        query.eq("school_id", schoolId)
                .eq("status", 1)
                .and(w -> w.eq("requester_id", a).eq("addressee_id", b)
                        .or().eq("requester_id", b).eq("addressee_id", a));
        return userFriendMapper.selectCount(query) > 0;
    }

    /** 一次查询取出一批用户的已成立好友关系（key 为对方用户 ID） */
    private Set<Integer> loadFriendIds(Integer schoolId, Integer me, Collection<Integer> others) {
        if (others == null || others.isEmpty()) {
            return Collections.emptySet();
        }
        QueryWrapper<UserFriend> query = new QueryWrapper<>();
        query.eq("school_id", schoolId).eq("status", 1)
                .and(w -> w.eq("requester_id", me).in("addressee_id", others)
                        .or().in("requester_id", others).eq("addressee_id", me));
        Set<Integer> ids = new HashSet<>();
        for (UserFriend r : userFriendMapper.selectList(query)) {
            ids.add(Objects.equals(r.getRequesterId(), me) ? r.getAddresseeId() : r.getRequesterId());
        }
        return ids;
    }

    /**
     * 消耗一次陌生私信配额：窗口过期则重置计数，再判断是否超限。
     * 配额与重置时间取自「学校覆盖 → 全站默认」。
     */
    private void consumeStrangerQuota(Integer schoolId, Integer senderId, Integer receiverId) {
        int limit = messagePolicyService.resolveStrangerLimit(schoolId);
        int resetHours = messagePolicyService.resolveResetHours(schoolId);
        LocalDateTime now = LocalDateTime.now();

        QueryWrapper<StrangerMessageQuota> query = new QueryWrapper<>();
        query.eq("school_id", schoolId).eq("sender_id", senderId).eq("receiver_id", receiverId);
        StrangerMessageQuota quota = quotaMapper.selectOne(query);

        if (quota == null) {
            StrangerMessageQuota created = new StrangerMessageQuota();
            created.setSchoolId(schoolId);
            created.setSenderId(senderId);
            created.setReceiverId(receiverId);
            created.setUsedCount(1);
            created.setWindowStart(now);
            created.setUpdateTime(now);
            quotaMapper.insert(created);
            return;
        }

        boolean expired = quota.getWindowStart() == null
                || quota.getWindowStart().plusHours(resetHours).isBefore(now);
        if (expired) {
            // 窗口已过：重置计数与窗口起点，可继续发送
            quota.setUsedCount(1);
            quota.setWindowStart(now);
            quota.setUpdateTime(now);
            quotaMapper.updateById(quota);
            return;
        }

        if (quota.getUsedCount() != null && quota.getUsedCount() >= limit) {
            throw new BusinessException(BusinessErrorCode.STRANGER_MESSAGE_LIMIT_EXCEEDED,
                    "向陌生用户发送私信的条数已达上限（" + limit + " 条），"
                            + resetHours + " 小时后可继续发送，或先添加对方为好友", null);
        }

        quota.setUsedCount((quota.getUsedCount() == null ? 0 : quota.getUsedCount()) + 1);
        quota.setUpdateTime(now);
        quotaMapper.updateById(quota);
    }

    private Map<Integer, User> loadUsers(Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<User> query = new QueryWrapper<>();
        query.in("id", userIds).select("id", "username", "avatar");
        return userMapper.selectList(query).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private PrivateMessageInfo toInfo(PrivateMessage m, Integer me) {
        return new PrivateMessageInfo(
                m.getId(),
                m.getSchoolId(),
                m.getSenderId(),
                m.getReceiverId(),
                m.getContent(),
                m.getIsRead(),
                m.getCreateTime(),
                Objects.equals(m.getSenderId(), me));
    }

    private Integer asInt(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    private long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }
}
