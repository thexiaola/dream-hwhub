package top.thexiaola.dreamhwhub.module.work_management.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.module.work_management.domain.TempFileUpload;
import top.thexiaola.dreamhwhub.module.work_management.dto.FileUploadResponse;
import top.thexiaola.dreamhwhub.module.work_management.mapper.TempFileUploadMapper;
import top.thexiaola.dreamhwhub.module.work_management.service.FileUploadService;
import top.thexiaola.dreamhwhub.support.session.UserUtils;
import top.thexiaola.dreamhwhub.support.validation.FileUploadValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件上传服务实现
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {
    
    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);
    
    private final TempFileUploadMapper tempFileUploadMapper;
    
    // 文件存储根目录
    private static final String UPLOAD_DIR = "uploads/temp/";
    
    public FileUploadServiceImpl(TempFileUploadMapper tempFileUploadMapper) {
        this.tempFileUploadMapper = tempFileUploadMapper;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadResponse uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, "文件不能为空", null);
        }
        
        // 获取当前用户
        Integer userId = UserUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(BusinessErrorCode.USER_NOT_LOGGED_IN, "用户未登录", null);
        }
        
        try {
            // 1. 生成安全的文件名（防止路径遍历攻击）
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.contains("..")) {
                throw new BusinessException(BusinessErrorCode.INVALID_FILE_PATH, "非法的文件名", null);
            }
            
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String safeFileName = UUID.randomUUID() + extension;
            
            // 2. 确保上传目录存在
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            
            // 3. 保存文件
            Path filePath = uploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath);
            
            // 4. 获取文件信息
            long fileSize = Files.size(filePath);
            String fileType = FileUploadValidator.detectFileType(filePath.toString());
            
            // 5. 执行安全检查
            FileUploadValidator.performFullSecurityCheck(filePath.toString(), fileSize);
            
            // 6. 保存到临时文件表
            TempFileUpload tempFile = new TempFileUpload();
            tempFile.setUploaderId(userId);
            tempFile.setFileName(originalFilename);
            tempFile.setFilePath(filePath.toString());
            tempFile.setFileSize(fileSize);
            tempFile.setFileType(fileType);
            tempFile.setUploadTime(LocalDateTime.now());
            tempFile.setExpireTime(LocalDateTime.now().plusHours(24)); // 24小时后过期
            tempFile.setIsUsed(false);
            
            tempFileUploadMapper.insert(tempFile);
            
            log.info("User {} uploaded file: id={}, name={}", userId, tempFile.getId(), originalFilename);
            
            // 7. 返回响应
            FileUploadResponse response = new FileUploadResponse();
            response.setFileId(tempFile.getId());
            response.setFileName(originalFilename);
            response.setFileSize(fileSize);
            response.setFileType(fileType);
            response.setUploadTime(tempFile.getUploadTime());
            
            return response;
            
        } catch (IOException e) {
            log.error("File upload failed for user {}", userId, e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, "文件上传失败: " + e.getMessage(), null);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void validateAndMarkAsUsed(Integer fileId, Integer userId) {
        if (fileId == null) {
            return; // 允许没有附件
        }
        
        // 查询临时文件
        TempFileUpload tempFile = tempFileUploadMapper.selectById(fileId);
        if (tempFile == null) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "文件不存在或已被使用", null);
        }
        
        // 验证文件归属权
        if (!tempFile.getUploaderId().equals(userId)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "无权使用该文件", null);
        }
        
        // 检查是否已过期
        if (LocalDateTime.now().isAfter(tempFile.getExpireTime())) {
            throw new BusinessException(BusinessErrorCode.WORK_STATUS_ERROR, "文件已过期，请重新上传", null);
        }
        
        // 检查是否已被使用
        if (Boolean.TRUE.equals(tempFile.getIsUsed())) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, "文件已被使用", null);
        }
        
        // 标记为已使用
        tempFile.setIsUsed(true);
        tempFileUploadMapper.updateById(tempFile);
        
        log.info("User {} validated and marked file {} as used", userId, fileId);
    }
}
