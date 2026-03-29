package top.thexiaola.dreamhwhub.module.work_management.service;

import top.thexiaola.dreamhwhub.module.work_management.domain.Work;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.WorkResponse;

import java.util.List;

/**
 * 作业服务接口
 */
public interface WorkService {

    /**
     * 创建作业
     *
     * @param request 创建作业请求
     * @return 创建的作业
     */
    Work createWork(CreateWorkRequest request);

    /**
     * 更新作业
     *
     * @param request 更新作业请求
     * @return 更新后的作业
     */
    Work updateWork(UpdateWorkRequest request);

    /**
     * 删除作业
     *
     * @param workId 作业 ID
     */
    void deleteWork(Integer workId);

    /**
     * 根据 ID 查询作业
     *
     * @param workId 作业 ID
     * @return 作业信息
     */
    Work getWorkById(Integer workId);

    /**
     * 查询作业列表
     *
     * @param teacherNo 教师工号（可选）
     * @param status 作业状态（可选）
     * @return 作业列表
     */
    List<WorkResponse> getWorkList(String teacherNo, Integer status);
}
