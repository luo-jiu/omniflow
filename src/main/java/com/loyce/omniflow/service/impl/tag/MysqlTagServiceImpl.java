package com.loyce.omniflow.service.impl.tag;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loyce.omniflow.dao.entity.TagDO;
import com.loyce.omniflow.dao.mapper.TagMapper;
import com.loyce.omniflow.service.TagService;

public class MysqlTagServiceImpl extends ServiceImpl<TagMapper, TagDO> implements TagService {

    @Override
    public String print() {
        return "MySQL";
    }

}
