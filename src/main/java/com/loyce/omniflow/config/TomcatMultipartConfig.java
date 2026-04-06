package com.loyce.omniflow.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatMultipartConfig {

    @Value("${omniflow.upload.content-length-limit-bytes:11811160064}")
    private String contentLengthLimitBytes;

    @PostConstruct
    public void init() {
        // 设置系统属性
        System.setProperty("org.apache.tomcat.util.http.fileupload.MultipartStream.HEADER_PART_SIZE_MAX", "8192");
        // 需大于业务允许的 max-file-size，并给 multipart 边界留余量。
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_LENGTH_LIMIT", contentLengthLimitBytes);
    }
}
