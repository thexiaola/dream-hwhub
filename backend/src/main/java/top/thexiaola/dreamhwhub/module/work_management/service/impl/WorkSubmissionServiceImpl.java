package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.entity.User;
import top.thexiaola.dreamhwhub.module.login.mapper.UserMapper;
import top.thexiaola.dreamhwhub.module.school.entity.SchoolMember;
import top.thexiaola.dreamhwhub.module.school.service.SchoolService;
import top.thexiaola.dreamhwhub.module.work_management.dto.BatchDownloadAttachmentsRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.GradeAnswersRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.GradeWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.AnswerItem;
import top.thexiaola.dreamhwhub.module.work_management.dto.SubmitWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.constant.QuestionType;
import top.thexiaola.dreamhwhub.module.work_management.constant.WorkType;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.ClassMember;
import top.thexiaola.dreamhwhub.module.work_management.entity.ExamSession;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkAnswer;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkQuestion;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkSubmissionAttachment;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassInfoMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ClassMemberMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkAnswerMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.ExamSessionMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.ClassService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkQuestionService;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkSubmissionService;
import top.thexiaola.dreamhwhub.module.work_management.service.support.AnswerGrader;
import top.thexiaola.dreamhwhub.module.work_management.vo.ClassMemberResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.UnsubmittedStudentResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkAnswerVO;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionSubmitResponse;
import top.thexiaola.dreamhwhub.support.mapper.WorkSubmissionResponseMapper;
import top.thexiaola.dreamhwhub.support.mapper.WorkSubmissionSubmitResponseMapper;
import top.thexiaola.dreamhwhub.support.session.UserLookupSupport;
import top.thexiaola.dreamhwhub.support.validation.FileUploadValidator;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 作业提交服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkSubmissionServiceImpl implements WorkSubmissionService {

    // 文件存储目录（运行目录下的 attachments/submission，提交附件）
    private static final String UPLOAD_DIR = "attachments/submission/";

    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkMapper workMapper;
    private final WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper;
    private final WorkAnswerMapper workAnswerMapper;
    private final ExamSessionMapper examSessionMapper;
    private final WorkQuestionService workQuestionService;
    private final AnswerGrader answerGrader;
    private final ClassService classService;
    private final UserMapper userMapper;
    private final UserLookupSupport userLookup;
    private final ClassInfoMapper classInfoMapper;
    private final ClassMemberMapper classMemberMapper;
    private final SchoolService schoolService;
    private final WorkSubmissionResponseMapper submissionResponseMapper;
    private final WorkSubmissionSubmitResponseMapper submissionSubmitResponseMapper;
    private final TransactionTemplate transactionTemplate;

    @Override
    public WorkSubmissionSubmitResponse submitWork(SubmitWorkRequest request) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询作业
        WorkInfo workInfo = workMapper.selectById(request.getWorkId());
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查权限（班级学生或助理/协作老师可以提交作业，班主任不可提交）
        if (!classService.canSubmitWork(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级学生或助理可以提交作业", null);
        }

        // 检查作业状态（必须是已发布状态才能提交）
        if (!isWorkPublished(workInfo)) {
            throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, "作业未发布或已结束", null);
        }

        // 检查是否已提交过（排除已软删除的记录）
        QueryWrapper<WorkSubmission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("work_id", request.getWorkId())
                .eq("submitter_id", currentUser.getId())
                .eq("is_deleted", false);
        long count = workSubmissionMapper.selectCount(queryWrapper);
        
        if (count > 0) {
            throw new BusinessException(BusinessErrorCode.WORK_ALREADY_SUBMITTED, "您已经提交过该作业", null);
        }

        // 判断是否逾期提交
        boolean isLate = workInfo.getDeadline() != null && LocalDateTime.now().isAfter(workInfo.getDeadline());
        
        // 如果不允许逾期提交且已截止，则拒绝
        if (isLate && Boolean.FALSE.equals(workInfo.getAllowLateSubmit())) {
            throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, "作业已截止，不允许逾期提交", null);
        }

        // 先保存附件文件到磁盘（在事务外执行，避免长时间占用数据库连接）
        List<MultipartFile> attachments = request.getAttachments();
        final Integer submissionId;
        final List<WorkSubmissionAttachment> savedAttachments;
        
        if (CollUtil.isNotEmpty(attachments)) {
            // 预先生成submissionId用于文件命名
            submissionId = generateSubmissionId();
            try {
                savedAttachments = saveSubmissionAttachmentsToDisk(currentUser.getId(), submissionId, attachments);
            } catch (Exception e) {
                log.error("Failed to save submission attachments", e);
                throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, 
                        "文件上传失败，请稍后重试", null);
            }
        } else {
            submissionId = null;
            savedAttachments = new ArrayList<>();
        }
        
        // 在事务中创建提交记录
        return transactionTemplate.execute(status -> {
            try {
                return createSubmissionRecord(request, currentUser, workInfo, isLate, submissionId, savedAttachments);
            } catch (Exception e) {
                // 如果事务失败，清理已上传的文件
                cleanupUploadedFiles(savedAttachments);
                status.setRollbackOnly();
                throw e;
            }
        });
    }
    
    /**
     * 生成临时的提交ID（用于文件命名）
     */
    private Integer generateSubmissionId() {
        // 使用时间戳作为临时ID，实际ID由数据库生成
        return Math.abs(java.util.UUID.randomUUID().hashCode());
    }
    
    /**
     * 将附件保存到磁盘（不包含数据库操作）
     */
    private List<WorkSubmissionAttachment> saveSubmissionAttachmentsToDisk(Integer userId, Integer tempSubmissionId, List<MultipartFile> files) throws Exception {
        List<WorkSubmissionAttachment> attachments = new ArrayList<>();
        
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.contains("..")) {
                throw new BusinessException(BusinessErrorCode.INVALID_FILE_PATH, "非法的文件名", null);
            }
            
            // 生成安全的文件名
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeFileName = tempSubmissionId + "_" + userId + "_" + timestamp + extension;
            
            // 确保上传目录存在（相对路径，基于运行目录）
            Path uploadPath = Paths.get(UPLOAD_DIR).normalize();
            Files.createDirectories(uploadPath);
            
            // 保存文件（相对路径）
            Path filePath = uploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath);
            
            // 获取文件信息
            long fileSize = Files.size(filePath);
            String fileType = FileUploadValidator.detectFileType(filePath.toString());
            
            // 执行完整的安全检查
            FileUploadValidator.performFullSecurityCheck(filePath.toString(), fileSize);
            
            // 创建附件对象（尚未保存到数据库）
            WorkSubmissionAttachment attachment = new WorkSubmissionAttachment();
            attachment.setSubmissionId(tempSubmissionId); // 临时ID
            attachment.setFileName(originalFilename);
            attachment.setFilePath(filePath.toString());
            attachment.setFileSize(fileSize);
            attachment.setFileType(fileType);
            attachment.setUploadTime(LocalDateTime.now());
            
            attachments.add(attachment);
        }
        
        return attachments;
    }
    
    /**
     * 在事务中创建提交记录和附件记录（由TransactionTemplate调用）
     */
    private WorkSubmissionSubmitResponse createSubmissionRecord(
            SubmitWorkRequest request, User currentUser, WorkInfo workInfo, 
            boolean isLate, Integer tempSubmissionId, List<WorkSubmissionAttachment> savedAttachments) {
        
        // 创建提交记录
        WorkSubmission submission = new WorkSubmission();
        submission.setWorkId(request.getWorkId());
        submission.setSubmitterId(currentUser.getId());
        submission.setClassId(workInfo.getClassId());
        submission.setSubmissionContent(request.getSubmissionContent());
        submission.setStatus(1);
        submission.setIsLate(isLate);
        submission.setCreateTime(LocalDateTime.now());
        submission.setUpdateTime(LocalDateTime.now());

        try {
            workSubmissionMapper.insert(submission);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("Duplicate submission attempt for work {} by user {}", request.getWorkId(), currentUser.getId());
            throw new BusinessException(BusinessErrorCode.WORK_ALREADY_SUBMITTED, "您已经提交过该作业", null);
        }
        
        // 保存逐题作答：客观题提交即自动评判记分，主观题/附加题待老师手动评分
        saveAnswers(submission, workInfo, request.getAnswers(), null);
        workSubmissionMapper.updateById(submission);

        // 保存附件记录到数据库（更新为真实的submissionId）
        List<WorkSubmissionSubmitResponse.AttachmentInfo> attachmentInfos = null;
        if (!savedAttachments.isEmpty()) {
            for (WorkSubmissionAttachment attachment : savedAttachments) {
                attachment.setSubmissionId(submission.getId()); // 更新为真实ID
                workSubmissionAttachmentMapper.insert(attachment);
            }
            
            // 转换为响应类型
            attachmentInfos = savedAttachments.stream()
                    .map(att -> new WorkSubmissionSubmitResponse.AttachmentInfo(
                            att.getId(),
                            att.getFileName(),
                            att.getFilePath(),
                            att.getFileSize(),
                            att.getFileType(),
                            att.getUploadTime()
                    ))
                    .collect(Collectors.toList());
        }
        
        // 转换为响应VO
        WorkSubmissionSubmitResponse response = submissionSubmitResponseMapper.toSubmitResponse(submission);
        response.setAttachments(attachmentInfos);

        // 考试：交卷后结束会话（记录交卷时刻并清空草稿）
        if (WorkType.isExam(workInfo.getWorkType())) {
            finishExamSession(workInfo.getId(), currentUser.getId(), 2);
        }

        return response;
    }

    /**
     * 结束考试会话（交卷/超时）。会话不存在时静默跳过（非考试或未开考直接交卷）。
     *
     * @param workId    考试 ID
     * @param studentId 学生 ID
     * @param status    结束状态：2-已交卷，3-超时自动交卷
     */
    private void finishExamSession(Integer workId, Integer studentId, int status) {
        // 交卷后需把 draft_answers 置空；updateById 会忽略 null 字段，
        // 故用 UpdateWrapper 显式 set(col, null)
        UpdateWrapper<ExamSession> update = new UpdateWrapper<>();
        update.eq("work_id", workId).eq("student_id", studentId)
                .set("status", status)
                .set("submit_time", LocalDateTime.now())
                .set("draft_answers", null)
                .set("update_time", LocalDateTime.now());
        examSessionMapper.update(null, update);
    }

    /**
     * 保存（或整体替换）某次提交的逐题作答，并对客观题执行自动评判。
     * <p>
     * 含题目的作业：为每道题落一条 work_answer；客观题按参考答案自动判定对错并给分，
     * 主观题/附加题留给老师手动评分。若本次作答全部为客观题，则汇总自动分作为提交总分并直接置为「已批改」；
     * 只要含主观题，提交保持「已提交」状态待老师评阅。纯文本作业（无题目）走原有逻辑，不做任何处理。
     *
     * @param submission   提交实体（insert 后需再次 updateById 才会落库）
     * @param workInfo     作业信息
     * @param answers      学生逐题作答（可为空）
     * @param replaceOld   true 时先清空该提交已有作答（用于修改提交）
     */
    private void saveAnswers(WorkSubmission submission, WorkInfo workInfo,
                             List<AnswerItem> answers, Boolean replaceOld) {
        Integer workId = workInfo.getId();
        List<WorkQuestion> questions = workQuestionService.listEntities(workId);
        if (questions.isEmpty()) {
            // 纯文本作业：无结构化题目
            return;
        }

        // 修改提交场景：先清空旧作答再重建
        if (Boolean.TRUE.equals(replaceOld)) {
            QueryWrapper<WorkAnswer> deleteQuery = new QueryWrapper<>();
            deleteQuery.eq("submission_id", submission.getId());
            workAnswerMapper.delete(deleteQuery);
        }

        Map<Integer, Object> studentAnswers = new HashMap<>();
        if (answers != null) {
            Set<Integer> validQuestionIds = questions.stream()
                    .map(WorkQuestion::getId)
                    .collect(Collectors.toSet());
            for (AnswerItem item : answers) {
                if (item == null || item.getQuestionId() == null) {
                    continue;
                }
                if (!validQuestionIds.contains(item.getQuestionId())) {
                    throw new BusinessException(BusinessErrorCode.ANSWER_INVALID,
                            "作答包含不属于该作业的题目", null);
                }
                studentAnswers.put(item.getQuestionId(), item.getAnswer());
            }
        }

        List<WorkAnswer> rows = new ArrayList<>(questions.size());
        BigDecimal autoTotal = BigDecimal.ZERO;
        boolean allAutoGradable = true;

        for (WorkQuestion question : questions) {
            Object rawAnswer = studentAnswers.get(question.getId());
            WorkAnswer row = new WorkAnswer();
            row.setSubmissionId(submission.getId());
            row.setQuestionId(question.getId());
            row.setAnswer(answerGrader.toJson(rawAnswer));

            if (QuestionType.isAutoGradable(question.getQuestionType())) {
                AnswerGrader.Result result = answerGrader.grade(question, rawAnswer);
                row.setGradingType("auto");
                row.setIsCorrect(result.correct());
                row.setScore(answerGrader.scoreOf(question, result.correct()));
                autoTotal = autoTotal.add(row.getScore() == null ? BigDecimal.ZERO : row.getScore());
                row.setGradeTime(LocalDateTime.now());
            } else {
                // 主观题/附加题：待老师手动评分
                allAutoGradable = false;
                row.setGradingType(null);
                row.setIsCorrect(null);
                row.setScore(null);
            }
            rows.add(row);
        }

        // 清理可能存在的重复作答（修改提交时已清空；新提交为并发防护），再批量插入
        if (!rows.isEmpty()) {
            workAnswerMapper.insert(rows);
        }

        if (allAutoGradable) {
            // 全部客观题：自动判分即为最终成绩
            submission.setScore(autoTotal);
            submission.setStatus(2);
            submission.setGradeTime(LocalDateTime.now());
        } else {
            // 含主观题：提交后等待老师评阅，暂不写总分
            submission.setStatus(1);
        }
    }

    /**
     * 依据当前作答明细重算提交总分（数据库侧求和），用于老师逐题评分之后。
     */
    private void refreshSubmissionTotal(WorkSubmission submission) {
        BigDecimal total = workAnswerMapper.sumScoreBySubmission(submission.getId());
        submission.setScore(total == null ? BigDecimal.ZERO : total);
        submission.setUpdateTime(LocalDateTime.now());
    }
    
    /**
     * 清理已上传的文件（事务失败时调用）
     */
    private void cleanupUploadedFiles(List<WorkSubmissionAttachment> attachments) {
        for (WorkSubmissionAttachment attachment : attachments) {
            try {
                Path filePath = Paths.get(attachment.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            } catch (Exception e) {
                log.error("Failed to cleanup file: {}", attachment.getFilePath(), e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkSubmissionSubmitResponse updateSubmission(Integer submissionId, String submissionContent,
                                                          List<MultipartFile> attachments,
                                                          List<Integer> removedAttachmentIds,
                                                          List<AnswerItem> answers) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询提交记录（排除已软删除的）
        WorkSubmission submission = workSubmissionMapper.selectById(submissionId);
        if (submission == null || Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }

        // 只能修改自己的提交
        if (!submission.getSubmitterId().equals(currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能修改自己的提交", null);
        }

        // 如果已经被批改且未被老师打回，不能修改
        if (submission.getStatus() == 2) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_ALREADY_GRADED, "作业已被批改，不能修改", null);
        }

        WorkInfo workInfo = workMapper.selectById(submission.getWorkId());

        // 检查是否已过截止时间，学生不能在截止后更新作业（除非被老师打回）
        if (submission.getStatus() != 3) {
            if (workInfo != null && workInfo.getDeadline() != null && LocalDateTime.now().isAfter(workInfo.getDeadline())) {
                throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, "作业已截止，无法修改", null);
            }
        }

        // 1. 处理附件删除
        if (CollUtil.isNotEmpty(removedAttachmentIds)) {
            for (Integer attachmentId : removedAttachmentIds) {
                WorkSubmissionAttachment attachment = workSubmissionAttachmentMapper.selectById(attachmentId);
                if (attachment != null && attachment.getSubmissionId().equals(submissionId)) {
                    // 物理删除文件
                    try {
                        Path filePath = Paths.get(attachment.getFilePath());
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to delete submission attachment file: {}", attachment.getFilePath(), e);
                    }
                    // 删除数据库记录
                    workSubmissionAttachmentMapper.deleteById(attachmentId);
                }
            }
        }

        // 2. 处理新附件上传
        if (CollUtil.isNotEmpty(attachments)) {
            saveSubmissionAttachmentsDirectly(currentUser.getId(), submissionId, attachments);
        }

        // 3. 更新提交内容
        if (submissionContent != null) {
            submission.setSubmissionContent(submissionContent);
        }
        submission.setStatus(1); // 重置为已提交状态
        submission.setUpdateTime(LocalDateTime.now());
        workSubmissionMapper.updateById(submission);

        // 4. 含题目的作业：整体替换逐题作答并重新自动评判（客观题）
        if (workInfo != null) {
            saveAnswers(submission, workInfo, answers, Boolean.TRUE);
            workSubmissionMapper.updateById(submission);
        }

        // 5. 构建响应（包含更新后的附件列表）
        WorkSubmissionSubmitResponse response = submissionSubmitResponseMapper.toSubmitResponse(submission);
        List<WorkSubmissionSubmitResponse.AttachmentInfo> attachmentInfos = getSubmissionAttachmentsForSubmitResponse(submissionId);
        response.setAttachments(attachmentInfos);
        
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubmission(Integer submissionId) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询提交记录（排除已软删除的）
        WorkSubmission submission = workSubmissionMapper.selectById(submissionId);
        if (submission == null || Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }

        // 如果是老师，可以删除任何学生的提交；如果是学生，只能删除自己的提交
        boolean isTeacher = classService.isTeacher(submission.getClassId(), currentUser.getId());
        if (!isTeacher) {
            if (!submission.getSubmitterId().equals(currentUser.getId())) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能删除自己的提交", null);
            }
            
            // 学生不能删除已过截止时间的作业
            WorkInfo workInfo = workMapper.selectById(submission.getWorkId());
            if (workInfo != null && workInfo.getDeadline() != null && LocalDateTime.now().isAfter(workInfo.getDeadline())) {
                throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, "已过截止时间的作业不能删除", null);
            }
        }

        // 清理附件：物理删除服务器文件 + 删除附件数据库记录（撤回后附件不应继续占用存储）
        QueryWrapper<WorkSubmissionAttachment> attachmentQuery = new QueryWrapper<>();
        attachmentQuery.eq("submission_id", submissionId);
        List<WorkSubmissionAttachment> submissionAttachments = workSubmissionAttachmentMapper.selectList(attachmentQuery);

        // 先在事务内删除附件记录并软删提交记录（保证 DB 原子性）
        if (CollUtil.isNotEmpty(submissionAttachments)) {
            workSubmissionAttachmentMapper.delete(attachmentQuery);
        }
        submission.setIsDeleted(true);
        workSubmissionMapper.updateById(submission);

        // 事务主体完成后物理删除服务器文件（异常仅记日志，最坏残留无引用的垃圾文件，
        // 避免反过来出现“提交记录还在、附件文件已丢”的不一致）
        if (CollUtil.isNotEmpty(submissionAttachments)) {
            for (WorkSubmissionAttachment attachment : submissionAttachments) {
                try {
                    Path filePath = Paths.get(attachment.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete submission attachment file on withdraw: {}", attachment.getFilePath(), e);
                }
            }
        }
    }

    @Override
    public WorkSubmissionResponse getSubmissionById(Integer submissionId) {
        User currentUser = userLookup.requireCurrentUser();

        WorkSubmission submission = workSubmissionMapper.selectById(submissionId);
        if (submission == null || Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }

        // 越权防护：仅提交人本人、该班老师/管理员可查看（该班老师对管理员返回 true）
        boolean isOwner = submission.getSubmitterId() != null
                && submission.getSubmitterId().equals(currentUser.getId());
        boolean isClassTeacher = classService.isTeacher(submission.getClassId(), currentUser.getId());
        if (!isOwner && !isClassTeacher) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "无权查看该提交记录", null);
        }
        
        // 转换为WorkSubmissionResponse
        return convertToResponse(submission);
    }

    /**
     * 按班级批量获取成员的学校内身份（姓名/学工号），返回以用户 ID 为键的映射
     */
    private Map<Integer, SchoolMember> loadClassMembers(Integer classId, Collection<Integer> userIds) {
        if (classId == null || userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null || classInfo.getSchoolId() == null) {
            return Collections.emptyMap();
        }
        return schoolService.getMembersByUserIds(classInfo.getSchoolId(), userIds);
    }

    @Override
    public List<WorkSubmissionResponse> getStudentSubmissions(Integer workId) {
        User currentUser = userLookup.requireCurrentUser();

        QueryWrapper<WorkSubmission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("submitter_id", currentUser.getId())
                   .eq("is_deleted", false);
        
        if (workId != null) {
            queryWrapper.eq("work_id", workId);
        }
        
        queryWrapper.orderByDesc("update_time");
        
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(queryWrapper);
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkSubmissionResponse> getSubmittedStudents(Integer workId) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询作业信息
        WorkInfo workInfo = workMapper.selectById(workId);
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查权限（只有班级老师可以查看）
        if (!classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以查看", null);
        }

        // 获取已提交的学生列表（排除已软删除的）
        QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.eq("work_id", workId)
                      .eq("is_deleted", false)
                      .orderByDesc("update_time");
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(submissionQuery);
        
        return submissions.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<UnsubmittedStudentResponse> getUnsubmittedStudents(Integer workId) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询作业信息
        WorkInfo workInfo = workMapper.selectById(workId);
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 检查权限（只有班级老师可以查看）
        if (!classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以查看", null);
        }

        // 未交学生 = 该班学生中尚未提交该作业的人，差集在数据库里用反连接完成
        QueryWrapper<ClassMember> memberQuery = new QueryWrapper<>();
        memberQuery.eq("class_id", workInfo.getClassId())
                .eq("role", 0)
                .apply("user_id NOT IN (SELECT submitter_id FROM work_submission WHERE work_id = {0} AND is_deleted = 0)",
                        workId);
        List<ClassMember> unsubmittedMembers = classMemberMapper.selectList(memberQuery);

        if (unsubmittedMembers.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> allStudentIds = unsubmittedMembers.stream()
            .map(ClassMember::getUserId)
            .collect(Collectors.toSet());

        // 查询未交学生详情（只取展示字段，绝不查询/返回 password 等敏感列）
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.select("id", "username", "email")
                 .in("id", allStudentIds);
        List<User> users = userMapper.selectList(userQuery);

        // 姓名与学号是班级内身份，从班级成员记录中取
        Map<Integer, SchoolMember> memberMap = loadClassMembers(workInfo.getClassId(), allStudentIds);

        return users.stream().map(u -> {
            UnsubmittedStudentResponse resp = new UnsubmittedStudentResponse();
            resp.setId(u.getId());
            resp.setUsername(u.getUsername());
            resp.setEmail(u.getEmail());
            SchoolMember member = memberMap.get(u.getId());
            if (member != null) {
                resp.setStudentName(member.getRealName());
                resp.setStudentNo(member.getStaffNo());
            }
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<WorkSubmissionResponse> getWorkSubmissions(Integer workId, Integer pageNum, Integer pageSize) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 检查权限（只有班级老师可以查看所有提交）
        WorkInfo workInfo = workMapper.selectById(workId);
        if (workInfo == null || !classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以查看所有提交", null);
        }

        QueryWrapper<WorkSubmission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("work_id", workId)
                   .eq("is_deleted", false)
                   .orderByDesc("update_time");
        
        // 使用MyBatisPlus分页
        Page<WorkSubmission> submissionPage = new Page<>(pageNum, pageSize);
        Page<WorkSubmission> pagedResult = workSubmissionMapper.selectPage(submissionPage, queryWrapper);
        
        if (pagedResult.getRecords().isEmpty()) {
            Page<WorkSubmissionResponse> page = new Page<>(pageNum, pageSize, 0);
            page.setRecords(Collections.emptyList());
            return page;
        }
        
        // 批量查询优化 - 收集所有提交ID
        List<Integer> submissionIds = pagedResult.getRecords().stream()
                .map(WorkSubmission::getId)
                .collect(Collectors.toList());
        
        // 批量查询附件
        QueryWrapper<WorkSubmissionAttachment> attQuery = new QueryWrapper<>();
        attQuery.in("submission_id", submissionIds);
        List<WorkSubmissionAttachment> allAttachments = workSubmissionAttachmentMapper.selectList(attQuery);
        
        Map<Integer, List<WorkSubmissionResponse.AttachmentInfo>> attachmentMap = allAttachments.stream()
                .collect(Collectors.groupingBy(
                    WorkSubmissionAttachment::getSubmissionId,
                    Collectors.mapping(att -> new WorkSubmissionResponse.AttachmentInfo(
                        att.getId(),
                        att.getFileName(),
                        att.getFilePath(),
                        att.getFileSize(),
                        att.getFileType(),
                        att.getUploadTime()
                    ), Collectors.toList())
                ));
        
        // 批量查询用户信息（提交人和批改人）
        Set<Integer> userIds = new HashSet<>();
        for (WorkSubmission submission : pagedResult.getRecords()) {
            userIds.add(submission.getSubmitterId());
            if (submission.getGraderId() != null) {
                userIds.add(submission.getGraderId());
            }
        }
        
        Map<Integer, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            QueryWrapper<User> userQuery = new QueryWrapper<>();
            userQuery.in("id", userIds);
            List<User> users = userMapper.selectList(userQuery);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }
        
        // 转换为响应对象，使用缓存的数据（用户信息 + 班级内身份）
        Map<Integer, SchoolMember> memberMap = loadClassMembers(workInfo.getClassId(), userIds);
        final Map<Integer, User> finalUserMap = userMap;
        final Map<Integer, SchoolMember> finalMemberMap = memberMap;
        // 该作业是否含题目（一次判定，复用到列表每一项，供前端决定是否展示"逐题评分"入口）
        final boolean workHasQuestions = workQuestionService.hasQuestions(workInfo.getId());
        List<WorkSubmissionResponse> responses = pagedResult.getRecords().stream()
                .map(submission -> {
                    WorkSubmissionResponse response = convertToResponseWithCache(submission, workInfo, finalUserMap, finalMemberMap);
                    response.setAttachments(attachmentMap.getOrDefault(submission.getId(), new ArrayList<>()));
                    response.setHasQuestions(workHasQuestions);
                    return response;
                })
                .collect(Collectors.toList());

        // 构建分页结果
        Page<WorkSubmissionResponse> page = new Page<>(pageNum, pageSize, pagedResult.getTotal());
        page.setRecords(responses);
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkSubmissionResponse gradeWork(GradeWorkRequest request) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询提交记录（排除已软删除的）
        WorkSubmission submission = workSubmissionMapper.selectById(request.getSubmissionId());
        if (submission == null || Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }

        // 查询作业信息并检查权限（只有班级老师可以批改作业）
        WorkInfo workInfo = workMapper.selectById(submission.getWorkId());
        if (workInfo == null || !classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以批改作业", null);
        }

        // 检查分数范围
        if (request.getScore().compareTo(new java.math.BigDecimal(workInfo.getTotalScore())) > 0) {
            throw new BusinessException(BusinessErrorCode.SCORE_OUT_OF_RANGE, "分数超过作业总分", null);
        }

        // 批改作业（支持重新批改）
        submission.setScore(request.getScore());
        submission.setComment(request.getComment());
        
        // 根据是否打回设置状态
        if (Boolean.TRUE.equals(request.getIsReturned())) {
            submission.setStatus(3); // 已打回，学生可以修改
        } else {
            submission.setStatus(2); // 已批改
        }
        
        submission.setGradeTime(LocalDateTime.now());
        submission.setGraderId(currentUser.getId());
        submission.setUpdateTime(LocalDateTime.now());

        workSubmissionMapper.updateById(submission);
        
        // 转换为WorkSubmissionResponse
        return convertToResponse(submission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkSubmissionResponse gradeAnswers(GradeAnswersRequest request) {
        // 获取当前用户
        User currentUser = userLookup.requireCurrentUser();

        // 查询提交记录（排除已软删除的）
        WorkSubmission submission = workSubmissionMapper.selectById(request.getSubmissionId());
        if (submission == null || Boolean.TRUE.equals(submission.getIsDeleted())) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "提交记录不存在", null);
        }

        // 查询作业信息并检查权限（只有班级老师可以批改作业）
        WorkInfo workInfo = workMapper.selectById(submission.getWorkId());
        if (workInfo == null || !classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以批改作业", null);
        }

        // 该次提交的全部作答（数据库一次查询）
        QueryWrapper<WorkAnswer> answerQuery = new QueryWrapper<>();
        answerQuery.eq("submission_id", submission.getId());
        List<WorkAnswer> answers = workAnswerMapper.selectList(answerQuery);
        if (answers.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.ANSWER_INVALID, "该提交没有逐题作答，无法逐题评分", null);
        }
        Map<Integer, WorkAnswer> answerByQuestion = answers.stream()
                .collect(Collectors.toMap(WorkAnswer::getQuestionId, a -> a));

        // 题目 ID -> 满分与题型（校验评分范围、判定客观题）
        Map<Integer, BigDecimal> scoreByQuestion = new HashMap<>();
        Map<Integer, String> typeByQuestion = new HashMap<>();
        for (WorkQuestion q : workQuestionService.listEntities(workInfo.getId())) {
            scoreByQuestion.put(q.getId(), q.getScore() == null ? BigDecimal.ZERO : q.getScore());
            typeByQuestion.put(q.getId(), q.getQuestionType());
        }

        LocalDateTime now = LocalDateTime.now();
        for (GradeAnswersRequest.GradeItem item : request.getItems()) {
            if (item == null || item.getQuestionId() == null) {
                continue;
            }
            WorkAnswer answer = answerByQuestion.get(item.getQuestionId());
            if (answer == null) {
                throw new BusinessException(BusinessErrorCode.ANSWER_INVALID,
                        "题目不属于该提交：" + item.getQuestionId(), null);
            }
            if (item.getScore() == null || item.getScore().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "分数不能为负", null);
            }
            BigDecimal fullScore = scoreByQuestion.getOrDefault(item.getQuestionId(), BigDecimal.ZERO);
            if (item.getScore().compareTo(fullScore) > 0) {
                throw new BusinessException(BusinessErrorCode.SCORE_OUT_OF_RANGE,
                        "本题得分不能超过该题满分", null);
            }

            // 手动评分（覆盖自动分）；客观题据得分是否等于满分回推对错
            answer.setScore(item.getScore());
            answer.setGradingType("manual");
            answer.setComment(item.getComment());
            answer.setGraderId(currentUser.getId());
            answer.setGradeTime(now);
            if (QuestionType.isObjective(typeByQuestion.get(item.getQuestionId()))) {
                answer.setIsCorrect(item.getScore().compareTo(fullScore) == 0);
            }
            workAnswerMapper.updateById(answer);
        }

        // 按数据库汇总的逐题得分重算总分，并置为「已批改」
        refreshSubmissionTotal(submission);
        submission.setStatus(2);
        submission.setGradeTime(now);
        submission.setGraderId(currentUser.getId());
        workSubmissionMapper.updateById(submission);

        return convertToResponse(submission);
    }

    /**
     * 转换为响应对象（带缓存的作业信息和用户信息）
     */
    private WorkSubmissionResponse convertToResponseWithCache(WorkSubmission submission, WorkInfo cachedWorkInfo,
                                                              Map<Integer, User> userMap,
                                                              Map<Integer, SchoolMember> memberMap) {
        WorkSubmissionResponse response = submissionResponseMapper.toResponse(submission, cachedWorkInfo);
        
        // 从缓存中获取批改人信息
        if (submission.getGraderId() != null) {
            User grader = userMap.get(submission.getGraderId());
            if (grader != null) {
                response.setGraderName(grader.getUsername());
            }
        }
        
        // 从缓存中获取提交人信息；姓名与学号取自班级成员记录
        User submitter = userMap.get(submission.getSubmitterId());
        if (submitter != null) {
            response.setSubmitterName(submitter.getUsername());
            response.setSubmitterUsername(submitter.getUsername());
            response.setSubmitterEmail(submitter.getEmail());
            SchoolMember member = memberMap.get(submitter.getId());
            if (member != null) {
                response.setSubmitterStudentName(member.getRealName());
                response.setSubmitterStudentNo(member.getStaffNo());
            }
        }
        
        return response;
    }
    
    /**
     * 转换为响应对象（用于单个查询）
     */
    private WorkSubmissionResponse convertToResponse(WorkSubmission submission) {
        WorkInfo workInfo = workMapper.selectById(submission.getWorkId());
        WorkSubmissionResponse response = submissionResponseMapper.toResponse(submission, workInfo);

        // 加载附件列表
        List<WorkSubmissionResponse.AttachmentInfo> attachments = getSubmissionAttachments(submission.getId());
        response.setAttachments(attachments);

        // 含题目的作业：填充逐题作答明细
        if (workInfo != null) {
            boolean hasQuestions = workQuestionService.hasQuestions(submission.getWorkId());
            response.setHasQuestions(hasQuestions);
            if (hasQuestions) {
                User currentUser = userLookup.requireCurrentUser();
                boolean isTeacher = classService.isTeacher(workInfo.getClassId(), currentUser.getId());
                response.setAnswers(fillAnswers(submission, isTeacher));
            }
        }

        return response;
    }

    /**
     * 组装某次提交的逐题作答明细。
     * <p>
     * 学生侧仅在其提交被批改（status=2）后才可见参考答案与解析；教师侧始终可见。
     *
     * @param submission 提交实体
     * @param isTeacher  查看者是否该班老师
     * @return 逐题作答明细
     */
    private List<WorkAnswerVO> fillAnswers(WorkSubmission submission, boolean isTeacher) {
        QueryWrapper<WorkAnswer> answerQuery = new QueryWrapper<>();
        answerQuery.eq("submission_id", submission.getId());
        List<WorkAnswer> rows = workAnswerMapper.selectList(answerQuery);

        List<WorkQuestion> questions = workQuestionService.listEntities(submission.getWorkId());
        Map<Integer, WorkQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(WorkQuestion::getId, q -> q));

        // 学生且未批改：不下发参考答案与解析
        boolean revealAnswer = isTeacher || (submission.getStatus() != null && submission.getStatus() == 2);

        List<WorkAnswerVO> result = new ArrayList<>(rows.size());
        for (WorkAnswer row : rows) {
            WorkQuestion q = questionMap.get(row.getQuestionId());
            WorkAnswerVO vo = new WorkAnswerVO();
            vo.setId(row.getId());
            vo.setQuestionId(row.getQuestionId());
            vo.setAnswer(answerGrader.parseJson(row.getAnswer()));
            vo.setScore(row.getScore());
            vo.setIsCorrect(row.getIsCorrect());
            vo.setGradingType(row.getGradingType());
            vo.setComment(row.getComment());
            vo.setGradeTime(row.getGradeTime());
            if (q != null) {
                vo.setOrderNo(q.getOrderNo());
                vo.setQuestionType(q.getQuestionType());
                vo.setQuestionTypeName(QuestionType.nameOf(q.getQuestionType()));
                vo.setContent(q.getContent());
                vo.setFullScore(q.getScore());
                vo.setAutoGradable(QuestionType.isAutoGradable(q.getQuestionType()));
                if (revealAnswer) {
                    vo.setCorrectAnswer(answerGrader.parseJson(q.getCorrectAnswer()));
                    vo.setAnalysis(q.getAnalysis());
                }
            }
            result.add(vo);
        }
        // 按题号排序，保证展示顺序与题干一致
        result.sort(Comparator.comparing(v -> v.getOrderNo() == null ? Integer.MAX_VALUE : v.getOrderNo()));
        return result;
    }
    
    /**
     * 保存提交附件（直接上传的文件）
     * @return 附件信息列表
     */
    private List<WorkSubmissionResponse.AttachmentInfo> saveSubmissionAttachmentsDirectly(Integer userId, Integer submissionId, List<MultipartFile> files) {
        List<WorkSubmissionResponse.AttachmentInfo> attachmentInfos = new ArrayList<>();

        if (CollUtil.isEmpty(files)) {
            return attachmentInfos;
        }

        // 文件需逐个落盘（IO 无法合并），但数据库记录收集后一次性批量插入，避免逐条 insert
        List<WorkSubmissionAttachment> pending = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            try {
                // 1. 获取原始文件名
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || originalFilename.contains("..")) {
                    throw new BusinessException(BusinessErrorCode.INVALID_FILE_PATH, "非法的文件名", null);
                }
                
                // 2. 生成安全的文件名（业务ID-用户ID-时间戳）
                String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                String safeFileName = submissionId + "_" + userId + "_" + timestamp + extension;
                
                // 3. 确保上传目录存在（相对路径，基于运行目录）
                Path uploadPath = Paths.get(UPLOAD_DIR).normalize();
                Files.createDirectories(uploadPath);
                
                // 4. 保存文件（相对路径）
                Path filePath = uploadPath.resolve(safeFileName);
                Files.copy(file.getInputStream(), filePath);
                
                // 5. 获取文件信息
                long fileSize = Files.size(filePath);
                String fileType = FileUploadValidator.detectFileType(filePath.toString());
                
                // 6. 执行完整的安全检查
                FileUploadValidator.performFullSecurityCheck(filePath.toString(), fileSize);
                
                // 7. 暂存待插入的数据库记录（稍后批量插入）
                WorkSubmissionAttachment attachment = new WorkSubmissionAttachment();
                attachment.setSubmissionId(submissionId);
                attachment.setFileName(originalFilename);
                attachment.setFilePath(filePath.toString());
                attachment.setFileSize(fileSize);
                attachment.setFileType(fileType);
                attachment.setUploadTime(LocalDateTime.now());
                pending.add(attachment);

            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to save submission attachment", e);
                throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, 
                        "文件上传失败，请稍后重试", null);
            }
        }

        // 8. 一次批量插入全部附件记录（MP 会把自增主键回填到实体）
        if (!pending.isEmpty()) {
            workSubmissionAttachmentMapper.insert(pending);
            for (WorkSubmissionAttachment attachment : pending) {
                attachmentInfos.add(new WorkSubmissionResponse.AttachmentInfo(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getFilePath(),
                        attachment.getFileSize(),
                        attachment.getFileType(),
                        attachment.getUploadTime()
                ));
            }
        }

        return attachmentInfos;
    }
    

    /**
     * 获取提交附件列表
     */
    private List<WorkSubmissionResponse.AttachmentInfo> getSubmissionAttachments(Integer submissionId) {
        QueryWrapper<WorkSubmissionAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("submission_id", submissionId);
        List<WorkSubmissionAttachment> attachments = workSubmissionAttachmentMapper.selectList(queryWrapper);
        
        return attachments.stream()
                .map(attachment -> new WorkSubmissionResponse.AttachmentInfo(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getFilePath(),
                        attachment.getFileSize(),
                        attachment.getFileType(),
                        attachment.getUploadTime()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取提交附件列表（用于SubmitResponse）
     */
    private List<WorkSubmissionSubmitResponse.AttachmentInfo> getSubmissionAttachmentsForSubmitResponse(Integer submissionId) {
        QueryWrapper<WorkSubmissionAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("submission_id", submissionId);
        List<WorkSubmissionAttachment> attachments = workSubmissionAttachmentMapper.selectList(queryWrapper);
        
        return attachments.stream()
                .map(attachment -> new WorkSubmissionSubmitResponse.AttachmentInfo(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getFilePath(),
                        attachment.getFileSize(),
                        attachment.getFileType(),
                        attachment.getUploadTime()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 检查作业是否已发布（在发布时间内）
     * @param workInfo 作业信息
     * @return true-已发布，false-未发布或已结束
     */
    private boolean isWorkPublished(WorkInfo workInfo) {
        LocalDateTime now = LocalDateTime.now();
        
        // 如果当前时间在发布时间之前，未发布
        return workInfo.getPublishTime() == null || !now.isBefore(workInfo.getPublishTime());
    }

    @Override
    public void batchDownloadAttachments(BatchDownloadAttachmentsRequest request, HttpServletResponse response) {
        // 1. 获取当前用户并验证权限
        User currentUser = userLookup.requireCurrentUser();

        // 2. 查询作业信息
        WorkInfo workInfo = workMapper.selectById(request.getWorkId());
        if (workInfo == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 3. 验证权限（只有班级老师可以批量下载）
        if (!classService.isTeacher(workInfo.getClassId(), currentUser.getId())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有班级老师可以批量下载作业附件", null);
        }

        // 4. 构建查询条件
        QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.eq("work_id", request.getWorkId())
                      .eq("is_deleted", false);

        // 根据筛选条件过滤
        if (request.getGradedOnly() != null) {
            if (request.getGradedOnly()) {
                submissionQuery.eq("status", 2); // 已批改
            } else {
                submissionQuery.ne("status", 2); // 未批改
            }
        }

        if (request.getLateOnly() != null) {
            submissionQuery.eq("is_late", request.getLateOnly());
        }

        submissionQuery.orderByAsc("submitter_id");

        // 5. 查询所有提交记录
        List<WorkSubmission> submissions = workSubmissionMapper.selectList(submissionQuery);
        if (submissions.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.SUBMISSION_NOT_FOUND, "没有找到符合条件的作业提交", null);
        }

        // 6. 批量查询学生信息
        Set<Integer> submitterIds = submissions.stream()
                .map(WorkSubmission::getSubmitterId)
                .collect(Collectors.toSet());
        
        QueryWrapper<User> userQuery = new QueryWrapper<>();
        userQuery.in("id", submitterIds);
        List<User> users = userMapper.selectList(userQuery);
        Map<Integer, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 班级内身份（姓名/学号）随成员记录存储
        Map<Integer, SchoolMember> memberMap = loadClassMembers(workInfo.getClassId(), submitterIds);

        // 7. 批量查询附件
        List<Integer> submissionIds = submissions.stream()
                .map(WorkSubmission::getId)
                .collect(Collectors.toList());
        
        QueryWrapper<WorkSubmissionAttachment> attachmentQuery = new QueryWrapper<>();
        attachmentQuery.in("submission_id", submissionIds)
                      .eq("is_deleted", false);
        List<WorkSubmissionAttachment> allAttachments = workSubmissionAttachmentMapper.selectList(attachmentQuery);

        if (allAttachments.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, "该作业没有附件", null);
        }

        // 8. 按提交ID分组附件
        Map<Integer, List<WorkSubmissionAttachment>> attachmentsBySubmission = allAttachments.stream()
                .collect(Collectors.groupingBy(WorkSubmissionAttachment::getSubmissionId));

        // 9. 设置响应头
        String zipFileName = workInfo.getTitle() + "_作业附件.zip";
        try {
            String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        } catch (Exception e) {
            log.error("Failed to encode zip file name", e);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
        }

        // 10. 创建ZIP文件并写入
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            int fileCount = 0;

            for (WorkSubmission submission : submissions) {
                List<WorkSubmissionAttachment> attachments = attachmentsBySubmission.get(submission.getId());
                if (attachments == null || attachments.isEmpty()) {
                    continue;
                }

                User student = userMap.get(submission.getSubmitterId());
                if (student == null) {
                    log.warn("Student not found for submission: {}", submission.getId());
                    continue;
                }

                // 为每个学生的附件创建子目录，学工号取自学校成员身份
                SchoolMember studentMember = memberMap.get(student.getId());
                String studentNoPart = studentMember != null && studentMember.getStaffNo() != null
                        ? studentMember.getStaffNo() : "";
                String studentDir = sanitizeFileName(student.getUsername() + "-" + studentNoPart) + "/";

                for (WorkSubmissionAttachment attachment : attachments) {
                    try {
                        // 生成自定义文件名
                        String customFileName = generateCustomFileName(
                                request.getFileNameFormat(),
                                student,
                                studentMember,
                                workInfo,
                                submission,
                                attachment
                        );

                        String zipEntryName = studentDir + customFileName;

                        // 确保文件名不重复（如果同一学生有多个同名文件）
                        zipEntryName = ensureUniqueEntryName(zipOut, zipEntryName);

                        // 添加ZIP条目
                        ZipEntry zipEntry = new ZipEntry(zipEntryName);
                        zipOut.putNextEntry(zipEntry);

                        // 读取文件内容并写入ZIP
                        Path filePath = Paths.get(attachment.getFilePath());
                        if (Files.exists(filePath)) {
                            try (InputStream inputStream = Files.newInputStream(filePath)) {
                                byte[] buffer = new byte[8192];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    zipOut.write(buffer, 0, bytesRead);
                                }
                            }
                            fileCount++;
                        } else {
                            log.warn("File not found: {}", attachment.getFilePath());
                        }

                        zipOut.closeEntry();
                    } catch (IOException e) {
                        log.error("Failed to add file to zip: {}", attachment.getFileName(), e);
                    }
                }
            }

            zipOut.finish();

        } catch (IOException e) {
            log.error("Failed to create zip file", e);
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, "打包文件失败", null);
        }
    }

    /**
     * 生成自定义文件名
     */
    private String generateCustomFileName(String format, User student, SchoolMember member, WorkInfo workInfo,
                                          WorkSubmission submission, WorkSubmissionAttachment attachment) {
        String fileName = format;

        // 替换变量；{userNo} / {idName} 取该成员在学校内的学工号与姓名
        String memberNo = member != null && member.getStaffNo() != null ? member.getStaffNo() : "";
        String memberName = member != null && member.getRealName() != null ? member.getRealName() : "";
        fileName = fileName.replace("{username}", sanitizeFileName(student.getUsername()));
        fileName = fileName.replace("{userNo}", sanitizeFileName(memberNo));
        fileName = fileName.replace("{idName}", sanitizeFileName(memberName));
        fileName = fileName.replace("{workTitle}", sanitizeFileName(workInfo.getTitle()));
        fileName = fileName.replace("{submissionId}", String.valueOf(submission.getId()));
        fileName = fileName.replace("{originalFileName}", sanitizeFileName(attachment.getFileName()));

        // 保留原始扩展名
        String originalExtension = getFileExtension(attachment.getFileName());
        if (!fileName.toLowerCase().endsWith(originalExtension.toLowerCase())) {
            fileName += originalExtension;
        }

        return fileName;
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        // 替换Windows和Linux文件系统不允许的字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 确保ZIP条目名称唯一
     */
    private String ensureUniqueEntryName(ZipOutputStream zipOut, String entryName) {
        // 简单策略：如果名称已存在，添加数字后缀
        String extension = getFileExtension(entryName);
        String nameWithoutExt = entryName.substring(0, entryName.length() - extension.length());
        
        int counter = 1;
        while (true) {
            try {
                // 尝试创建条目，如果成功则返回
                zipOut.putNextEntry(new ZipEntry(entryName));
                zipOut.closeEntry();
                return entryName;
            } catch (IllegalArgumentException e) {
                // 条目已存在，生成新名称
                entryName = nameWithoutExt + "_" + counter + extension;
                counter++;
                if (counter > 100) {
                    // 防止无限循环
                    return entryName;
                }
            } catch (IOException e) {
                // IO异常，生成新名称
                entryName = nameWithoutExt + "_" + counter + extension;
                counter++;
                if (counter > 100) {
                    return entryName;
                }
            }
        }
    }
    
}