package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.support.validation.XssValidator;

import java.util.List;

/**
 * 提交作业请求
 */
@Data
public class SubmitWorkRequest {

    /**
     * 作业 ID
     */
    private Integer workId;

    /**
     * 提交内容/文本描述
     */
    @Size(max = 2048, message = "提交内容长度不能超过 2048 位")
    @Pattern(regexp = "^[^\\t\\f\\v]*$", message = "提交内容不能包含特殊字符（制表符等）")
    private String submissionContent;

    /**
     * 附件文件列表（直接上传的文件）
     */
    private List<MultipartFile> attachments;

    /**
     * 自定义校验：XSS防护
     */
    public void validate() {
        if (submissionContent != null) {
            XssValidator.validateNoXss(submissionContent, "提交内容");
        }
    }
}
