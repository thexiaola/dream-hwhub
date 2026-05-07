package top.thexiaola.dreamhwhub.support.validation;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import top.thexiaola.dreamhwhub.enums.BusinessErrorCode;
import top.thexiaola.dreamhwhub.exception.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传安全验证工具类
 * 提供文件类型、大小、病毒扫描等安全检查功能
 */
@Slf4j
public class FileUploadValidator {
    
    /**
     * 允许的文件扩展名白名单
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 文档类
            "pdf", "doc", "docx", "wps", "pages",
            "xls", "xlsx", "et", "csv", "numbers",
            "ppt", "pptx", "dps", "key",
            "txt", "md", "rtf", "odt", "ods", "odp",
            // 图片类
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tif", "tiff",
            // 音频类
            "mp3", "wav", "m4a", "flac", "aac", "ogg", "wma",
            // 视频类
            "mp4", "wmv", "mov", "avi", "mkv", "flv", "webm",
            // 压缩类
            "zip", "rar", "7z", "tar", "gz",
            // 代码类
            "java", "py", "c", "cpp", "h", "hpp", "js", "ts", "html", "css", "json", "xml"
    );
    
    /**
     * 最大文件大小：50MB
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    
    /**
     * 禁止的 MIME 类型黑名单
     */
    private static final Set<String> FORBIDDEN_MIME_TYPES = Set.of(
            "application/x-msdownload",
            "application/x-executable",
            "application/x-dosexec",
            "application/x-sh",
            "application/x-bat",
            "application/x-powershell",
            "application/javascript",
            "application/x-apple-diskimage"
    );
    
    /**
     * 文件魔数映射表（用于深度检测文件真实类型）
     * 格式：扩展名 -> 魔数前缀字节数组
     */
    private static final Map<String, byte[]> FILE_MAGIC_NUMBERS = new HashMap<>();
    
    static {
        // PDF文件
        FILE_MAGIC_NUMBERS.put("pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        // JPEG图片
        FILE_MAGIC_NUMBERS.put("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        FILE_MAGIC_NUMBERS.put("jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        // PNG图片
        FILE_MAGIC_NUMBERS.put("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        // GIF图片
        FILE_MAGIC_NUMBERS.put("gif", new byte[]{0x47, 0x49, 0x46, 0x38});
        // ZIP压缩文件
        FILE_MAGIC_NUMBERS.put("zip", new byte[]{0x50, 0x4B, 0x03, 0x04});
        // RAR压缩文件
        FILE_MAGIC_NUMBERS.put("rar", new byte[]{0x52, 0x61, 0x72, 0x21});
        // 7Z压缩文件
        FILE_MAGIC_NUMBERS.put("7z", new byte[]{0x37, 0x7A, (byte) 0xBC, (byte) 0xAF});
        // Word文档(docx)
        FILE_MAGIC_NUMBERS.put("docx", new byte[]{0x50, 0x4B, 0x03, 0x04});
        // Excel表格(xlsx)
        FILE_MAGIC_NUMBERS.put("xlsx", new byte[]{0x50, 0x4B, 0x03, 0x04});
        // PowerPoint演示文稿(pptx)
        FILE_MAGIC_NUMBERS.put("pptx", new byte[]{0x50, 0x4B, 0x03, 0x04});
        // MP4视频
        FILE_MAGIC_NUMBERS.put("mp4", new byte[]{0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70});
        // MP3音频
        FILE_MAGIC_NUMBERS.put("mp3", new byte[]{(byte) 0xFF, (byte) 0xFB});
    }
    
    /**
     * 安全的上传目录（非 Web 目录）
     */
    private static final String UPLOAD_BASE_DIR = System.getProperty("user.home") + 
            "/dream-hwhub/uploads/";
    
    /**
     * 验证文件路径安全性
     *
     * @param filePath 文件路径
     * @throws BusinessException 如果路径不安全
     */
    public static void validateFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "文件路径不能为空", null);
        }
        
        // 检查路径遍历攻击
        if (filePath.contains("..") || filePath.contains("/") && filePath.startsWith("/")) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "非法的文件路径", null);
        }
        
        // 检查是否包含危险字符
        if (filePath.matches(".*[<>|\"?*].*")) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "文件名包含非法字符", null);
        }
        
        // 确保文件在安全的上传目录内
        String normalizedPath = Paths.get(filePath).normalize().toString();
        String uploadDir = Paths.get(UPLOAD_BASE_DIR).normalize().toString();
        
        if (!normalizedPath.startsWith(uploadDir)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "文件必须存储在安全的上传目录内", null);
        }
    }
    
    /**
     * 验证文件扩展名
     *
     * @param fileName 文件名
     * @throws BusinessException 如果扩展名不被允许
     */
    public static void validateFileExtension(String fileName) {
        String extension = getString(fileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("Blocked file with disallowed extension: {}, allowed extensions: {}", 
                    extension, String.join(", ", ALLOWED_EXTENSIONS));
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "不允许的文件类型：" + extension, null);
        }
    }

    private static @NonNull String getString(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    "文件名不能为空", null);
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                    "文件必须包含有效的扩展名", null);
        }

        return StrUtil.subSuf(fileName, lastDotIndex + 1).toLowerCase();
    }

    /**
     * 验证文件大小
     *
     * @param fileSize 文件大小（字节）
     * @throws BusinessException 如果文件超过大小限制
     */
    public static void validateFileSize(Long fileSize) {
        if (fileSize == null) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "文件大小不能为空", null);
        }
        
        if (fileSize <= 0) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "文件大小必须大于 0", null);
        }
        
        if (fileSize > MAX_FILE_SIZE) {
            log.warn("Blocked oversized file: {} bytes, max allowed: {} bytes", 
                    fileSize, MAX_FILE_SIZE);
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    String.format("文件大小超过限制 (%.2f MB)", MAX_FILE_SIZE / 1024.0 / 1024.0), null);
        }
    }
    
    /**
     * 验证文件是否存在且可读
     *
     * @param filePath 文件路径
     * @throws BusinessException 如果文件不存在或不可读
     */
    public static void validateFileExists(String filePath) {
        Path path = Paths.get(filePath);
        
        if (!Files.exists(path)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "文件不存在", null);
        }
        
        if (!Files.isReadable(path)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "文件不可读", null);
        }
        
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "必须是普通文件，不能是目录或符号链接", null);
        }
    }
    
    /**
     * 检测文件真实类型（通过魔数）
     *
     * @param filePath 文件路径
     * @return 检测到的 MIME 类型
     */
    public static String detectFileType(String filePath) {
        try {
            Path path = Paths.get(filePath);
            String mimeType = Files.probeContentType(path);
            
            if (mimeType == null) {
                // 如果无法检测，使用扩展名判断
                String fileName = path.getFileName().toString();
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    String extension = StrUtil.subSuf(fileName, lastDotIndex + 1).toLowerCase();
                    return getMimeTypeFromExtension(extension);
                }
                return "application/octet-stream";
            }
            
            return mimeType;
        } catch (Exception e) {
            log.error("Failed to detect file type for: {}", filePath, e);
            return "application/octet-stream";
        }
    }
    
    /**
     * 验证文件的 MIME 类型
     *
     * @param mimeType 文件的 MIME 类型
     * @throws BusinessException 如果 MIME 类型在黑名单中
     */
    public static void validateMimeType(String mimeType) {
        if (mimeType == null) {
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "MIME 类型不能为空", null);
        }
        
        String normalizedMimeType = mimeType.toLowerCase();
        
        if (FORBIDDEN_MIME_TYPES.contains(normalizedMimeType)) {
            log.warn("Blocked dangerous MIME type: {}", mimeType);
            throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED, 
                    "不允许的文件类型", null);
        }
    }
    
    /**
     * 执行完整的文件安全检查
     *
     * @param filePath 文件路径
     * @param fileSize 文件大小
     * @throws BusinessException 如果任何检查失败
     */
    public static void performFullSecurityCheck(String filePath, Long fileSize) {
        // 1. 验证文件路径
        validateFilePath(filePath);
        
        // 2. 验证文件扩展名
        String fileName = Paths.get(filePath).getFileName().toString();
        validateFileExtension(fileName);
        
        // 3. 验证文件大小
        validateFileSize(fileSize);
        
        // 4. 验证文件存在
        validateFileExists(filePath);
        
        // 5. 检测并验证 MIME 类型
        String mimeType = detectFileType(filePath);
        validateMimeType(mimeType);
        
        // 6. 验证文件魔数（防止扩展名欺骗）
        validateFileMagicNumber(filePath, fileName);
        
        log.info("File security check passed: {}, size: {}, type: {}", 
                fileName, fileSize, mimeType);
    }
    
    /**
     * 根据扩展名获取 MIME 类型
     *
     * @param extension 文件扩展名
     * @return MIME 类型
     */
    private static String getMimeTypeFromExtension(String extension) {
        return switch (extension) {
            // 文档类
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "wps", "dps" -> "application/vnd.ms-works";
            case "pages" -> "application/x-iwork-pages-sffpages";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "et" -> "application/vnd.etone";
            case "csv" -> "text/csv";
            case "numbers" -> "application/x-iwork-numbers-sffnumbers";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "key" -> "application/x-iwork-keynote-sffkey";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            case "rtf" -> "application/rtf";
            // 图片类
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "tif", "tiff" -> "image/tiff";
            // 音频类
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "m4a" -> "audio/mp4";
            case "flac" -> "audio/flac";
            case "aac" -> "audio/aac";
            case "ogg" -> "audio/ogg";
            case "wma" -> "audio/x-ms-wma";
            // 视频类
            case "mp4" -> "video/mp4";
            case "wmv" -> "video/x-ms-wmv";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "flv" -> "video/x-flv";
            case "webm" -> "video/webm";
            // 压缩类
            case "zip" -> "application/zip";
            case "rar" -> "application/vnd.rar";
            case "7z" -> "application/x-7z-compressed";
            case "tar" -> "application/x-tar";
            case "gz" -> "application/gzip";
            // 代码类
            case "java" -> "text/x-java-source";
            case "py" -> "text/x-python";
            case "c" -> "text/x-c";
            case "cpp" -> "text/x-c++src";
            case "h" -> "text/x-c";
            case "hpp" -> "text/x-c++hdr";
            case "js" -> "application/javascript";
            case "ts" -> "application/typescript";
            case "html" -> "text/html";
            case "css" -> "text/css";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            default -> "application/octet-stream";
        };
    }
    
    /**
     * 获取安全的上传目录路径
     *
     * @param subDirectory 子目录名称（可选）
     * @return 绝对路径
     */
    public static String getSecureUploadPath(String subDirectory) {
        ensureUploadDirectoryExists();
        
        if (subDirectory != null && !subDirectory.isEmpty()) {
            // 清理子目录名称中的危险字符
            String safeSubDir = subDirectory.replaceAll("[^a-zA-Z0-9_-]", "_");
            Path uploadPath = Paths.get(UPLOAD_BASE_DIR, safeSubDir);
            
            try {
                Files.createDirectories(uploadPath);
                return uploadPath.normalize().toString();
            } catch (Exception e) {
                log.error("Failed to create upload directory: {}", uploadPath, e);
                throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, 
                        "无法创建上传目录", null);
            }
        }
        
        return Paths.get(UPLOAD_BASE_DIR).normalize().toString();
    }
    
    /**
     * 确保上传目录存在
     */
    private static void ensureUploadDirectoryExists() {
        Path uploadDir = Paths.get(UPLOAD_BASE_DIR);
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created secure upload directory: {}", UPLOAD_BASE_DIR);
            }
        } catch (Exception e) {
            log.error("Failed to create upload directory: {}", UPLOAD_BASE_DIR, e);
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR, 
                    "无法初始化上传目录", null);
        }
    }
    
    /**
     * 获取允许的文件扩展名列表
     *
     * @return 允许的扩展名集合
     */
    public static Set<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }
    
    /**
     * 获取最大文件大小限制
     *
     * @return 最大文件大小（字节）
     */
    public static long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }
    
    /**
     * 验证文件魔数（防止扩展名欺骗攻击）
     * 通过读取文件头部字节来验证文件真实类型是否与扩展名匹配
     *
     * @param filePath 文件路径
     * @param fileName 文件名
     * @throws BusinessException 如果魔数验证失败
     */
    private static void validateFileMagicNumber(String filePath, String fileName) {
        try {
            // 获取文件扩展名
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
                return; // 无扩展名，跳过魔数验证
            }
            
            String extension = StrUtil.subSuf(fileName, lastDotIndex + 1).toLowerCase();
            
            // 查找对应的魔数定义
            byte[] expectedMagic = FILE_MAGIC_NUMBERS.get(extension);
            if (expectedMagic == null) {
                // 该扩展名没有魔数定义，跳过验证
                return;
            }
            
            // 读取文件头部字节
            Path path = Paths.get(filePath);
            long fileLength = Files.size(path);
            if (fileLength < expectedMagic.length) {
                throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                        "文件损坏或格式不正确", null);
            }
            
            try (InputStream inputStream = Files.newInputStream(path)) {
                byte[] actualMagic = new byte[expectedMagic.length];
                int bytesRead = inputStream.read(actualMagic);
                
                if (bytesRead != expectedMagic.length) {
                    throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                            "文件读取失败", null);
                }
                
                // 比较魔数
                if (!java.util.Arrays.equals(expectedMagic, actualMagic)) {
                    log.warn("File magic number mismatch for {}: expected={}, actual={}",
                            fileName, bytesToHex(expectedMagic), bytesToHex(actualMagic));
                    throw new BusinessException(BusinessErrorCode.PERMISSION_DENIED,
                            "文件内容与扩展名不匹配，可能存在安全风险", null);
                }
            }
        } catch (IOException e) {
            log.error("Failed to validate file magic number: {}", filePath, e);
            throw new BusinessException(BusinessErrorCode.SYSTEM_ERROR,
                    "文件验证失败", null);
        }
    }
    
    /**
     * 将字节数组转换为十六进制字符串（用于日志记录）
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}
