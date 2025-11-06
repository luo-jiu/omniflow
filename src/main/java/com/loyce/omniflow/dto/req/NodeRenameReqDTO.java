package com.loyce.omniflow.dto.req;

import lombok.Data;

@Data
public class NodeRenameReqDTO {

    /**
     * 主键 id
     */
    private Long id;

    /**
     * 新名称
     */
    private String name;

    /**
     * 新的父节点 ID
     */
    private Long newParentId;

    /**
     * 库 ID
     */
    private Long libraryId;
}
