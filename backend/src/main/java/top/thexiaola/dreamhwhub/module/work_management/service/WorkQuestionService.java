package top.thexiaola.dreamhwhub.module.work_management.service;

import top.thexiaola.dreamhwhub.module.work_management.dto.QuestionItem;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkQuestion;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkQuestionStudentVO;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkQuestionVO;

import java.util.List;

/**
 * 作业题目服务接口
 * <p>
 * 题目属作业的一部分：老师保存作业时整体替换题目；学生取题时不下发参考答案。
 */
public interface WorkQuestionService {

    /**
     * 整体替换某作业的题目（传空列表表示清除题目、退回纯文本作业）
     *
     * @param workId    作业 ID
     * @param questions 题目列表
     */
    void replaceQuestions(Integer workId, List<QuestionItem> questions);

    /**
     * 查询某作业的题目（教师侧，含参考答案与解析）
     *
     * @param workId 作业 ID
     * @return 题目列表
     */
    List<WorkQuestionVO> listForTeacher(Integer workId);

    /**
     * 查询某作业的题目（学生侧，不含参考答案与解析）
     *
     * @param workId 作业 ID
     * @return 题目列表
     */
    List<WorkQuestionStudentVO> listForStudent(Integer workId);

    /**
     * 查询题目实体（供评分逻辑读取参考答案）
     *
     * @param workId 作业 ID
     * @return 题目实体列表
     */
    List<WorkQuestion> listEntities(Integer workId);

    /**
     * 作业是否含题目
     *
     * @param workId 作业 ID
     * @return true-含题目
     */
    boolean hasQuestions(Integer workId);

    /**
     * 校验题目列表的合法性（题型、选项、答案、分值）
     *
     * @param questions 题目列表
     */
    void validateQuestions(List<QuestionItem> questions);
}
