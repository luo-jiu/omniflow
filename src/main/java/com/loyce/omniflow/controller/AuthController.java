package com.loyce.omniflow.controller;

import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.dto.req.UserLoginReqDTO;
import com.loyce.omniflow.dto.resp.UserLoginRespDTO;
import com.loyce.omniflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO req) {
        return Results.success(userService.login(req));
    }

    /**
     * 查询登录状态
     */
    @GetMapping("/status")
    public Result<Boolean> checkLogin(@RequestParam String username, @RequestParam String token) {
        return Results.success(userService.checkLogin(username, token));
    }

    /**
     * 等出
     */
    @DeleteMapping("/logout")
    public Result<Void> logout(@RequestParam String username, @RequestParam String token) {
        userService.logout(username, token);
        return Results.success();
    }
}
