package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.work_management.constant.QuestionType;
import top.thexiaola.dreamhwhub.module.work_management.dto.QuestionItem;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkQuestion;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkQuestionMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkQuestionService;
import top.thexiaola.dreamhwhub.module.work_management.service.support.AnswerGrader;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkQuestionStudentVO;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkQuestionVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作业题目服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkQuestionServiceImpl implements WorkQuestionService {

    private final WorkQuestionMapper workQuestionMapper;
    private final AnswerGrader answerGrader;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceQuestions(Integer workId, List<QuestionItem> questions) {
        if (workId == null) {
            return;
        }
        // 整体替换：先删旧题，再按顺序写入新题
        QueryWrapper<WorkQuestion> deleteQuery = new QueryWrapper<>();
        deleteQuery.eq("work_id", workId);
        workQuestionMapper.delete(deleteQuery);

        if (questions == null || questions.isEmpty()) {
            return;
        }
        validateQuestions(questions);

        List<WorkQuestion> entities = new ArrayList<>(questions.size());
        int order = 1;
        for (QuestionItem item : questions) {
            WorkQuestion entity = new WorkQuestion();
            entity.setWorkId(workId);
            entity.setOrderNo(order++);
            entity.setQuestionType(item.getQuestionType());
            entity.setContent(StrUtil.trim(item.getContent()));
            entity.setOptions(item.getOptions() == null ? null : JSONUtil.toJsonStr(item.getOptions()));
            entity.setCorrectAnswer(answerGrader.toJson(item.getCorrectAnswer()));
            entity.setScore(item.getScore() == null ? BigDecimal.ZERO : item.getScore());
            entity.setAnalysis(StrUtil.trim(item.getAnalysis()));
            entity.setAutoGradable(QuestionType.isAutoGradable(item.getQuestionType()));
            entities.add(entity);
        }
        // 批量插入，避免逐条往返
        workQuestionMapper.insert(entities);
        log.info("Replaced {} questions for work {}", entities.size(), workId);
    }

    @Override
    public List<WorkQuestionVO> listForTeacher(Integer workId) {
        List<WorkQuestion> questions = queryOrdered(workId);
        List<WorkQuestionVO> result = new ArrayList<>(questions.size());
        for (WorkQuestion q : questions) {
            WorkQuestionVO vo = new WorkQuestionVO();
            vo.setId(q.getId());
            vo.setWorkId(q.getWorkId());
            vo.setOrderNo(q.getOrderNo());
            vo.setQuestionType(q.getQuestionType());
            vo.setQuestionTypeName(QuestionType.nameOf(q.getQuestionType()));
            vo.setContent(q.getContent());
            vo.setOptions(parseOptions(q.getOptions()));
            vo.setCorrectAnswer(answerGrader.parseJson(q.getCorrectAnswer()));
            vo.setScore(q.getScore());
            vo.setAnalysis(q.getAnalysis());
            vo.setAutoGradable(q.getAutoGradable());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<WorkQuestionStudentVO> listForStudent(Integer workId) {
        List<WorkQuestion> questions = queryOrdered(workId);
        List<WorkQuestionStudentVO> result = new ArrayList<>(questions.size());
        for (WorkQuestion q : questions) {
            // 学生侧严格不下发参考答案与解析
            WorkQuestionStudentVO vo = new WorkQuestionStudentVO();
            vo.setId(q.getId());
            vo.setOrderNo(q.getOrderNo());
            vo.setQuestionType(q.getQuestionType());
            vo.setQuestionTypeName(QuestionType.nameOf(q.getQuestionType()));
            vo.setContent(q.getContent());
            vo.setOptions(parseStudentOptions(q.getOptions()));
            vo.setScore(q.getScore());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<WorkQuestion> listEntities(Integer workId) {
        return queryOrdered(workId);
    }

    @Override
    public boolean hasQuestions(Integer workId) {
        if (workId == null) {
            return false;
        }
        QueryWrapper<WorkQuestion> query = new QueryWrapper<>();
        query.eq("work_id", workId);
        return workQuestionMapper.selectCount(query) > 0;
    }

    @Override
    public void validateQuestions(List<QuestionItem> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (QuestionItem item : questions) {
            String type = item.getQuestionType();
            if (!QuestionType.isValid(type)) {
                throw new BusinessException(BusinessErrorCode.QUESTION_TYPE_INVALID,
                        "题型不合法：" + type, null);
            }
            if (StrUtil.isBlank(item.getContent())) {
                throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "题干不能为空", null);
            }
            if (item.getScore() == null || item.getScore().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR, "题目分值不能为负", null);
            }
            total = total.add(item.getScore());

            // 选择题：选项与答案必填
            if (QuestionType.SINGLE.equals(type) || QuestionType.MULTIPLE.equals(type)) {
                if (item.getOptions() == null || item.getOptions().size() < 2) {
                    throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                            "选择题至少需要 2 个选项", null);
                }
                if (item.getCorrectAnswer() == null) {
                    throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                            "选择题必须设置参考答案", null);
                }
            }
            // 判断题：答案必填
            if (QuestionType.JUDGE.equals(type) && item.getCorrectAnswer() == null) {
                throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                        "判断题必须设置参考答案", null);
            }
            // 填空题：答案必填
            if (QuestionType.FILL.equals(type) && item.getCorrectAnswer() == null) {
                throw new BusinessException(BusinessErrorCode.PARAMETER_ERROR,
                        "填空题必须设置参考答案", null);
            }
            // 主观题/附加题：无需参考答案（仅老师手动评分）
        }
    }

    // ===== 私有辅助 =====

    private List<WorkQuestion> queryOrdered(Integer workId) {
        if (workId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<WorkQuestion> query = new QueryWrapper<>();
        query.eq("work_id", workId).orderByAsc("order_no").orderByAsc("id");
        return workQuestionMapper.selectList(query);
    }

    private List<WorkQuestionVO.OptionVO> parseOptions(String options) {
        if (StrUtil.isBlank(options)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(JSONUtil.parseArray(options), null).stream()
                    .map(o -> {
                        cn.hutool.json.JSONObject obj = (cn.hutool.json.JSONObject) o;
                        return new WorkQuestionVO.OptionVO(
                                obj.getStr("key"), obj.getStr("text"));
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse question options: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<WorkQuestionStudentVO.OptionVO> parseStudentOptions(String options) {
        if (StrUtil.isBlank(options)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.parseArray(options).stream()
                    .map(o -> {
                        cn.hutool.json.JSONObject obj = (cn.hutool.json.JSONObject) o;
                        return new WorkQuestionStudentVO.OptionVO(
                                obj.getStr("key"), obj.getStr("text"));
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse question options: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
