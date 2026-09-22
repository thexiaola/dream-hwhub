package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建学校请求（平台管理员操作）
 */
@Data
public class CreateSchoolRequest {

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

    /**
     * 加入学校是否免审核：true-填入学工号与姓名后直接加入，false/null-需学校管理员审核
     */
    private Boolean allowJoinWithoutApproval;
}
