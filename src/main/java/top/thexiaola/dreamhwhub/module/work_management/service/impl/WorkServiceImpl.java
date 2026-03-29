package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.login.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.module.work_management.domain.Work;
import top.thexiaola.dreamhwhub.module.work_management.dto.CreateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.UpdateWorkRequest;
import top.thexiaola.dreamhwhub.module.work_management.dto.WorkResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.WorkMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.WorkService;
import top.thexiaola.dreamhwhub.util.UserUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业服务实现类
 */
@Service
public class WorkServiceImpl implements WorkService {

    private final WorkMapper workMapper;

    public WorkServiceImpl(WorkMapper workMapper) {
        this.workMapper = workMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Work createWork(CreateWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限（只有教师可以发布作业）
        if (currentUser.getPermission() < 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有教师可以发布作业", null);
        }

        // 创建作业
        Work work = new Work();
        work.setTitle(request.getTitle());
        work.setDescription(request.getDescription());
        work.setTeacherNo(currentUser.getUserNo());
        work.setTeacherName(currentUser.getIdName());
        work.setDeadline(request.getDeadline());
        work.setTotalScore(request.getTotalScore());
        work.setStatus(1); // 已发布
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());

        workMapper.insert(work);
        return work;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Work updateWork(UpdateWorkRequest request) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限
        if (currentUser.getPermission() < 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有教师可以修改作业", null);
        }

        // 查询作业
        Work work = workMapper.selectById(request.getId());
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 只能修改自己发布的作业
        if (!work.getTeacherNo().equals(currentUser.getUserNo())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能修改自己发布的作业", null);
        }

        // 更新作业
        work.setTitle(request.getTitle());
        work.setDescription(request.getDescription());
        work.setDeadline(request.getDeadline());
        work.setTotalScore(request.getTotalScore());
        work.setStatus(request.getStatus());
        work.setUpdateTime(LocalDateTime.now());

        workMapper.updateById(work);
        return work;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWork(Integer workId) {
        // 获取当前用户
        User currentUser = UserUtils.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }

        // 检查权限
        if (currentUser.getPermission() < 2) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只有教师可以删除作业", null);
        }

        // 查询作业
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }

        // 只能删除自己发布的作业
        if (!work.getTeacherNo().equals(currentUser.getUserNo())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "只能删除自己发布的作业", null);
        }

        workMapper.deleteById(workId);
    }

    @Override
    public Work getWorkById(Integer workId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BusinessException(BusinessErrorCode.WORK_NOT_FOUND, "作业不存在", null);
        }
        return work;
    }

    @Override
    public List<WorkResponse> getWorkList(String teacherNo, Integer status) {
        QueryWrapper<Work> queryWrapper = new QueryWrapper<>();
        
        if (teacherNo != null && !teacherNo.isEmpty()) {
            queryWrapper.eq("teacher_no", teacherNo);
        }
        
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("create_time");
        
        List<Work> works = workMapper.selectList(queryWrapper);
        LocalDateTime now = LocalDateTime.now();
        
        return works.stream()
                .map(work -> {
                    WorkResponse response = new WorkResponse();
                    response.setId(work.getId());
                    response.setTitle(work.getTitle());
                    response.setDescription(work.getDescription());
                    response.setTeacherNo(work.getTeacherNo());
                    response.setTeacherName(work.getTeacherName());
                    response.setDeadline(work.getDeadline());
                    response.setTotalScore(work.getTotalScore());
                    response.setStatus(work.getStatus());
                    response.setIsOverdue(work.getDeadline() != null && now.isAfter(work.getDeadline()));
                    response.setCreateTime(work.getCreateTime());
                    response.setUpdateTime(work.getUpdateTime());
                    return response;
                })
                .collect(Collectors.toList());
    }
}
