package com.loyce.omniflow.controller;

import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.util.MinioUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final MinioUtils minioUtils;

    /**
     * 上传文件到 MinIO
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "path", required = false) String path) {
        try {
            if (path == null || path.isBlank()) {
                path = "default";
            }
            String objectName = String.format("%s/%s", path, file.getOriginalFilename());
            minioUtils.uploadFile(
                    objectName,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
            // 生成一个临时访问链接（例如 60 分钟有效）
            String url = minioUtils.getPreignedUrl(objectName, 60);
            return Results.success(url);
        } catch (Exception e) {
            return Results.failure(e.toString());
        }
    }

    /**
     * 获取文件的临时访问链接
     */
    @GetMapping("/link")
    public Result<String> getFileLink(
            @RequestParam(name = "file_name") String fileName,
            @RequestParam(name = "path", required = false) String path,
            @RequestParam(defaultValue = "60") int expiry) {
        try {
            if (path == null || path.isBlank()) {
                path = "default";
            }
            String objectName = String.format("%s/%s", path, fileName);
            String url = minioUtils.getPreignedUrl(objectName, expiry);
            return Results.success(url);
        } catch (Exception e) {
            return Results.failure("获取链接失败: " + e.getMessage());
        }
    }
}
