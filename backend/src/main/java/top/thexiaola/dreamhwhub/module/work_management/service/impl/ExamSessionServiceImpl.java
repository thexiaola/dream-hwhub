package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.work_management.constant.ExamViolationType;
import top.thexiaola.dreamhwhub.module.work_management.constant.WorkType;
import top.thexiaola.dreamhwhub.module.work_management.dto.ReportViolationRequest;
import top.thexiaola.dreamhwhub.module.work_management.entity.ExamSession;
import top.thexiaola.dreamhwhub.module.work_management.entity.ExamViolation;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkInfo;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ExamSessionMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ExamViolationMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.service.ExamSessionService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkQuestionService;
import top.thexiaola.dreamhwhub.module.work_management.service.support.ExamContentScrambler;
import top.thexiaola.dreamhwhub.module.work_management.service.support.ExamFontRegistry;
import top.thexiaola.dreamhwhub.module.work_management.vo.ExamEnterResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.ExamViolationVO;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkQuestionStudentVO;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 考试会话服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamSessionServiceImpl implements ExamSessionService {

    private final WorkMapper workMapper;
    private final ExamSessionMapper examSessionMapper;
    private final ExamViolationMapper examViolationMapper;
    private final WorkQuestionService workQuestionService;
    private final ClassService classService;
    private final ClassAccessResolver classAccessResolver;
    private final UserMapper userMapper;
    private final ExamFontRegistry examFontRegistry;
    private final ExamContentScrambler examContentScrambler;
    private final UserLookupSupport userLookup;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExamEnterResponse enterExam(Integer workId) {
        User currentUser = userLookup.requireCurrentUser();
        WorkInfo work = requireExam(workId);

        // 仅该班学生（或助理/协作老师）可参加考试；班主任本人不参加
        if (!classService.canSubmitWork(work.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级学生可以参加考试", null);
        }

        ExamSession session = examSessionMapper.selectOne(new QueryWrapper<ExamSession>()
                .eq("work_id", workId).eq("student_id", currentUser.getId()));

        LocalDateTime now = LocalDateTime.now();
        if (session == null) {
            session = new ExamSession();
            session.setWorkId(workId);
            session.setStudentId(currentUser.getId());
            session.setStartTime(now);
            session.setStatus(1);
            session.setViolationCount(0);
            session.setFontSeed(resolveFontSeed(work));
            session.setQuestionOrder(resolveQuestionOrder(work));
            examSessionMapper.insert(session);
        }

        // 题目：按学生题序排序；开启字体映射时打乱文本
        List<WorkQuestionStudentVO> questions = buildStudentQuestions(work, session);

        ExamEnterResponse response = new ExamEnterResponse();
        response.setWorkId(work.getId());
        response.setTitle(work.getTitle());
        response.setDescription(work.getDescription());
        response.setTotalScore(work.getTotalScore());
        response.setStatus(1);
        response.setDurationMinutes(work.getExamDurationMinutes());
        response.setStartTime(session.getStartTime());
        response.setSubmitted(session.getStatus() != null && session.getStatus() != 1);
        response.setViolationCount(session.getViolationCount() == null ? 0 : session.getViolationCount());
        response.setDraftAnswers(session.getDraftAnswers());

        // 限时计算
        if (work.getExamDurationMinutes() != null) {
            LocalDateTime endTime = session.getStartTime().plusMinutes(work.getExamDurationMinutes());
            response.setEndTime(endTime);
            long remaining = Duration.between(now, endTime).getSeconds();
            response.setRemainingSeconds(Math.max(0, remaining));
        }

        // 反作弊配置
        boolean anti = Boolean.TRUE.equals(work.getAntiCheatEnabled());
        response.setAntiCheatEnabled(anti);
        response.setAntiCheatFont(anti && Boolean.TRUE.equals(work.getAntiCheatFont()));
        response.setAntiCheatFullscreen(anti && Boolean.TRUE.equals(work.getAntiCheatFullscreen()));
        response.setAntiCheatNoCopy(anti && Boolean.TRUE.equals(work.getAntiCheatNoCopy()));
        response.setAntiCheatDetectLeave(anti && Boolean.TRUE.equals(work.getAntiCheatDetectLeave()));
        response.setAntiCheatMaxViolations(anti ? work.getAntiCheatMaxViolations() : null);
        response.setFontSeed(Boolean.TRUE.equals(work.getAntiCheatFont()) ? session.getFontSeed() : null);
        response.setQuestions(questions);

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reportViolation(Integer workId, ReportViolationRequest request) {
        User currentUser = userLookup.requireCurrentUser();
        WorkInfo work = requireExam(workId);
        if (!classService.canSubmitWork(work.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "无权上报该考试的违规", null);
        }
        if (!ExamViolationType.isValid(request.getType())) {
            throw new BusinessException(BusinessErrorCode.EXAM_VIOLATION_INVALID,
                    "违规类型不合法：" + request.getType(), null);
        }

        ExamSession session = examSessionMapper.selectOne(new QueryWrapper<ExamSession>()
                .eq("work_id", workId).eq("student_id", currentUser.getId()));
        if (session == null) {
            throw new BusinessException(BusinessErrorCode.EXAM_SESSION_NOT_FOUND, "尚未开始考试", null);
        }
        if (session.getStatus() != null && session.getStatus() != 1) {
            // 已交卷：不再累计违规
            return false;
        }

        ExamViolation violation = new ExamViolation();
        violation.setSessionId(session.getId());
        violation.setWorkId(workId);
        violation.setStudentId(currentUser.getId());
        violation.setType(request.getType());
        violation.setDetail(request.getDetail());
        violation.setOccurTime(LocalDateTime.now());
        examViolationMapper.insert(violation);

        int count = (session.getViolationCount() == null ? 0 : session.getViolationCount()) + 1;
        session.setViolationCount(count);
        session.setUpdateTime(LocalDateTime.now());
        examSessionMapper.updateById(session);

        Integer max = work.getAntiCheatMaxViolations();
        boolean reached = max != null && max > 0 && count >= max;
        log.info("Exam {} student {} violation {} (count={}, max={})",
                workId, currentUser.getId(), request.getType(), count, max);
        return reached;
    }

    @Override
    public void saveDraft(Integer workId, String draft) {
        User currentUser = userLookup.requireCurrentUser();
        WorkInfo work = requireExam(workId);
        if (!classService.canSubmitWork(work.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "无权保存该考试的草稿", null);
        }
        ExamSession session = examSessionMapper.selectOne(new QueryWrapper<ExamSession>()
                .eq("work_id", workId).eq("student_id", currentUser.getId()));
        if (session == null) {
            throw new BusinessException(BusinessErrorCode.EXAM_SESSION_NOT_FOUND, "尚未开始考试", null);
        }
        if (session.getStatus() != null && session.getStatus() != 1) {
            throw new BusinessException(BusinessErrorCode.EXAM_SESSION_FINISHED, "考试已结束", null);
        }
        session.setDraftAnswers(draft);
        session.setUpdateTime(LocalDateTime.now());
        examSessionMapper.updateById(session);
    }

    @Override
    public List<ExamViolationVO> listViolations(Integer workId) {
        User currentUser = userLookup.requireCurrentUser();
        WorkInfo work = requireExam(workId);
        if (!classService.isTeacher(work.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以查看违规记录", null);
        }

        List<ExamViolation> violations = examViolationMapper.selectList(new QueryWrapper<ExamViolation>()
                .eq("work_id", workId).orderByDesc("occur_time").orderByDesc("id"));
        if (violations.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量取学生用户名与班级内身份（姓名/学号）
        Set<Integer> studentIds = new HashSet<>();
        violations.forEach(v -> studentIds.add(v.getStudentId()));
        Map<Integer, User> userMap = new HashMap<>();
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.select("id", "username").in("id", studentIds);
        userMapper.selectList(userQuery).forEach(u -> userMap.put(u.getId(), u));
        Map<Integer, SchoolMember> memberMap = classAccessResolver.loadSchoolMembersByClassId(work.getClassId(), studentIds);

        List<ExamViolationVO> result = new ArrayList<>(violations.size());
        for (ExamViolation v : violations) {
            ExamViolationVO vo = new ExamViolationVO();
            vo.setId(v.getId());
            vo.setSessionId(v.getSessionId());
            vo.setWorkId(v.getWorkId());
            vo.setStudentId(v.getStudentId());
            User u = userMap.get(v.getStudentId());
            vo.setStudentName(u != null ? u.getUsername() : null);
            SchoolMember m = memberMap.get(v.getStudentId());
            if (m != null) {
                vo.setStudentRealName(m.getRealName());
                vo.setStudentNo(m.getStaffNo());
            }
            vo.setType(v.getType());
            vo.setTypeName(ExamViolationType.nameOf(v.getType()));
            vo.setDetail(v.getDetail());
            vo.setOccurTime(v.getOccurTime());
            result.add(vo);
        }
        return result;
    }

    // ===== 私有辅助 =====

    private WorkInfo requireExam(Integer workId) {
        WorkInfo work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }
        if (!WorkType.isExam(work.getWorkType())) {
            throw new BusinessException(BusinessErrorCode.EXAM_NOT_FOUND, "该任务不是考试", null);
        }
        return work;
    }

    /** 为开启字体映射的考试分配一个打乱字体种子 */
    private Integer resolveFontSeed(WorkInfo work) {
        if (!Boolean.TRUE.equals(work.getAntiCheatFont()) || !examFontRegistry.isAvailable()) {
            return null;
        }
        List<Integer> seeds = examFontRegistry.seeds();
        return seeds.get(ThreadLocalRandom.current().nextInt(seeds.size()));
    }

    /** 为开启题目乱序的考试生成一份学生专属题序 */
    private String resolveQuestionOrder(WorkInfo work) {
        if (!Boolean.TRUE.equals(work.getShuffleQuestions())) {
            return null;
        }
        List<Integer> ids = new ArrayList<>();
        for (WorkQuestionStudentVO q : workQuestionService.listForStudent(work.getId())) {
            ids.add(q.getId());
        }
        Collections.shuffle(ids);
        return JSONUtil.toJsonStr(ids);
    }

    /** 组装学生题目：按学生题序排序，并（可选）按字体映射打乱文本 */
    private List<WorkQuestionStudentVO> buildStudentQuestions(WorkInfo work, ExamSession session) {
        List<WorkQuestionStudentVO> questions = workQuestionService.listForStudent(work.getId());

        // 按学生题序排序
        String orderJson = session.getQuestionOrder();
        if (orderJson != null && !orderJson.isBlank()) {
            Map<Integer, Integer> index = new HashMap<>();
            JSONArray arr = JSONUtil.parseArray(orderJson);
            for (int i = 0; i < arr.size(); i++) {
                index.put(arr.getInt(i), i);
            }
            questions.sort(Comparator.comparingInt(q -> index.getOrDefault(q.getId(), Integer.MAX_VALUE)));
        }

        // 字体映射：打乱题干与选项文本
        if (Boolean.TRUE.equals(work.getAntiCheatFont()) && session.getFontSeed() != null) {
            Map<Integer, Integer> mapping = examFontRegistry.mapping(session.getFontSeed());
            if (!mapping.isEmpty()) {
                for (WorkQuestionStudentVO q : questions) {
                    q.setContent(examContentScrambler.scramble(q.getContent(), mapping));
                    if (q.getOptions() != null) {
                        for (WorkQuestionStudentVO.OptionVO opt : q.getOptions()) {
                            opt.setText(examContentScrambler.scramble(opt.getText(), mapping));
                        }
                    }
                }
            }
        }
        return questions;
    }
}
