package com.loyce.omniflow.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.loyce.omniflow.common.database.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("library")
public class LibraryDO extends BaseDO {

    /**
     * ID - 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 库名
     */
    private String name;

    /**
     * 是否收藏：0=未收藏，1=已收藏
     */
    private Integer starred;

}
