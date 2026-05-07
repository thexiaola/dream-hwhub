package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 置顶作业请求
 */
@Data
public class PinWorkRequest {

    /**
     * 作业 ID
     */
    @NotNull(message = "作业 ID 不能为空")
    private Integer workId;

    /**
     * 是否置顶：true-置顶，false-取消置顶
     */
    @NotNull(message = "置顶状态不能为空")
    private Boolean isPinned;
}
