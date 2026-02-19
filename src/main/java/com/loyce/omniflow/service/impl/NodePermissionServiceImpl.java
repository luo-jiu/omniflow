package com.loyce.omniflow.service.impl;

import com.loyce.omniflow.dao.entity.NodeDO;
import com.loyce.omniflow.dao.mapper.NodeMapper;
import com.loyce.omniflow.service.NodePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 节点权限辅助服务实现
 */
@Service("nodePermissionService")
@RequiredArgsConstructor
public class NodePermissionServiceImpl implements NodePermissionService {

    private final NodeMapper nodeMapper;

    @Override
    public Long getLibraryIdByNodeId(Long nodeId) {
        NodeDO nodeDO = nodeMapper.selectById(nodeId);
        return nodeDO != null ? nodeDO.getLibraryId() : null;
    }
}
