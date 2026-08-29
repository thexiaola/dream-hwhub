package top.thexiaola.dreamhwhub.module.file_management.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 附件下载 / 预览接口
 *
 * <p>上传文件统一存放在运行目录下的 attachments/ 内（attachments/work 作业附件、
 * attachments/submission 提交附件）。该接口基于数据库中的 file_path 读取文件流返回。
 * 请求需携带 JWT（AuthInterceptor 统一拦截 /api/**），GET 方法不受 CSRF 保护限制。</p>
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    /** 附件根目录（运行目录下的 attachments） */
    private static final Path UPLOAD_ROOT = Paths.get("attachments").toAbsolutePath().normalize();

    /** 旧版附件根目录（历史数据仍存放在 upload/ 下，仅允许读取，不再写入） */
    private static final Path LEGACY_UPLOAD_ROOT = Paths.get("upload").toAbsolutePath().normalize();

    /**
     * 下载或内联预览附件
     *
     * @param path     附件绝对路径（数据库中的 filePath）
     * @param fileName 下载时展示的文件名（可选，默认取原文件名）
     * @param inline   是否内联预览（true 时浏览器直接展示图片/PDF 等，false 时触发下载）
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam("path") String path,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "inline", defaultValue = "false") boolean inline) throws IOException {

        Path target = toSafeUploadPath(path);
        if (target == null) {
            log.warn("附件路径不合法或越界被拒绝: {}", path);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
            log.warn("附件文件不存在或不可读: {}", target);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String name = (fileName == null || fileName.isBlank())
                ? target.getFileName().toString()
                : fileName;

        ContentDisposition disposition = inline
                ? ContentDisposition.inline().filename(name, StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build();

        MediaType mediaType = inline
                ? MediaTypeFactory.getMediaType(target.getFileName().toString())
                        .orElse(MediaType.APPLICATION_OCTET_STREAM)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(Files.newInputStream(target)));
    }

    /**
     * 将请求路径解析为附件根目录内的绝对路径，越界返回 null。
     * 新根为 attachments/，兼容旧根 upload/（历史数据）。
     */
    private static Path toSafeUploadPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        Path target = Paths.get(rawPath).toAbsolutePath().normalize();
        if (!target.startsWith(UPLOAD_ROOT) && !target.startsWith(LEGACY_UPLOAD_ROOT)) {
            return null;
        }
        return target;
    }
}
