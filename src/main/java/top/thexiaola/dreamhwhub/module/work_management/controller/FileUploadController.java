package top.thexiaola.dreamhwhub.module.work_management.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.common.api.ApiResponse;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.login.domain.User;
import top.thexiaola.dreamhwhub.module.work_management.dto.FileUploadResponse;
import top.thexiaola.dreamhwhub.module.work_management.service.FileUploadService;
import top.thexiaola.dreamhwhub.support.logging.LogUtil;
import top.thexiaola.dreamhwhub.support.session.UserUtils;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {
    
    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);
    
    private final FileUploadService fileUploadService;
    
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }
    
    /**
     * 上传文件（返回文件ID）
     */
    @PostMapping("/file")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(@RequestParam("file") MultipartFile file) {
        String ip = LogUtil.getCurrentClientIp();
        try {
            User user = UserUtils.getCurrentUser();
            String userInfo = LogUtil.getUserInfoString(ip, user);
            
            FileUploadResponse response = fileUploadService.uploadFile(file);
            log.info("User ({}) uploaded file successfully, fileId: {}", userInfo, response.getFileId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (BusinessException e) {
            log.warn("User upload file failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("User upload file error", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "文件上传失败"));
        }
    }
}
