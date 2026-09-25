package top.thexiaola.dreamhwhub.module.message.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加好友请求（按学校，目标为同校用户）
 */
@Data
public class AddFriendRequest {

    /**
     * 目标学校 ID（好友按学校隔离）
     */
    @NotNull(message = "请选择学校")
    private Integer schoolId;

    /**
     * 目标用户 ID
     */
    @NotNull(message = "请选择要添加的用户")
    private Integer targetUserId;
}
