package top.thexiaola.dreamhwhub.module.work_management.service;

import top.thexiaola.dreamhwhub.module.work_management.dto.ReportViolationRequest;
import top.thexiaola.dreamhwhub.module.work_management.vo.ExamEnterResponse;
import top.thexiaola.dreamhwhub.module.work_management.vo.ExamViolationVO;

import java.util.List;

/**
 * 考试会话服务接口
 * <p>
 * 负责学生进入考试（创建/恢复会话）、限时与反作弊配置下发、违规上报，
 * 以及教师查看考试违规明细。
 */
public interface ExamSessionService {

    /**
     * 进入考试：创建或恢复考试会话，返回题目（按需乱序/字体映射打乱）与剩余时间。
     *
     * @param workId 考试 ID
     * @return 进入考试所需数据
     */
    ExamEnterResponse enterExam(Integer workId);

    /**
     * 上报一次违规（离开全屏、切屏、复制粘贴等）。
     *
     * @param workId    考试 ID
     * @param request   违规类型与说明
     * @return 是否已达到违规上限（达到即应自动交卷）
     */
    boolean reportViolation(Integer workId, ReportViolationRequest request);

    /**
     * 保存答题草稿（自动保存，刷新/断线后可恢复）。
     *
     * @param workId 考试 ID
     * @param draft  草稿 JSON 字符串
     */
    void saveDraft(Integer workId, String draft);

    /**
     * 查询某场考试的违规记录（教师侧，按学生分组便于审计）。
     *
     * @param workId 考试 ID
     * @return 违规记录列表
     */
    List<ExamViolationVO> listViolations(Integer workId);
}
