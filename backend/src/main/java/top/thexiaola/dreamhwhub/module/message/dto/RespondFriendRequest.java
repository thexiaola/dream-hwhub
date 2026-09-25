package top.thexiaola.dreamhwhub.module.message.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 响应好友申请请求（同意 / 拒绝）
 */
@Data
public class RespondFriendRequest {

    /**
     * 好友关系 ID
     */
    @NotNull(message = "请指定好友申请")
    private Integer relationId;

    /**
     * true-同意，false-拒绝
     */
    @NotNull(message = "请选择处理方式")
    private Boolean accepted;
}
