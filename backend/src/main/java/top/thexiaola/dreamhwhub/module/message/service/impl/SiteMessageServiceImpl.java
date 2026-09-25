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
import top.thexiaola.dreamhwhub.module.message.constant.SiteMessageType;
import top.thexiaola.dreamhwhub.module.message.dto.SiteMessageQueryRequest;
import top.thexiaola.dreamhwhub.module.message.entity.SiteMessage;
import top.thexiaola.dreamhwhub.module.message.mapper.SiteMessageMapper;
import top.thexiaola.dreamhwhub.module.message.service.SiteMessageService;
import top.thexiaola.dreamhwhub.module.message.vo.SiteMessageResponse;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 站内信服务实现类
 * <p>
 * 站内信属个人消息，不做权限节点控制，一律以「接收人 = 当前用户」过滤；
 * 所有筛选（学校 / 类型 / 已读 / 关键字）与分页都下推到数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteMessageServiceImpl implements SiteMessageService {

    private final SiteMessageMapper siteMessageMapper;
    private final SchoolService schoolService;
    private final UserLookupSupport userLookup;
    private final ClassMemberMapper classMemberMapper;
    private final ClassInfoMapper classInfoMapper;

    @Override
    public Page<SiteMessageResponse> listMyMessages(SiteMessageQueryRequest request) {
        User currentUser = userLookup.requireCurrentUser();

        QueryWrapper<SiteMessage> query = new QueryWrapper<>();
        // 只查自己的消息；接收人过滤始终存在，保证数据隔离
        query.eq("recipient_id", currentUser.getId());
        if (request.getSchoolId() != null) {
            query.eq("school_id", request.getSchoolId());
        }
        if (StrUtil.isNotBlank(request.getType())) {
            query.eq("type", request.getType().trim());
        }
        if (request.getIsRead() != null) {
            query.eq("is_read", request.getIsRead());
        }
        if (StrUtil.isNotBlank(request.getKeyword())) {
            String kw = escapeLike(request.getKeyword().trim());
            // 标题 / 内容 / 班级名任一命中即可
            query.and(w -> w.apply("title LIKE {0} ESCAPE '!'", "%" + kw + "%")
                    .or().apply("content LIKE {0} ESCAPE '!'", "%" + kw + "%")
                    .or().apply("class_name LIKE {0} ESCAPE '!'", "%" + kw + "%"));
        }
        query.orderByDesc("create_time").orderByDesc("id");

        Page<SiteMessage> page = siteMessageMapper.selectPage(
                new Page<>(request.getPageNum(), request.getPageSize()), query);

        Page<SiteMessageResponse> result = new Page<>(
                page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toResponses(page.getRecords()));
        return result;
    }

    @Override
    public List<UnreadCount> countUnreadBySchool() {
        User currentUser = userLookup.requireCurrentUser();

        QueryWrapper<SiteMessage> query = new QueryWrapper<>();
        query.select("school_id", "COUNT(*) AS cnt")
                .eq("recipient_id", currentUser.getId())
                .eq("is_read", false)
                .groupBy("school_id");
        // 分组统计在数据库完成，避免取出全部未读消息后在内存聚合
        List<Map<String, Object>> rows = siteMessageMapper.selectMaps(query);

        List<UnreadCount> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object schoolId = row.get("school_id");
            Object cnt = row.get("cnt");
            if (schoolId instanceof Number school && cnt instanceof Number count) {
                result.add(new UnreadCount(school.intValue(), count.longValue()));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Integer messageId) {
        User currentUser = userLookup.requireCurrentUser();
        if (messageId == null) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "消息 ID 不能为空", null);
        }

        SiteMessage message = siteMessageMapper.selectById(messageId);
        if (message == null || !Objects.equals(message.getRecipientId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能操作自己的站内信", null);
        }
        if (Boolean.TRUE.equals(message.getIsRead())) {
            return;
        }

        // 仅更新本人这条消息，用 UpdateWrapper 精确限定接收人
        UpdateWrapper<SiteMessage> update = new UpdateWrapper<>();
        update.eq("id", messageId)
                .eq("recipient_id", currentUser.getId())
                .set("is_read", true)
                .set("read_time", LocalDateTime.now());
        siteMessageMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Integer schoolId) {
        User currentUser = userLookup.requireCurrentUser();

        UpdateWrapper<SiteMessage> update = new UpdateWrapper<>();
        update.eq("recipient_id", currentUser.getId())
                .eq("is_read", false);
        if (schoolId != null) {
            update.eq("school_id", schoolId);
        }
        update.set("is_read", true).set("read_time", LocalDateTime.now());
        siteMessageMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyWorkPublished(Integer classId, String className, Integer workId,
            String workTitle, Integer publisherId) {
        if (classId == null || workId == null) {
            return;
        }

        // 收件人＝该班全部成员（含学生与协作老师），排除发布者本人。
        // 排除条件与去重都下推数据库，避免取回成员后又在内存里过滤
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", classId)
                .ne("user_id", publisherId)
                .select("DISTINCT user_id");
        List<Integer> recipientIds = classMemberMapper.selectObjs(memberQuery).stream()
                .filter(Objects::nonNull)
                .map(obj -> ((Number) obj).intValue())
                .toList();
        if (recipientIds.isEmpty()) {
            return;
        }

        // 学校 ID 取自班级，用于站内信按学校隔离
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        Integer schoolId = classInfo != null ? classInfo.getSchoolId() : null;
        if (schoolId == null) {
            log.warn("Skip work-published notification: class {} has no school", classId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String content = "班级「" + (className == null ? "" : className) + "」布置了新作业："
                + (workTitle == null ? "" : workTitle);
        List<SiteMessage> messages = new ArrayList<>(recipientIds.size());
        for (Integer recipientId : recipientIds) {
            SiteMessage message = new SiteMessage();
            message.setRecipientId(recipientId);
            message.setSchoolId(schoolId);
            message.setType(SiteMessageType.WORK_PUBLISHED);
            message.setTitle("新作业：" + (workTitle == null ? "" : workTitle));
            message.setContent(content);
            message.setClassId(classId);
            message.setClassName(className);
            message.setWorkId(workId);
            message.setIsRead(false);
            message.setCreateTime(now);
            messages.add(message);
        }
        // 单条 INSERT 多值批量写入，避免逐条往返
        siteMessageMapper.insertBatch(messages);
        log.info("Work {} published in class {}, notified {} recipients", workId, classId, messages.size());
    }

    /**
     * 批量转换为响应对象（批量补齐学校名称，避免 N+1）
     */
    private List<SiteMessageResponse> toResponses(List<SiteMessage> messages) {
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> schoolIds = messages.stream()
                .map(SiteMessage::getSchoolId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> schoolNames = schoolService.getSchoolNames(schoolIds);

        return messages.stream()
                .map(m -> new SiteMessageResponse(
                        m.getId(),
                        m.getSchoolId(),
                        schoolNames.get(m.getSchoolId()),
                        m.getType(),
                        m.getTitle(),
                        m.getContent(),
                        m.getClassId(),
                        m.getClassName(),
                        m.getWorkId(),
                        m.getIsRead(),
                        m.getReadTime(),
                        m.getCreateTime()))
                .toList();
    }

    /**
     * 转义 LIKE 通配符，避免用户输入的 %、_、! 被当作通配符（配合 SQL 的 ESCAPE '!'）
     */
    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}