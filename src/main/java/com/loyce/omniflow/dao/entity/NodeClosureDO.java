package com.loyce.omniflow.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("node_closure")
public class NodeClosureDO {
    /**
     * 祖先节点id
     */
    private Integer ancestor;

    /**
     * 后代节点id
     */
    private Integer descendant;

    /**
     * 层级深度 0表示自身
     */
    private Integer depth;

    /**
     * 所属库id
     */
    private Integer libraryId;
}
