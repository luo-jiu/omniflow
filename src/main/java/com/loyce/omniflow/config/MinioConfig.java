package com.loyce.omniflow.config;

import com.loyce.omniflow.util.MinioUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Value("${minio.defaultBucket}")
    private String defaultBucket;

    @Bean
    public MinioUtils minioUtils() {
        return new MinioUtils(endpoint, accessKey, secretKey, defaultBucket);
    }
}
