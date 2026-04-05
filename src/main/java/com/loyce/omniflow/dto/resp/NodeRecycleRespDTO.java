package com.loyce.omniflow.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NodeRecycleRespDTO {

    private Long id;

    private String name;

    private String ext;

    private String mimeType;

    private Long fileSize;

    /**
     * 节点类型：0-文件夹，1-文件（服务层会转换成 dir/file）
     */
    private String type;

    private Long parentId;

    private Long libraryId;

    private LocalDateTime deletedAt;

    /**
     * 回收站中该节点的已删除后代数量（不含自身）
     */
    private Integer deletedDescendantCount;
}
