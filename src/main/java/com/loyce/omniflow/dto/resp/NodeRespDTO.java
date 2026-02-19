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
}

