package com.loyce.omniflow.controller;

import cn.hutool.core.bean.BeanUtil;
import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.dto.req.UserRegisterReqDTO;
import com.loyce.omniflow.dto.req.UserUpdateReqDTO;
import com.loyce.omniflow.dto.resp.UserActualRespDTO;
import com.loyce.omniflow.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    /**
     * 查询用户信息(不脱敏)
     */
    @GetMapping("/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username) {
        // 使用BeanUtil 包一层返回就不会脱敏了
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
    }

    /**
     * 查询用户名是否存在
     */
    @GetMapping("/exists")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        return Results.success(userService.hasUsername(username));
    }

    /**
     * 注册用户
     */
    @PostMapping()
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UserUpdateReqDTO requestParam) {
        requestParam.setId(id);
        userService.update(requestParam);
        return Results.success();
    }
}
