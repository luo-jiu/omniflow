package com.loyce.omniflow.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("node_tag_rel")
public class NodeTagRelDO {

    /**
     * 关系主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 所属库ID
     */
    private Long libraryId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
