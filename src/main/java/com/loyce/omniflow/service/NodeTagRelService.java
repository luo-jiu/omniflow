package com.loyce.omniflow.service;

import java.util.List;

public interface NodeTagRelService {

    /**
     * 覆盖节点标签关系（先删后建）
     */
    void replaceNodeTags(Long nodeId, Long libraryId, List<Long> tagIds);

    /**
     * 批量删除节点的标签关系（用于节点彻底删除）
     */
    void deleteByNodeIds(List<Long> nodeIds);
}
