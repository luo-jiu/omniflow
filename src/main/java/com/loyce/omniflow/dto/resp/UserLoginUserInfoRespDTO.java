package com.loyce.omniflow.dto.resp;

import lombok.Data;

@Data
public class UserLoginUserInfoRespDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像链接（从 ext 中解析）
     */
    private String avatar;

    /**
     * 扩展信息（JSON）
     */
    private String ext;
}

