package top.thexiaola.dreamhwhub.module.work_management.service;

import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.module.work_management.dto.FileUploadResponse;

/**
 * 文件上传服务接口
 */
public interface FileUploadService {
    
    /**
     * 上传文件（返回文件ID）
     * @param file 上传的文件
     * @return 文件上传响应
     */
    FileUploadResponse uploadFile(MultipartFile file);
    
    /**
     * 验证文件归属权并标记为已使用
     * @param fileId 文件ID
     * @param userId 当前用户ID
     */
    void validateAndMarkAsUsed(Integer fileId, Integer userId);
}
