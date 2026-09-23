package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通过邀请码加入班级请求
 */
@Data
public class JoinByInviteCodeRequest {

    /**
     * 班级邀请码（25 位随机码）
     */
    @NotBlank(message = "邀请码不能为空")
    @Size(max = 64, message = "邀请码长度不能超过 64 位")
    @Pattern(regexp = "^[^\\r\\n\\t\\f\\v]+$", message = "邀请码不能包含特殊字符（换行符、制表符等）")
    private String inviteCode;

    /**
     * 当前所在学校 ID：只能加入该学校下的班级，跨校加入会被拒绝
     */
    @NotNull(message = "请先选择学校")
    private Integer schoolId;
}
