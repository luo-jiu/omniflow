package com.loyce.omniflow.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.loyce.omniflow.common.database.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("tags")
public class TagDO extends BaseDO {
    /**
     * 标签ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签场景类型（ASMR / FILE_TAB / COMIC / GENERAL）
     */
    private String type;

    /**
     * 场景目标键（FILE_TAB 使用，如 IMG/MP3/COMIC）
     */
    private String targetKey;

    /**
     * 归属用户ID，NULL 表示系统标签
     */
    private Long ownerUserId;

    /**
     * 标签主色（HEX）
     */
    private String color;

    /**
     * 标签文字色（HEX，可空）
     */
    private String textColor;

    /**
     * 同场景排序值（越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 启用状态：1 启用，0 停用
     */
    private Integer enabled;

    /**
     * 标签描述
     */
    private String description;
}
