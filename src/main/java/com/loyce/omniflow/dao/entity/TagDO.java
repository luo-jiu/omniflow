package com.loyce.omniflow.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.loyce.omniflow.common.database.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("tags")
public class TagDO extends BaseDO {
}
