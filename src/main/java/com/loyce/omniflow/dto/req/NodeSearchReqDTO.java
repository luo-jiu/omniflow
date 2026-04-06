package com.loyce.omniflow.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class NodeSearchReqDTO {

    /**
     * 所属库ID
     */
    private Long libraryId;

    /**
     * 节点名称关键字（可空）
     */
    private String keyword;

    /**
     * 标签ID列表（可空）
     */
    private List<Long> tagIds;

    /**
     * 标签匹配模式：ANY / ALL（默认 ANY）
     */
    private String tagMatchMode;

    /**
     * 返回条数上限（默认200，最大500）
     */
    private Integer limit;
}
