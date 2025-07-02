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
    private Integer id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 父节点id
     */
    private Integer parentId;

    /**
     * 节点类型 - 0-文件夹，1-文件
     */
    private Integer type;

    /**
     * 内置类型
     */
    private String builtInType;

    /**
     * 文件大小 - 仅文件需要
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String mineType;

    /**
     * 是否开启归档模式（0：否，1：是）默认0
     */
    private Integer archiveMode;

    /**
     * 所属库id
     */
    private Integer libraryId;
}
