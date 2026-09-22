package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改学校成员姓名与学工号请求（学校管理员操作）
 */
@Data
public class UpdateSchoolMemberIdentityRequest {

    /**
     * 目标成员的用户 ID
     */
    @NotNull(message = "成员用户 ID 不能为空")
    private Integer userId;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名长度不能超过 32 位")
    @Pattern(regexp = "^[\\p{L}\\s·-]+$", message = "姓名只能包含字母、汉字及允许的符号（空格、中点、连字符）")
    private String realName;

    /**
     * 学工号（同一学校内唯一）
     */
    @NotBlank(message = "学工号不能为空")
    @Size(max = 24, message = "学工号长度不能超过 24 位")
    @Pattern(regexp = "^[0-9A-Za-z_-]+$", message = "学工号只能包含字母、数字、下划线和连字符")
    private String staffNo;
}
