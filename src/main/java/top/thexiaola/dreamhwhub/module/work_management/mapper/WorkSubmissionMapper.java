package top.thexiaola.dreamhwhub.module.work_management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.work_management.domain.WorkSubmission;

import java.util.List;

/**
 * 作业提交 Mapper 接口
 */
@Mapper
public interface WorkSubmissionMapper extends BaseMapper<WorkSubmission> {
    
    /**
     * 查询某次作业的未交学生列表（数据库层面LEFT JOIN）
     * @param workId 作业ID
     * @return 未交学生列表
     */
    @Select("SELECT u.* FROM user u " +
            "INNER JOIN class_member cm ON u.id = cm.user_id " +
            "LEFT JOIN work_submission ws ON u.id = ws.submitter_id AND ws.work_id = #{workId} " +
            "WHERE cm.class_id = (SELECT class_id FROM work_info WHERE id = #{workId}) " +
            "AND cm.is_teacher = 0 " +
            "AND ws.id IS NULL")
    List<User> selectUnsubmittedStudents(@Param("workId") Integer workId);
}
