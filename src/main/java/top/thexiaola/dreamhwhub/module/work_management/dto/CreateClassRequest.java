package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建班级请求
 */
@Data
public class CreateClassRequest {

    /**
     * 班级名称
     */
    @NotBlank(message = "班级名称不能为空")
    private String className;

    /**
     * 班级描述
     */
    private String description;
}
