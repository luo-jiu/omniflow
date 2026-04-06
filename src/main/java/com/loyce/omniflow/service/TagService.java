package com.loyce.omniflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.loyce.omniflow.dao.entity.TagDO;
import com.loyce.omniflow.dto.req.TagCreateReqDTO;
import com.loyce.omniflow.dto.req.TagUpdateReqDTO;
import com.loyce.omniflow.dto.resp.TagRespDTO;

import java.util.List;

public interface TagService extends IService<TagDO> {
    String print();

    List<TagRespDTO> listTags(String type);

    TagRespDTO createTag(TagCreateReqDTO requestParam);

    TagRespDTO updateTag(Long tagId, TagUpdateReqDTO requestParam);

    void deleteTag(Long tagId);
}
