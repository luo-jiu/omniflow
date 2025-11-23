package com.loyce.omniflow.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatMultipartConfig {

    @PostConstruct
    public void init() {
        // 设置系统属性
        System.setProperty("org.apache.tomcat.util.http.fileupload.MultipartStream.HEADER_PART_SIZE_MAX", "8192");
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_LENGTH_LIMIT", "209715200");
    }
}