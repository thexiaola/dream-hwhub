package top.thexiaola.dreamhwhub.module.work_management.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 提交作业请求
 */
@Data
public class SubmitWorkRequest {

    /**
     * 作业 ID（数字格式）
     */
    private String workId;

    /**
     * 提交内容/文本描述
     */
    private String submissionContent;

    /**
     * 附件文件列表（直接上传的文件）
     */
    private List<MultipartFile> attachments;
}
