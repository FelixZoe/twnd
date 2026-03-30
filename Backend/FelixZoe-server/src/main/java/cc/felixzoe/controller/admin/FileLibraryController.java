package cc.felixzoe.controller.admin;

import cc.felixzoe.result.Result;
import cc.felixzoe.service.FileLibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 管理端文件库接口
 */
@RestController("adminFileLibraryController")
@RequestMapping("/admin/file-library")
@Slf4j
public class FileLibraryController {

    @Autowired
    private FileLibraryService fileLibraryService;

    /**
     * 文件库专用上传（存到 file-library 子目录）
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(MultipartFile file) {
        log.info("文件库上传: {}", file.getOriginalFilename());
        String fileUrl = fileLibraryService.uploadFile(file);
        return Result.success(fileUrl);
    }

    /**
     * 获取文件列表（可按分类筛选）
     * @param category 文件分类（image/audio/video等，为空则返回全部）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listFiles(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("查询文件库: category={}, page={}, pageSize={}", category, page, pageSize);
        Map<String, Object> result = fileLibraryService.listFiles(category, page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取文件分类统计
     */
    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> getStats() {
        log.info("查询文件统计");
        List<Map<String, Object>> stats = fileLibraryService.getFileStats();
        return Result.success(stats);
    }

    /**
     * 删除文件
     * @param fileName 文件名（如 xxx.webp）
     */
    @DeleteMapping
    public Result<Void> deleteFile(@RequestParam String fileName) {
        log.info("删除文件: {}", fileName);
        fileLibraryService.deleteFile(fileName);
        return Result.success();
    }

    /**
     * 批量删除文件
     * @param fileNames 文件名列表
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteFiles(@RequestBody List<String> fileNames) {
        log.info("批量删除文件: {}", fileNames);
        fileLibraryService.batchDeleteFiles(fileNames);
        return Result.success();
    }
}
