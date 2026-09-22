package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改学校信息请求（平台管理员或学校管理员操作）
 */
@Data
public class UpdateSchoolRequest {

    /**
     * 学校名称
     */
    @NotBlank(message = "学校名称不能为空")
    @Size(max = 100, message = "学校名称长度不能超过 100 位")
    private String schoolName;

    /**
     * 学校描述
     */
    @Size(max = 500, message = "学校描述长度不能超过 500 位")
    private String description;
}
