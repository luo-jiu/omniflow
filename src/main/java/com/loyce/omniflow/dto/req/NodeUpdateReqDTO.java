package com.loyce.omniflow.dto.req;

import lombok.Data;

@Data
public class NodeUpdateReqDTO {

    /**
     * 主键 id
     */
    private Integer id;

    /**
     * 内置类型
     */
    private String builtInType;

    /**
     * 是否开启归档模式（0：否，1：是）默认0
     */
    private Integer archiveMode;

}
