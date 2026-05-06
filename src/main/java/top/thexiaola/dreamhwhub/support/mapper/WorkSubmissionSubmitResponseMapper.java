package top.thexiaola.dreamhwhub.support.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionSubmitResponse;

/**
 * 作业提交响应对象映射器（用于提交接口，不包含批改信息）
 */
@Mapper(componentModel = "spring")
public interface WorkSubmissionSubmitResponseMapper {
    
    /**
     * WorkSubmission 实体转换为 WorkSubmissionSubmitResponse VO
     * 不包含批改相关字段（score, comment, gradeTime, graderId）
     * 
     * @param submission 提交实体
     * @return 提交响应 VO
     */
    @Mapping(target = "id", source = "submission.id")
    @Mapping(target = "createTime", source = "submission.createTime")
    @Mapping(target = "updateTime", source = "submission.updateTime")
    @Mapping(target = "attachments", ignore = true)  // 附件需要单独加载
    WorkSubmissionSubmitResponse toSubmitResponse(WorkSubmission submission);
}
