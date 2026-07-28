package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置班级助理请求
 */
@Data
public class SetAssistantRequest {

    /**
     * 学生用户 ID
     */
    @NotNull(message = "学生用户 ID 不能为空")
    private Integer studentUserId;
}
