package com.loyce.omniflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.loyce.omniflow.dao.mapper")
public class OmniflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(OmniflowApplication.class, args);
    }
}
