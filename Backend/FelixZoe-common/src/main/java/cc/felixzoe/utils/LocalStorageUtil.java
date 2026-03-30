package cc.felixzoe.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Slf4j
public class LocalStorageUtil {
    private String storagePath;
    private String accessUrl;

    public LocalStorageUtil(String storagePath, String accessUrl) {
        this.storagePath = storagePath;
        this.accessUrl = accessUrl;
    }

    /**
     * 流式文件上传到本地存储
     * ★ 优化：使用 InputStream 流式写入，替代 byte[] 全量加载
     *    内存占用从 O(fileSize) 降至 O(bufferSize=64KB)
     */
    public String upload(InputStream inputStream, String extension, String fileName) {
        String category = getFileCategory(extension);
        String relativePath = category + "/" + fileName;
        try {
            Path dirPath = Paths.get(storagePath, category);
            Files.createDirectories(dirPath);
            Path filePath = Paths.get(storagePath, relativePath);
            // 流式写入，64KB 缓冲区
            try (OutputStream out = Files.newOutputStream(filePath);
                 InputStream in = inputStream) {
                byte[] buffer = new byte[64 * 1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            String fileUrl = accessUrl + "/" + relativePath;
            log.info("文件上传到本地: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("本地文件存储失败: {}", e.getMessage());
            throw new RuntimeException("文件存储失败", e);
        }
    }

    /**
     * 兼容旧接口：byte[] 方式上传（保持向后兼容）
     */
    public String upload(byte[] bytes, String extension, String fileName) {
        String category = getFileCategory(extension);
        String relativePath = category + "/" + fileName;
        try {
            Path dirPath = Paths.get(storagePath, category);
            Files.createDirectories(dirPath);
            Path filePath = Paths.get(storagePath, relativePath);
            Files.write(filePath, bytes);
            String fileUrl = accessUrl + "/" + relativePath;
            log.info("文件上传到本地: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("本地文件存储失败: {}", e.getMessage());
            throw new RuntimeException("文件存储失败", e);
        }
    }

    /** Determine file category based on extension */
    public String getFileCategory(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "png", "gif", "bmp", "webp", "jpeg", "svg", "ico", "tiff" -> "image";
            case "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v", "3gp" -> "video";
            case "mp3", "wav", "wma", "ogg", "aac", "flac", "m4a", "ape", "mid", "midi" -> "audio";
            case "lrc", "lrcx", "krc", "qrc", "trc", "ksc" -> "lyric";
            case "txt", "md", "rtf" -> "text";
            case "pdf" -> "pdf";
            case "doc", "docx", "dot", "dotx" -> "word";
            case "xls", "xlsx", "xlt", "xltx" -> "excel";
            case "zip", "rar", "7z", "tar", "gz", "bz2" -> "archive";
            case "ttf", "otf", "woff", "woff2", "eot" -> "font";
            default -> "other";
        };
    }
}
