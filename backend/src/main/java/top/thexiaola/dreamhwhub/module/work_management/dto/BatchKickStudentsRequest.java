package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchKickStudentsRequest {

    @NotEmpty(message = "用户ID列表不能为空")
    @Size(min = 1, message = "至少需要踢出一个用户")
    private List<Integer> studentUserIds;
}
