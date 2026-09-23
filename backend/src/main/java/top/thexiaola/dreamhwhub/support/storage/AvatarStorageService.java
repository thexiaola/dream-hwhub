package top.thexiaola.dreamhwhub.support.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;
import top.thexiaola.dreamhwhub.support.validation.FileUploadValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 用户头像文件存储：保存到 attachments/avatar 下并返回相对路径。
 *
 * <p>头像仅允许图片类型并单独限制大小；入库的路径为相对路径，可交给
 * FileController 的下载接口内联预览。替换头像时旧文件的删除由调用方在
 * 数据库更新成功后执行，避免更新失败导致头像丢失。</p>
 */
@Slf4j
@Service
public class AvatarStorageService {

    /** 头像存储目录（相对运行目录） */
    private static final String AVATAR_DIR = "attachments/avatar/";

    /** 头像最大大小：5MB */
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    /** 允许的头像扩展名 */
    private static final Set<String> AVATAR_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp");

    private static final DateTimeFormatter NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 保存头像文件
     *
     * @param file   上传的头像文件
     * @param userId 所属用户 ID，用于生成文件名
     * @return 头像相对路径（形如 attachments/avatar/xxx.png）
     */
    public String save(MultipartFile file, Integer userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.PARAMETER_MISSING, "请选择要上传的头像文件", null);
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new BusinessException(BusinessErrorCode.INVALID_FILE_PATH, "非法的文件名", null);
        }

        String extension = extractExtension(originalFilename);
        if (!AVATAR_EXTENSIONS.contains(extension)) {
            throw new BusinessException(BusinessErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "头像仅支持 jpg / jpeg / png / gif / bmp / webp 格式", null);
        }
        FileUploadValidator.validateFileSize(file.getSize(), MAX_AVATAR_SIZE);

        String safeFileName = String.format("avatar_%s_%s_%s.%s",
                userId,
                LocalDateTime.now().format(NAME_FORMAT),
                UUID.randomUUID().toString().substring(0, 8),
                extension);

        Path uploadDir = Paths.get(AVATAR_DIR).toAbsolutePath().normalize();
        Path filePath = uploadDir.resolve(safeFileName);
        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), filePath);
            // 落盘后做完整安全检查（扩展名 / MIME / 魔数），失败时删除已写入的文件
            FileUploadValidator.performFullSecurityCheck(filePath.toString(), Files.size(filePath), MAX_AVATAR_SIZE);
        } catch (BusinessException e) {
            deleteQuietly(filePath);
            throw e;
        } catch (Exception e) {
            deleteQuietly(filePath);
            log.error("Failed to save avatar for user {}", userId, e);
            throw new BusinessException(BusinessErrorCode.FILE_UPLOAD_FAILED, "头像上传失败，请重试", null);
        }

        // 与作业附件保持一致，入库相对路径，便于迁移且不暴露服务器绝对目录
        String relativePath = AVATAR_DIR + safeFileName;
        log.info("Saved avatar for user {} at {}", userId, relativePath);
        return relativePath;
    }

    /**
     * 删除旧头像文件；仅限头像目录内的路径，避免误删其它文件。
     *
     * @param avatarPath 头像相对路径
     */
    public void delete(String avatarPath) {
        if (avatarPath == null || avatarPath.isBlank()) {
            return;
        }
        Path avatarRoot = Paths.get(AVATAR_DIR).toAbsolutePath().normalize();
        Path target = Paths.get(avatarPath).toAbsolutePath().normalize();
        if (!target.startsWith(avatarRoot)) {
            return;
        }
        deleteQuietly(target);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (Exception e) {
            log.warn("Failed to delete avatar file: {}", path, e);
        }
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BusinessException(BusinessErrorCode.FILE_TYPE_NOT_ALLOWED, "头像文件缺少有效的扩展名", null);
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
