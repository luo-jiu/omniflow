package com.loyce.omniflow.dto.req;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeCreateReqDTO {
    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型 - 0-文件夹，1-文件
     */
    private Integer type;

    /**
     * 父节点id - null表示自己为 root
     */
    private Long parentId;

    /**
     * 所属库id
     */
    private Long libraryId;

    /**
     * 文件大小 - 仅文件需要
     */
    private Long fileSize;

    /**
     * 文件类型 - 仅文件需要
     */
    private String mineType;
}
