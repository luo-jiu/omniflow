package com.loyce.omniflow.service;

/**
 * 节点权限辅助服务
 */
public interface NodePermissionService {

    /**
     * 根据节点ID获取所属库ID
     * @param nodeId 节点ID
     * @return 库ID
     */
    Long getLibraryIdByNodeId(Long nodeId);
}
