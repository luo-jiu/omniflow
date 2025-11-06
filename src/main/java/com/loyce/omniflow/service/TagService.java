package com.loyce.omniflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.loyce.omniflow.dao.entity.TagDO;

public interface TagService extends IService<TagDO> {
    String print();
}
