package com.loyce.omniflow.dto.req;

import lombok.Data;

@Data
public class NodeMoveReqDTO {

    /**
     * 节点名称（兼容字段，服务端以数据库中的节点名为准）
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
