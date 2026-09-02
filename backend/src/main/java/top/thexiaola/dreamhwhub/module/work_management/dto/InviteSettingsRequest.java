package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 学生邀请设置请求
 */
@Data
public class InviteSettingsRequest {

    /**
     * 是否允许学生邀请同学加入
     */
    @NotNull(message = "邀请设置不能为空")
    private Boolean allowStudentInvite;
}
