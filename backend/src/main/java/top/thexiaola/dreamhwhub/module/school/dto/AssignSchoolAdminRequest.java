package top.thexiaola.dreamhwhub.module.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 指派或取消学校管理员请求（平台管理员操作）
 * 目标用户尚未加入学校时，需同时提供其学工号与姓名以建立学校成员关系
 */
@Data
public class AssignSchoolAdminRequest {

    /**
     * 目标用户的用户名或邮箱
     */
    @NotBlank(message = "用户账号不能为空")
    private String userAccount;

    /**
     * true-指派为学校管理员，false-取消学校管理员身份（降为学生）
     */
    @NotNull(message = "请选择指派或取消")
    private Boolean assigned;

    /**
     * 学工号：目标用户尚未加入学校时必填
     */
    @Size(max = 24, message = "学工号长度不能超过 24 位")
    @Pattern(regexp = "^$|^[0-9A-Za-z_-]+$", message = "学工号只能包含字母、数字、下划线和连字符")
    private String staffNo;

    /**
     * 姓名：目标用户尚未加入学校时必填
     */
    @Size(max = 32, message = "姓名长度不能超过 32 位")
    @Pattern(regexp = "^$|^[\\p{L}\\s·-]+$", message = "姓名只能包含字母、汉字及允许的符号（空格、中点、连字符）")
    private String realName;
}
