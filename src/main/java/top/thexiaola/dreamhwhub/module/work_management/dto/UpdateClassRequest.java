package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新班级信息请求
 */
@Data
public class UpdateClassRequest {

    /**
     * 班级 ID
     */
    @NotNull(message = "班级 ID 不能为空")
    private Integer classId;

    /**
     * 班级名称
     */
    @NotBlank(message = "班级名称不能为空")
    @Size(max = 100, message = "班级名称长度不能超过 100 位")
    private String className;

    /**
     * 班级描述
     */
    @Size(max = 500, message = "班级描述长度不能超过 500 位")
    private String description;
}
