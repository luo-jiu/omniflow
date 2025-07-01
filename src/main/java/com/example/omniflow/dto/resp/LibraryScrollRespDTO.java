package com.example.omniflow.dto.resp;

import lombok.Data;

/**
 * 库分页返回响应
 */
@Data
public class LibraryScrollRespDTO {

    /**
     * ID - 自增主键
     */
    private Long id;

    /**
     * 用户名 - 用户登录名称
     */
    private String name;

}
