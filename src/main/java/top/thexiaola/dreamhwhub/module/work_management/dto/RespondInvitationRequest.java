package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 响应邀请请求
 */
@Data
public class RespondInvitationRequest {

    /**
     * 邀请 ID（数字格式）
     */
    @NotNull(message = "邀请 ID 不能为空")
    @Pattern(regexp = "^[0-9]+$", message = "邀请 ID 必须是数字")
    private String invitationId;

    /**
     * 是否同意（true-同意，false-拒绝）
     */
    @NotNull(message = "响应结果不能为空")
    private Boolean accepted;

    /**
     * 回复说明（可选）
     */
    @Size(max = 500, message = "回复说明长度不能超过 500 位")
    @Pattern(regexp = "^[^\\t\\f\\v]*$", message = "回复说明不能包含特殊字符（制表符等）")
    private String comment;
}
