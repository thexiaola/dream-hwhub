package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量设置班级助理请求
 */
@Data
public class BatchSetAssistantsRequest {

    @NotEmpty(message = "用户ID列表不能为空")
    @Size(min = 1, message = "至少需要选择一个用户")
    private List<Integer> studentUserIds;
}
