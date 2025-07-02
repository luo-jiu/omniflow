package com.loyce.omniflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.loyce.omniflow.dao.entity.UserDO;
import com.loyce.omniflow.dto.req.UserLoginReqDTO;
import com.loyce.omniflow.dto.req.UserRegisterReqDTO;
import com.loyce.omniflow.dto.req.UserUpdateReqDTO;
import com.loyce.omniflow.dto.resp.UserLoginRespDTO;
import com.loyce.omniflow.dto.resp.UserRespDTO;

public interface UserService extends IService<UserDO> {

    /**
     * 根据用户姓名查询用户信息
     *
     * @return 用户返回实体
     */
    UserRespDTO getUserByUsername(String username);

    /**
     * 检查用户名是否存在
     */
    Boolean hasUsername(String username);

    /**
     * 注册用户
     *
     * @param requestParam 用户注册请求参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 根据用户名修改用户
     *
     * @param requestParam 修改用户请求参数
     */
    void update(UserUpdateReqDTO requestParam);

    /**
     * 用户登录
     *
     * @param requestParam 用户登录请求参数
     * @return 用户登录返回参数
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    /**
     * 检查用户是否登录
     *
     * @param username 用户名
     * @param token    用户登录Token
     */
    Boolean checkLogin(String username, String token);

    /**
     * 用户退出登录
     *
     * @param username
     * @param token
     */
    void logout(String username, String token);

}
