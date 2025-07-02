package com.loyce.omniflow.dto.resp;

import lombok.Data;

/**
 * 节点基本信息响应
 */
@Data
public class NodeRespDTO {
    /**
     * ID
     */
    private Integer id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型 - dir or file
     */
    private String type;

    /**
     * 父节点id - null表示自己为 root
     */
    private Integer parentId;

    /**
     * 所属库id
     */
    private Integer libraryId;

}
