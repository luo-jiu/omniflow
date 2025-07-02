package com.loyce.omniflow.dto.req;

import lombok.Data;

/**
 * 仓库创建请求参数
 */
@Data
public class LibraryCreateReqDTO {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 仓库名
     */
    private String name;
}
