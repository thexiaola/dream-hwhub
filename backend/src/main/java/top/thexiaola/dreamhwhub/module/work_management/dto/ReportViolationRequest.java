package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 考试违规上报请求（由学生端在检测到违规行为时提交）
 */
@Data
public class ReportViolationRequest {

    /**
     * 违规类型：fullscreen_exit / visibility_hidden / window_blur / copy / cut / paste
     */
    @NotBlank(message = "违规类型不能为空")
    private String type;

    /**
     * 补充说明（可选）
     */
    @Size(max = 255, message = "补充说明长度不能超过 255 位")
    private String detail;
}
