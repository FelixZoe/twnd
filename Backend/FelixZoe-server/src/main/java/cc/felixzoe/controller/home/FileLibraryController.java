package cc.felixzoe.controller.home;

import cc.felixzoe.result.Result;
import cc.felixzoe.service.FileLibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 首页端文件库接口（只读，无需登录）
 */
@RestController("homeFileLibraryController")
@RequestMapping("/home/file-library")
@Slf4j
public class FileLibraryController {

    @Autowired
    private FileLibraryService fileLibraryService;

    /**
     * 获取文件列表（公开只读）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> listFiles(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("首页文件库查询: category={}, page={}, pageSize={}", category, page, pageSize);
        Map<String, Object> result = fileLibraryService.listFiles(category, page, pageSize);
        return Result.success(result);
    }

    /**
     * 获取文件分类统计（公开只读）
     */
    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> getStats() {
        List<Map<String, Object>> stats = fileLibraryService.getFileStats();
        return Result.success(stats);
    }
}
