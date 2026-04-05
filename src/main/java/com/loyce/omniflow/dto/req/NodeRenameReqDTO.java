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
     * 新扩展名（仅文件）；可为空字符串表示删除后缀
     */
    private String ext;

//    /**
//     * 新的父节点 ID
//     */
//    private Long newParentId;
//
//    /**
//     * 库 ID
//     */
//    private Long libraryId;
}
