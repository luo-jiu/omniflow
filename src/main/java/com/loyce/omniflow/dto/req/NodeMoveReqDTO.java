package com.loyce.omniflow.dto.req;

import lombok.Data;

@Data
public class NodeMoveReqDTO {

    /**
     * 要移动的文件名
     */
    private String name;

    /**
     * 要移动的节点 ID
     */
    private Long nodeId;

    /**
     * 新的父节点 ID
     */
    private Long newParentId;

    /**
     * 插入到哪个节点前（可选）
     */
    private Long beforeNodeId;

    /**
     * 库 ID
     */
    private Long libraryId;

}
