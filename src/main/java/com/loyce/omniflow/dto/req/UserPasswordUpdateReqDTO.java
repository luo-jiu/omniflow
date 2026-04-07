package com.loyce.omniflow.dto.req;

import lombok.Data;

@Data
public class UserPasswordUpdateReqDTO {

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;
}

