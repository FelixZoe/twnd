package cc.felixzoe.service.impl;

import cc.felixzoe.constant.MessageConstant;
import cc.felixzoe.exception.UploadFileErrorException;
import cc.felixzoe.properties.ImageProperties;
import cc.felixzoe.service.CommonService;
import cc.felixzoe.utils.LocalStorageUtil;
import cc.felixzoe.utils.ImageCompressUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class CommonServiceImpl implements CommonService {

    @Autowired
    private LocalStorageUtil localStorageUtil;
    @Autowired
    private ImageCompressUtil imageCompressUtil;
    @Autowired
    private ImageProperties imageProperties;

    /**
     * 文件上传（本地存储版）
     */
    // Allowed file extensions whitelist
    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "ico", "svg",
        "mp4", "avi", "mov", "mkv", "webm",
        "mp3", "wav", "ogg", "aac", "flac", "m4a",
        "lrc", "lrcx", "krc",
        "pdf", "doc", "docx", "xls", "xlsx",
        "zip", "rar", "7z",
        "ttf", "otf", "woff", "woff2"
    );

    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new UploadFileErrorException(MessageConstant.FILE_EMPTY);
        }
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.contains(".")) {
                throw new UploadFileErrorException("文件名无效");
            }
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            // Security: check against whitelist
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new UploadFileErrorException("不支持的文件类型: ." + extension);
            }
            byte[] bytes = file.getBytes();

            // 如果是图片，先压缩
            if (localStorageUtil.getFileCategory(extension).equals("image")) {
                bytes = imageCompressUtil.compress(file);
                extension = imageProperties.getOutPutFormat();
            }

            String uuidFileName = UUID.randomUUID() + "." + extension;
            String fileUrl = localStorageUtil.upload(bytes, extension, uuidFileName);

            return fileUrl;

        } catch (IOException e) {
            throw new UploadFileErrorException(MessageConstant.UPLOAD_FAILED);
        }
    }
}
