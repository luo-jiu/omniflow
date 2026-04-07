package com.loyce.omniflow.dto.req;

import lombok.Data;

/**
 * 仓库修改请求参数
 */
@Data
public class LibraryUpdateReqDTO {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 仓库名
     */
    private String name;

    /**
     * 是否收藏：0=未收藏，1=已收藏
     */
    private Integer starred;
}
