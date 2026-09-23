package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核班级接管申请请求（学校管理员）
 * <p>
 * 申请 ID 走路径参数，请求体只携带审核结果与意见。
 */
@Data
public class ReviewClassTakeoverRequest {

    /**
     * 审核结果：true-同意接管，false-拒绝
     */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    /**
     * 审核意见
     */
    @Size(max = 256, message = "审核意见长度不能超过 256 位")
    @Pattern(regexp = "^[^\\t\\f\\v]*$", message = "审核意见不能包含特殊字符（制表符等）")
    private String comment;
}
