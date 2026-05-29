package top.thexiaola.dreamhwhub.module.work_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量下载作业附件请求
 */
@Data
public class BatchDownloadAttachmentsRequest {

    /**
     * 作业 ID
     */
    @NotNull(message = "作业 ID 不能为空")
    private Integer workId;

    /**
     * 文件名格式模板
     * 支持的变量：
     * - {username}: 用户名
     * - {userNo}: 学号/工号
     * - {idName}: 身份证姓名
     * - {workTitle}: 作业标题
     * - {submissionId}: 提交ID
     * - {originalFileName}: 原始文件名
     * 示例："{username}-{userNo}_{workTitle}"
     * 默认："{username}-{userNo}_{originalFileName}"
     */
    private String fileNameFormat = "{username}-{userNo}_{originalFileName}";

    /**
     * 是否只下载已批改的作业（可选）
     * null-全部，true-已批改，false-未批改
     */
    private Boolean gradedOnly;

    /**
     * 是否只下载逾期提交的作业（可选）
     * null-全部，true-逾期，false-按时
     */
    private Boolean lateOnly;
}
