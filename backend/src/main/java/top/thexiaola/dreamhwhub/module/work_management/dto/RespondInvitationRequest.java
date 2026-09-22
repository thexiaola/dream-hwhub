package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 响应邀请请求
 * 姓名与学工号取自用户的学校成员身份，不在此处填写
 */
@Data
public class RespondInvitationRequest {

    /**
     * 邀请 ID（响应收到的邀请时需要）
     */
    private Integer invitationId;

    /**
     * 是否同意：true=同意，false=拒绝
     */
    @NotNull(message = "响应结果不能为空")
    private Boolean accepted;
}
