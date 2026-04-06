package com.loyce.omniflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.loyce.omniflow.dao.entity.NodeTagRelDO;
import com.loyce.omniflow.dao.entity.TagDO;
import com.loyce.omniflow.dao.mapper.NodeTagRelMapper;
import com.loyce.omniflow.dao.mapper.TagMapper;
import com.loyce.omniflow.service.NodeTagRelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NodeTagRelServiceImpl implements NodeTagRelService {

    private final NodeTagRelMapper nodeTagRelMapper;
    private final TagMapper tagMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceNodeTags(Long nodeId, Long libraryId, List<Long> tagIds) {
        if (nodeId == null || nodeId <= 0 || libraryId == null || libraryId <= 0) {
            return;
        }

        nodeTagRelMapper.deleteByNodeId(nodeId);

        List<Long> normalizedTagIds = normalizeTagIds(tagIds);
        if (normalizedTagIds.isEmpty()) {
            return;
        }

        List<Long> validTagIds = filterValidTagIds(normalizedTagIds);
        if (validTagIds.isEmpty()) {
            return;
        }

        List<NodeTagRelDO> rels = new ArrayList<>(validTagIds.size());
        for (Long tagId : validTagIds) {
            NodeTagRelDO rel = new NodeTagRelDO();
            rel.setNodeId(nodeId);
            rel.setTagId(tagId);
            rel.setLibraryId(libraryId);
            rels.add(rel);
        }
        nodeTagRelMapper.batchInsert(rels);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByNodeIds(List<Long> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return;
        }
        List<Long> normalized = new ArrayList<>();
        for (Long nodeId : nodeIds) {
            if (nodeId != null && nodeId > 0) {
                normalized.add(nodeId);
            }
        }
        if (normalized.isEmpty()) {
            return;
        }
        nodeTagRelMapper.deleteByNodeIds(normalized);
    }

    private List<Long> normalizeTagIds(List<Long> rawTagIds) {
        if (rawTagIds == null || rawTagIds.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long tagId : rawTagIds) {
            if (tagId != null && tagId > 0) {
                unique.add(tagId);
            }
        }
        return new ArrayList<>(unique);
    }

    private List<Long> filterValidTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<TagDO> existingTags = tagMapper.selectList(
                Wrappers.lambdaQuery(TagDO.class)
                        .select(TagDO::getId)
                        .eq(TagDO::getEnabled, 1)
                        .in(TagDO::getId, tagIds)
        );
        if (existingTags == null || existingTags.isEmpty()) {
            return List.of();
        }
        Set<Long> validTagIdSet = new HashSet<>();
        for (TagDO tag : existingTags) {
            if (tag.getId() != null) {
                validTagIdSet.add(tag.getId());
            }
        }
        List<Long> validTagIds = new ArrayList<>();
        for (Long tagId : tagIds) {
            if (validTagIdSet.contains(tagId)) {
                validTagIds.add(tagId);
            }
        }
        return validTagIds;
    }
}
