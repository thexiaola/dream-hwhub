package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置学校成员角色请求（学校管理员操作）
 * 仅可在「老师」与「学生」之间调整，学校管理员由平台管理员指派
 */
@Data
public class UpdateSchoolMemberRoleRequest {

    /**
     * 目标成员的用户 ID
     */
    @NotNull(message = "成员用户 ID 不能为空")
    private Integer userId;

    /**
     * 目标角色：1-老师（可创建班级），0-学生
     */
    @NotNull(message = "角色不能为空")
    @Min(value = 0, message = "角色取值无效")
    @Max(value = 1, message = "学校管理员由平台管理员指派")
    private Integer role;
}
