package com.loyce.omniflow.common.biz.user;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.common.enums.UserErrorCodeEnum;
import com.loyce.omniflow.common.enums.RedisPrefixCodeEnum;
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

    private static final List<IgnoredRoute> IGNORE_ROUTES = Lists.newArrayList(
            new IgnoredRoute("/api/v1/auth/register", "POST"),
            new IgnoredRoute("/api/v1/auth/login", "POST"),
            new IgnoredRoute("/api/v1/auth/exists", "GET"),
            new IgnoredRoute("/api/v1/user", "POST"),
            new IgnoredRoute("/api/v1/user/exists", "GET"),
            new IgnoredRoute("/api/v1/files/upload", null),
            new IgnoredRoute("/api/v1/files/link", null),
            new IgnoredRoute("/api/v1/directory/upload", null)
    );

    // TODO 未来加入本地缓存 防止多次请求redis
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();

        // 1. 是否在忽略列表（路径 + 方法）
        if (isIgnoredRoute(requestURI, requestMethod)) {
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
            userInfoJsonStr = stringRedisTemplate.opsForHash().get(RedisPrefixCodeEnum.USER_LOGIN_CODING + request.getHeader("username"), token);
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

    private boolean isIgnoredRoute(String requestURI, String requestMethod) {
        for (IgnoredRoute route : IGNORE_ROUTES) {
            if (route.matches(requestURI, requestMethod)) {
                return true;
            }
        }
        return false;
    }

    private static final class IgnoredRoute {
        private final String uri;
        private final String method;

        private IgnoredRoute(String uri, String method) {
            this.uri = uri;
            this.method = method;
        }

        private boolean matches(String requestURI, String requestMethod) {
            if (!uri.equals(requestURI)) {
                return false;
            }
            return method == null || method.equalsIgnoreCase(requestMethod);
        }
    }

}
