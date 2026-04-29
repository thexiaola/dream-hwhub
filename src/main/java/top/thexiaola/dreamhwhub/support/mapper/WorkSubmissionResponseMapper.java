package top.thexiaola.dreamhwhub.support.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkInfo;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkSubmission;
import top.thexiaola.dreamhwhub.module.work_management.vo.WorkSubmissionResponse;

/**
 * 作业提交响应对象映射器
 */
@Mapper(componentModel = "spring")
public interface WorkSubmissionResponseMapper {
    
    /**
     * WorkSubmission 实体转换为 WorkSubmissionResponse VO
     * 需要额外传入 WorkInfo 以获取作业标题
     * 
     * @param submission 提交实体
     * @param workInfo 作业信息（用于获取标题）
     * @return 提交响应 VO
     */
    @Mapping(target = "workTitle", source = "workInfo.title")
    @Mapping(target = "id", source = "submission.id")
    @Mapping(target = "createTime", source = "submission.createTime")
    @Mapping(target = "updateTime", source = "submission.updateTime")
    @Mapping(target = "attachments", ignore = true)  // 附件需要单独加载
    WorkSubmissionResponse toResponse(WorkSubmission submission, WorkInfo workInfo);
}