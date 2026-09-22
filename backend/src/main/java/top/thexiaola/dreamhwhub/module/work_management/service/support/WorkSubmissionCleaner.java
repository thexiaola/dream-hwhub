package top.thexiaola.dreamhwhub.module.work_management.service.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkSubmissionAttachment;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionAttachmentMapper;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkSubmissionMapper;

import java.util.List;

/**
 * 作业提交清理器
 * <p>
 * 成员被踢出班级，或被移出/退出学校时，软删除其在该班级的作业提交与附件。
 * 独立成组件以便班级模块与学校模块共用同一套清理规则。
 */
@Component
@RequiredArgsConstructor
public class WorkSubmissionCleaner {

    private final WorkMapper workMapper;
    private final WorkSubmissionMapper workSubmissionMapper;
    private final WorkSubmissionAttachmentMapper workSubmissionAttachmentMapper;

    /**
     * 软删除用户在指定班级内的全部作业提交与附件
     *
     * @param classId 班级 ID
     * @param userId  用户 ID
     */
    public void cleanupClassSubmissions(Integer classId, Integer userId) {
        QueryWrapper<WorkInfo> workQuery = new QueryWrapper<>();
        workQuery.eq("class_id", classId).select("id");
        List<Integer> workIds = workMapper.selectList(workQuery).stream()
                .map(WorkInfo::getId)
                .toList();
        if (workIds.isEmpty()) {
            return;
        }

        QueryWrapper<WorkSubmission> submissionQuery = new QueryWrapper<>();
        submissionQuery.eq("submitter_id", userId)
                .eq("is_deleted", false)
                .in("work_id", workIds);
        List<Integer> submissionIds = workSubmissionMapper.selectList(submissionQuery).stream()
                .map(WorkSubmission::getId)
                .toList();
        if (submissionIds.isEmpty()) {
            return;
        }

        // 先批量软删除附件记录，再软删除提交记录
        QueryWrapper<WorkSubmissionAttachment> attachmentQuery = new QueryWrapper<>();
        attachmentQuery.in("submission_id", submissionIds).eq("is_deleted", false);
        WorkSubmissionAttachment attachmentUpdate = new WorkSubmissionAttachment();
        attachmentUpdate.setIsDeleted(true);
        workSubmissionAttachmentMapper.update(attachmentUpdate, attachmentQuery);

        QueryWrapper<WorkSubmission> submissionUpdateQuery = new QueryWrapper<>();
        submissionUpdateQuery.in("id", submissionIds);
        WorkSubmission submissionUpdate = new WorkSubmission();
        submissionUpdate.setIsDeleted(true);
        workSubmissionMapper.update(submissionUpdate, submissionUpdateQuery);
    }
}
