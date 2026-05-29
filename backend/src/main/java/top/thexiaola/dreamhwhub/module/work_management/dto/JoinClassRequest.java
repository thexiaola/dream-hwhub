package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 加入班级请求
 */
@Data
public class JoinClassRequest {

    /**
     * 班级 ID
     */
    @NotNull(message = "班级 ID 不能为空")
    private Integer classId;
}
