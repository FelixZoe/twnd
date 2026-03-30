package cc.felixzoe.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileLibraryService {

    /**
     * 文件库专用上传
     * @param file 文件
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file);

    /**
     * 获取文件列表
     * @param category 文件分类
     * @param page 页码
     * @param pageSize 每页数量
     * @return 文件列表和分页信息
     */
    Map<String, Object> listFiles(String category, Integer page, Integer pageSize);

    /**
     * 获取文件分类统计
     * @return 各分类文件数量和大小
     */
    List<Map<String, Object>> getFileStats();

    /**
     * 删除文件
     * @param fileName 文件名（如 xxx.webp）
     */
    void deleteFile(String fileName);

    /**
     * 批量删除文件
     * @param fileNames 文件名列表
     */
    void batchDeleteFiles(List<String> fileNames);
}
