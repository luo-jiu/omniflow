package com.loyce.omniflow.service.impl.tag;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.dao.entity.TagDO;
import com.loyce.omniflow.dao.mapper.TagMapper;
import com.loyce.omniflow.dto.req.TagCreateReqDTO;
import com.loyce.omniflow.dto.req.TagUpdateReqDTO;
import com.loyce.omniflow.dto.resp.TagRespDTO;
import com.loyce.omniflow.service.TagService;

import java.util.List;

public class ElasticsearchTagServiceImpl extends ServiceImpl<TagMapper, TagDO> implements TagService {

    @Override
    public String print() {
        return "ElasticSearch";
    }

    @Override
    public List<TagRespDTO> listTags(String type) {
        throw new ClientException("当前搜索引擎不支持标签管理");
    }

    @Override
    public TagRespDTO createTag(TagCreateReqDTO requestParam) {
        throw new ClientException("当前搜索引擎不支持标签管理");
    }

    @Override
    public TagRespDTO updateTag(Long tagId, TagUpdateReqDTO requestParam) {
        throw new ClientException("当前搜索引擎不支持标签管理");
    }

    @Override
    public void deleteTag(Long tagId) {
        throw new ClientException("当前搜索引擎不支持标签管理");
    }
}

