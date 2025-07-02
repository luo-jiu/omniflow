package com.loyce.omniflow.dto.resp;

import lombok.Data;

@Data
public class NodePathRespDTO {
    /**
     * 节点id
     */
    private Integer id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点深度
     */
    private Integer depth;

}
