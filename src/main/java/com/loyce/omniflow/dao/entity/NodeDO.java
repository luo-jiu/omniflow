package com.loyce.omniflow.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.loyce.omniflow.common.database.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("nodes")
public class NodeDO extends BaseDO {
    /**
     * ID - 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 文件扩展名（仅文件）
     */
    private String ext;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 文件大小 - 仅文件需要
     */
    private Long fileSize;

    /**
     * 对象存储 Key
     */
    private String storageKey;

    /**
     * 内置类型
     */
    private String builtInType;

    /**
     * 节点类型 - 0-文件夹，1-文件
     */
    private Integer type;

    /**
     * 是否开启归档模式（0：否，1：是）默认0
     */
    private Integer archiveMode;

    /**
     * 视图扩展元数据（JSON）
     */
    private String viewMeta;

    /**
     * 同级节点排序
     */
    private Integer sortOrder;

    /**
     * 父节点id
     */
    private Long parentId;

    /**
     * 所属库id
     */
    private Long libraryId;
}
