package com.loyce.omniflow.util;

import io.minio.*;
import io.minio.http.Method;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class MinioUtils {
    private final MinioClient minioClient;
    private final String defaultBucket;

    // 初始化MinIO客户端
    public MinioUtils(String endpoint, String accessKey, String secretKey, String defaultBucket) {
        this.minioClient = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
        this.defaultBucket = defaultBucket;
    }

    // 上传文件
    public Boolean uploadFile(String objectName, InputStream stream, long size, String contentType) throws Exception {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(defaultBucket).build());
            }
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectName)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
        return true;
    }

    // 下载文件
    public InputStream downloadFile(String objectName) throws Exception {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    // 生成预签名URL（用于临时访问）
    public String getPreignedUrl(String objectName, int expiryMinutes) throws Exception {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(defaultBucket)
                    .object(objectName)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("生成链接失败: " + e.getMessage(), e);
        }
    }

    // 删除文件
    public void deleteFile(String objectName) throws Exception {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(defaultBucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }
}