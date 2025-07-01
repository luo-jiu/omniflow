package com.example.omniflow.common.biz.user;

import com.alibaba.fastjson2.JSON;
import com.example.omniflow.common.convention.exception.ClientException;
import com.example.omniflow.common.convention.result.Results;
import com.example.omniflow.common.enums.UserErrorCodeEnum;
import com.google.common.collect.Lists;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    private static final List<String> IGNORE_URI = Lists.newArrayList(
            "/api/omniflow/v1/user",
            "/api/omniflow/v1/user/login",
            "/api/omniflow/v1/user/has-username"
    );

    // TODO 未来加入本地缓存 防止多次请求redis
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestURI = request.getRequestURI();

        // 1. 是否在忽略列表
        if (IGNORE_URI.contains(requestURI)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 2. 获取token
        String token = extractToken(request);
        if (token == null) {
            throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
        }

        // 3. 读取用户信息
        Object userInfoJsonStr;
        try {
            userInfoJsonStr = stringRedisTemplate.opsForHash().get("login:" + request.getHeader("username"), token);
            if (userInfoJsonStr == null) {
                throw new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL);
            }
        } catch (Exception e) {
            returnJson((HttpServletResponse)servletResponse, JSON.toJSONString(Results.failure(new ClientException(UserErrorCodeEnum.USER_TOKEN_FAIL))));
            return;
        }
        UserInfoDTO userInfoDTO = JSON.parseObject(userInfoJsonStr.toString(), UserInfoDTO.class);
        UserContext.setUser(userInfoDTO);

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }

    private void returnJson(HttpServletResponse response, String json) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractToken(HttpServletRequest request) {
        // 从 Header 提取 Token
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

}
