package cc.felixzoe.service.impl;

import cc.felixzoe.constant.MessageConstant;
import cc.felixzoe.exception.UploadFileErrorException;
import cc.felixzoe.service.FileLibraryService;
import cc.felixzoe.utils.LocalStorageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileLibraryServiceImpl implements FileLibraryService {

    @Autowired
    private LocalStorageUtil localStorageUtil;

    /**
     * 文件库根目录：{storagePath}/file-library/
     */
    private static final String FILE_LIBRARY_DIR = "file-library";

    /**
     * 流式写入缓冲区大小 (64KB) —— 避免一次性加载整个文件到堆内存
     */
    private static final int STREAM_BUFFER_SIZE = 64 * 1024;

    private Path getLibraryRoot() {
        return Paths.get(localStorageUtil.getStoragePath(), FILE_LIBRARY_DIR);
    }

    private String getLibraryAccessUrl() {
        return localStorageUtil.getAccessUrl() + "/" + FILE_LIBRARY_DIR;
    }

    @Override
    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new UploadFileErrorException(MessageConstant.FILE_EMPTY);
        }
        try {
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf(".") + 1);
            }

            // 用 UUID 作文件名，保留原始扩展名
            String newFileName = UUID.randomUUID() + "." + extension;

            Path libraryRoot = getLibraryRoot();
            Files.createDirectories(libraryRoot);

            Path filePath = libraryRoot.resolve(newFileName);

            // ★ 优化核心：使用 transferTo() 流式写入，避免 file.getBytes() 将整个文件加载到堆内存
            // 对于大文件（>1MB），内存占用从 O(fileSize) 降到 O(bufferSize=64KB)
            file.transferTo(filePath.toFile());

            String fileUrl = getLibraryAccessUrl() + "/" + newFileName;
            log.info("文件库上传: {} ({}KB)", fileUrl, file.getSize() / 1024);
            return fileUrl;
        } catch (IOException e) {
            log.error("文件库上传失败", e);
            throw new UploadFileErrorException(MessageConstant.UPLOAD_FAILED);
        }
    }

    @Override
    public Map<String, Object> listFiles(String category, Integer page, Integer pageSize) {
        List<Map<String, Object>> allFiles = new ArrayList<>();

        try {
            Path libraryRoot = getLibraryRoot();
            if (!Files.exists(libraryRoot)) {
                return buildPageResult(allFiles, 0, page, pageSize);
            }

            try (Stream<Path> files = Files.list(libraryRoot)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                        String fileName = file.getFileName().toString();
                        String ext = "";
                        int dotIdx = fileName.lastIndexOf('.');
                        if (dotIdx > 0) {
                            ext = fileName.substring(dotIdx + 1).toLowerCase();
                        }

                        String fileCat = localStorageUtil.getFileCategory(ext);

                        // 如果指定了分类筛选
                        if (category != null && !category.isEmpty() && !category.equals(fileCat)) {
                            return;
                        }

                        Map<String, Object> fileInfo = new LinkedHashMap<>();
                        fileInfo.put("name", fileName);
                        fileInfo.put("category", fileCat);
                        fileInfo.put("url", getLibraryAccessUrl() + "/" + fileName);
                        fileInfo.put("size", attrs.size());
                        fileInfo.put("extension", ext);
                        fileInfo.put("createdTime", attrs.creationTime().toMillis());
                        fileInfo.put("modifiedTime", attrs.lastModifiedTime().toMillis());

                        allFiles.add(fileInfo);
                    } catch (IOException e) {
                        log.warn("读取文件属性失败: {}", file, e);
                    }
                });
            }

            // 按修改时间倒序
            allFiles.sort((a, b) -> Long.compare(
                    (Long) b.get("modifiedTime"),
                    (Long) a.get("modifiedTime")
            ));

        } catch (IOException e) {
            log.error("扫描文件库目录失败", e);
        }

        return buildPageResult(allFiles, allFiles.size(), page, pageSize);
    }

    @Override
    public List<Map<String, Object>> getFileStats() {
        List<Map<String, Object>> stats = new ArrayList<>();

        try {
            Path libraryRoot = getLibraryRoot();
            if (!Files.exists(libraryRoot)) {
                Map<String, Object> totalStat = new LinkedHashMap<>();
                totalStat.put("category", "all");
                totalStat.put("count", 0L);
                totalStat.put("size", 0L);
                stats.add(totalStat);
                return stats;
            }

            // 按文件扩展名分类统计
            Map<String, long[]> catStats = new LinkedHashMap<>();
            long totalCount = 0;
            long totalSize = 0;

            try (Stream<Path> files = Files.list(libraryRoot)) {
                List<Path> fileList = files.filter(Files::isRegularFile).toList();
                for (Path f : fileList) {
                    try {
                        long size = Files.size(f);
                        String fileName = f.getFileName().toString();
                        String ext = "";
                        int dotIdx = fileName.lastIndexOf('.');
                        if (dotIdx > 0) {
                            ext = fileName.substring(dotIdx + 1).toLowerCase();
                        }
                        String cat = localStorageUtil.getFileCategory(ext);

                        catStats.computeIfAbsent(cat, k -> new long[]{0, 0});
                        catStats.get(cat)[0]++;
                        catStats.get(cat)[1] += size;
                        totalCount++;
                        totalSize += size;
                    } catch (IOException ignored) {
                    }
                }
            }

            // 总计
            Map<String, Object> totalStat = new LinkedHashMap<>();
            totalStat.put("category", "all");
            totalStat.put("count", totalCount);
            totalStat.put("size", totalSize);
            stats.add(totalStat);

            // 各分类
            for (Map.Entry<String, long[]> entry : catStats.entrySet()) {
                Map<String, Object> stat = new LinkedHashMap<>();
                stat.put("category", entry.getKey());
                stat.put("count", entry.getValue()[0]);
                stat.put("size", entry.getValue()[1]);
                stats.add(stat);
            }

        } catch (IOException e) {
            log.error("统计文件库信息失败", e);
        }

        return stats;
    }

    @Override
    public void deleteFile(String fileName) {
        try {
            // 安全检查：防止路径穿越
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new RuntimeException("非法文件名");
            }
            Path filePath = getLibraryRoot().resolve(fileName);
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                Files.delete(filePath);
                log.info("文件库删除: {}", fileName);
            } else {
                log.warn("文件不存在: {}", fileName);
            }
        } catch (IOException e) {
            log.error("删除文件失败: {}", fileName, e);
            throw new RuntimeException("删除文件失败");
        }
    }

    @Override
    public void batchDeleteFiles(List<String> fileNames) {
        for (String fileName : fileNames) {
            try {
                deleteFile(fileName);
            } catch (Exception e) {
                log.warn("批量删除中，文件删除失败: {}", fileName, e);
            }
        }
    }

    private Map<String, Object> buildPageResult(List<Map<String, Object>> allFiles, long total, int page, int pageSize) {
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allFiles.size());

        List<Map<String, Object>> pageData;
        if (fromIndex >= allFiles.size()) {
            pageData = new ArrayList<>();
        } else {
            pageData = allFiles.subList(fromIndex, toIndex);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("records", pageData);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }
}
