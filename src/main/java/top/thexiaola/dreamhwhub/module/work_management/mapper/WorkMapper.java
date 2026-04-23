package top.thexiaola.dreamhwhub.module.work_management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.thexiaola.dreamhwhub.module.work_management.entity.WorkInfo;

/**
 * 作业 Mapper 接口
 */
@Mapper
public interface WorkMapper extends BaseMapper<WorkInfo> {
}
