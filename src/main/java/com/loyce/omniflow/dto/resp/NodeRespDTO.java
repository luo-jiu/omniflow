package com.loyce.omniflow.dto.resp;

import lombok.Data;

/**
 * 节点基本信息响应
 */
@Data
public class NodeRespDTO {

    private Integer id;

    private String name;

    /**
     * dir / file
     */
    private String type;

    private Integer parentId;

    private Integer libraryId;

    /**
     * 文件扩展名
     */
    private String ext;

    /**
     * MIME 类型
     */
    private String mimeType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 内置类型（文件/文件夹均可）
     */
    private String builtInType;

    /**
     * 是否开启归档模式（0：否，1：是）
     */
    private Integer archiveMode;

    /**
     * 视图扩展元数据（JSON）
     */
    private String viewMeta;
}
