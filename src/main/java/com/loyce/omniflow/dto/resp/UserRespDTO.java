package com.loyce.omniflow.dto.resp;

import lombok.Data;

/**
 * 用户返回参数响应
 */
@Data
public class UserRespDTO {
    /**
     * ID - 自增主键
     */
    private Long id;

    /**
     * 用户名 - 用户登录名称
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 扩展信息（JSON）
     */
    private String ext;

    /**
     * 头像访问地址（由 ext.avatarKey 换算）
     */
    private String avatar;
}
