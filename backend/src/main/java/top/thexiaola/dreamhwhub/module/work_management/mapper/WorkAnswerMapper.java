package top.thexiaola.dreamhwhub.module.work_management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkAnswer;

import java.math.BigDecimal;

/**
 * 作业作答 Mapper 接口
 */
@Mapper
public interface WorkAnswerMapper extends BaseMapper<WorkAnswer> {

    /**
     * 在数据库侧汇总某次提交的作答得分（NULL 视为 0）
     *
     * @param submissionId 提交 ID
     * @return 得分合计
     */
    @Select("SELECT COALESCE(SUM(score), 0) FROM work_answer WHERE submission_id = #{submissionId}")
    BigDecimal sumScoreBySubmission(@Param("submissionId") Integer submissionId);
}
