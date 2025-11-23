package com.loyce.omniflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;

@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {

    /**
     * 配置全局 CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 允许前端访问的域名，可以用 * 代表所有，用于本地调试
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置全局 CSP Header
     */
//    @Bean
//    public Filter cspFilter() {
//        return (ServletRequest request, ServletResponse response, FilterChain chain)
//                -> {
//            HttpServletResponse httpResponse = (HttpServletResponse) response;
//            httpResponse.setHeader(
//                    "Content-Security-Policy",
//                    // 允许自己域名 + 后端 API 地址 + WebSocket 地址访问
//                    "default-src 'self'; " +
//                            "connect-src 'self' http://localhost:8848 ws://localhost:8848; " +
//                            "img-src 'self' data: blob:; " +
//                            "script-src 'self' 'unsafe-inline'; " +
//                            "style-src 'self' 'unsafe-inline';"
//            );
//            chain.doFilter(request, response);
//        };
//    }
}