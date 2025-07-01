package com.example.omniflow.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.omniflow.common.database.BaseDO;
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
     * 库状态 - 0=正常 1=删除
     */
    private Integer delFlag;

}
