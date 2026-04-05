package com.loyce.omniflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.dao.entity.NodeClosureDO;
import com.loyce.omniflow.dao.entity.NodeDO;
import com.loyce.omniflow.dao.mapper.NodeClosureMapper;
import com.loyce.omniflow.dao.mapper.NodeMapper;
import com.loyce.omniflow.dto.req.NodeCreateReqDTO;
import com.loyce.omniflow.dto.req.NodeMoveReqDTO;
import com.loyce.omniflow.dto.req.NodeRenameReqDTO;
import com.loyce.omniflow.dto.req.NodeUpdateReqDTO;
import com.loyce.omniflow.dto.resp.NodeRecycleRespDTO;
import com.loyce.omniflow.dto.resp.NodePathRespDTO;
import com.loyce.omniflow.dto.resp.NodeRespDTO;
import com.loyce.omniflow.event.NodeDeleteEvent;
import com.loyce.omniflow.service.NodeService;
import com.loyce.omniflow.service.helper.NodeNameConflictChecker;
import com.loyce.omniflow.service.helper.WindowsFileNameValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NodeServiceImpl extends ServiceImpl<NodeMapper, NodeDO> implements NodeService {

    private final NodeClosureMapper nodeClosureMapper;
    private final NodeNameConflictChecker checker;
    private final WindowsFileNameValidator windowsFileNameValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public NodeDO createNode(NodeCreateReqDTO req) {
        // 1. 判断是否有重名文件
        checker.checkDuplicateName(req.getName(), req.getParentId(), req.getLibraryId(), null);

        // 2. 构建 node节点并插入 node表
        NodeDO node = buildNodeDO(req);
        Integer sortOrder = baseMapper.getSortByLibraryIdAndParentId(req.getParentId(), req.getLibraryId());
        if (sortOrder == null) {
            sortOrder = 0;
        }
        node.setSortOrder(sortOrder + 15);
        baseMapper.insert(node);  // 插入后 node.id会自动设置为新值
        Long newNodeId = node.getId();

        // 3. 插入 node_closure表
        // 3.1 先插入自身节点信息
        NodeClosureDO selfClosure = new NodeClosureDO();
        selfClosure.setAncestor(newNodeId);
        selfClosure.setDescendant(newNodeId);
        selfClosure.setDepth(0);
        selfClosure.setLibraryId(req.getLibraryId());
        nodeClosureMapper.insert(selfClosure);

        // 3.2 如果有父节点，插入与其的关系
        if (req.getParentId() != null && req.getParentId() != 0) {
            // 查询父节点的所有祖先节点(包括父节点自生)
            List<NodeClosureDO> ancestorClosures = nodeClosureMapper.findAncestorsByParentId(req.getParentId(), req.getLibraryId());
            for (NodeClosureDO ancestorClosure  : ancestorClosures) {
                NodeClosureDO closure = new NodeClosureDO();
                int newDepth = ancestorClosure.getDepth() + 1;
                closure.setAncestor(ancestorClosure.getAncestor());
                closure.setDescendant(newNodeId);
                closure.setDepth(newDepth);  // 深度加1
                closure.setLibraryId(req.getLibraryId());
                nodeClosureMapper.insert(closure);
            }
        }
        return node;
    }

    private static NodeDO buildNodeDO(NodeCreateReqDTO req) {
        NodeDO node = new NodeDO();
        node.setName(req.getName());
        node.setParentId(req.getParentId() != null ? req.getParentId() : 0L);
        node.setType(req.getType());
        node.setLibraryId(req.getLibraryId());
        node.setBuiltInType("DEF");
        node.setArchiveMode(0);
        if (isFile(req.getType())) {
            node.setFileSize(req.getFileSize());
            node.setMimeType(req.getMimeType());
            node.setExt(req.getExt());
            node.setStorageKey(req.getStorageKey());
        }
        return node;
    }

    private static boolean isFile(Integer type) {
        return type != null && type == 1;
    }

    public List<NodeRespDTO> getAllDescendants(Long nodeId, Long libraryId) {
        List<NodeRespDTO> rawList = baseMapper.findAllDescendants(nodeId, libraryId);
        for (NodeRespDTO dto : rawList) {
            dto.setType(mapType(dto.getType()));
        }
        return rawList;
    }

    public List<NodeRespDTO> getDirectChildren(Long nodeId, Long libraryId) {
        List<NodeRespDTO> rawList = baseMapper.findDirectChildren(nodeId, libraryId);
        for (NodeRespDTO dto : rawList) {
            dto.setType(mapType(dto.getType()));
        }
        return rawList;
    }

    public List<NodePathRespDTO> getAncestors(Long nodeId, Long libraryId) {
        return baseMapper.findAncestors(nodeId, libraryId);
    }

    public String getStorageKey(Long nodeId) {
        NodeDO node = baseMapper.selectById(nodeId);
        if (node == null) {
            throw new ClientException("Node not found or does not belong to the specified library.");
        }
        return node.getStorageKey();
    }

    public String getFullPath(Long nodeId, Long libraryId) {
        List<NodePathRespDTO> pathNodes = baseMapper.findFullPath(nodeId, libraryId);
        if (pathNodes.isEmpty()) {
            throw new ClientException("Node not found or does not belong to the specified library.");
        }

        // 拼接路径
        StringBuilder path = new StringBuilder();
        for (NodePathRespDTO node : pathNodes) {
            path.append("/").append(node.getName());
        }
        return path.toString();
    }

    @Override
    public void updateNode(Long nodeId, NodeUpdateReqDTO requestParam) {
        requestParam.setId(nodeId);
        baseMapper.updateNode(requestParam);
    }

    @Override
    public void rename(Long nodeId, NodeRenameReqDTO req) {
        NodeDO nodeDO = baseMapper.selectById(nodeId);
        if (nodeDO == null) {
            throw new ClientException("Node not found with ID: " + nodeId);
        }

        String normalizedName = windowsFileNameValidator.normalizeName(req.getName());
        String normalizedExt = "";
        if (isFile(nodeDO.getType())) {
            normalizedExt = windowsFileNameValidator.normalizeExt(req.getExt());
        }
        windowsFileNameValidator.validate(normalizedName, normalizedExt);

        // 重命名时需判断是否有相同文件名
        checker.checkDuplicateName(normalizedName, nodeDO.getParentId(), nodeDO.getLibraryId(), nodeDO.getId());
        req.setId(nodeId);
        req.setName(normalizedName);
        req.setExt(normalizedExt);
        baseMapper.rename(req);
    }

    @Override
    public void reorderNode() {

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNode(Long nodeId, NodeMoveReqDTO req) {
        Long newParentId = req.getNewParentId();
        Long libraryId = req.getLibraryId();
        Long beforeNodeId = req.getBeforeNodeId();
        // 判断移动目标下是否有重名文件
        checker.checkDuplicateName(req.getName(), newParentId, libraryId, nodeId);

        // 1. 校验节点是否存在
        NodeDO node = baseMapper.selectByIdAndLibraryId(nodeId, libraryId);
        if (node == null) {
            throw new ClientException("Node not found with ID: " + nodeId);
        }

        // 2. 校验新父节点是否存在
        if (newParentId != 0) { // 0 表示根节点
            NodeDO newParent = baseMapper.selectByIdAndLibraryId(newParentId, libraryId);
            if (newParent == null) {
                throw new ClientException("New parent node not found with ID: " + newParentId);
            }
        }

        // 3. 防止将节点移动到自身或其后代下（避免循环）
        if (isDescendant(nodeId, newParentId, libraryId)) {
            throw new ClientException("Cannot move node to its descendant");
        }

        // 4. 计算新的sort_order
        Integer newOrder;
        if (beforeNodeId!= null) {
            NodeDO beforeNode = baseMapper.selectByIdAndLibraryId(beforeNodeId, libraryId);
            if (beforeNode == null) {
                throw new ClientException("Before-node not found: " + beforeNodeId);
            }
            // 在 beforeNode前插入，所有 >= beforeNode.sort_order的节点整体后移
            baseMapper.incrementSortOrderAfter(newParentId, libraryId, beforeNode.getSortOrder());
            newOrder = beforeNode.getSortOrder();
        } else {
            // 没指定目标，放到最后
            newOrder = baseMapper.getSortByLibraryIdAndParentId(newParentId, libraryId) + 15;
        }

        // 5. 更新 nodes 表中的 parent_id
        // baseMapper.updateParentAndSort(nodeId, targetParentId, newOrder, libraryId);
        baseMapper.updateParentId(nodeId, newParentId, newOrder, libraryId);

        // 6. 删除旧关系
        nodeClosureMapper.deleteOldRelations(nodeId, libraryId);

        // 7. 插入新关系
        nodeClosureMapper.insertNewRelations(nodeId, newParentId, libraryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNodeAndChildren(Long ancestorId, Long libraryId) {
        try {
            // 软删除：仅标记 deleted_at，保留 node_closure 和 MinIO 文件用于回收站恢复
            List<Long> descendantIds = nodeClosureMapper.selectDescendantIdsByAncestorAndLibrary(ancestorId, libraryId);
            if (CollectionUtils.isEmpty(descendantIds)) {
                return true;
            }
            this.removeByIds(descendantIds);
            return true;
        } catch (Exception e) {
            throw new ClientException("删除节点及其子节点失败: " + e.getMessage());
        }
    }

    @Override
    public List<NodeRecycleRespDTO> getRecycleBinItems(Long libraryId) {
        List<NodeRecycleRespDTO> deletedNodes = baseMapper.selectDeletedByLibraryId(libraryId);
        if (CollectionUtils.isEmpty(deletedNodes)) {
            return new ArrayList<>();
        }

        Set<Long> deletedNodeIds = new HashSet<>();
        for (NodeRecycleRespDTO item : deletedNodes) {
            deletedNodeIds.add(item.getId());
            item.setType(mapType(item.getType()));
        }

        List<NodeRecycleRespDTO> topLevelItems = new ArrayList<>();
        for (NodeRecycleRespDTO item : deletedNodes) {
            Long parentId = item.getParentId();
            if (parentId == null || parentId <= 0 || !deletedNodeIds.contains(parentId)) {
                topLevelItems.add(item);
            }
        }

        topLevelItems.sort(Comparator.comparing(NodeRecycleRespDTO::getDeletedAt).reversed()
                .thenComparing(NodeRecycleRespDTO::getId, Comparator.reverseOrder()));
        return topLevelItems;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreNodeAndChildren(Long ancestorId, Long libraryId) {
        NodeDO targetNode = baseMapper.selectByIdAndLibraryIdIncludeDeleted(ancestorId, libraryId);
        if (targetNode == null) {
            throw new ClientException("回收站节点不存在");
        }
        if (targetNode.getDeletedAt() == null) {
            return true;
        }

        List<Long> descendantIds = nodeClosureMapper.selectDescendantIdsByAncestorAndLibrary(ancestorId, libraryId);
        if (CollectionUtils.isEmpty(descendantIds)) {
            throw new ClientException("该删除记录无法恢复，可能是历史彻底删除数据");
        }

        if (targetNode.getParentId() != null && targetNode.getParentId() > 0) {
            NodeDO parentNode = baseMapper.selectByIdAndLibraryIdIncludeDeleted(targetNode.getParentId(), libraryId);
            if (parentNode == null) {
                throw new ClientException("无法恢复：父目录不存在");
            }
            if (parentNode.getDeletedAt() != null && !descendantIds.contains(parentNode.getId())) {
                throw new ClientException("无法恢复：父目录仍在回收站中");
            }
        }

        List<NodeDO> nodesToRestore = baseMapper.selectByIdsAndLibraryId(descendantIds, libraryId);
        Set<Long> restoringNodeIds = new HashSet<>(descendantIds);
        for (NodeDO node : nodesToRestore) {
            if (node.getDeletedAt() == null) {
                continue;
            }
            Long parentId = node.getParentId();
            if (parentId != null && restoringNodeIds.contains(parentId)) {
                // 父节点也在同一批恢复内，重名校验由父节点外层边界统一处理即可
                continue;
            }
            checker.checkDuplicateName(node.getName(), parentId, libraryId, node.getId());
        }

        baseMapper.restoreByIds(descendantIds, libraryId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean hardDeleteNodeAndChildren(Long ancestorId, Long libraryId) {
        NodeDO targetNode = baseMapper.selectByIdAndLibraryIdIncludeDeleted(ancestorId, libraryId);
        if (targetNode == null) {
            return true;
        }
        if (targetNode.getDeletedAt() == null) {
            throw new ClientException("仅支持彻底删除回收站中的节点");
        }

        List<Long> descendantIds = nodeClosureMapper.selectDescendantIdsByAncestorAndLibrary(ancestorId, libraryId);
        if (CollectionUtils.isEmpty(descendantIds)) {
            return true;
        }

        List<NodeDO> nodesToDelete = baseMapper.selectByIdsAndLibraryId(descendantIds, libraryId);
        List<String> filePaths = new ArrayList<>();
        for (NodeDO node : nodesToDelete) {
            if (node.getType() != null && node.getType() == 1 && node.getStorageKey() != null && !node.getStorageKey().isBlank()) {
                filePaths.add(node.getStorageKey());
            }
        }

        nodeClosureMapper.deleteClosuresByAncestorAndLibrary(ancestorId, libraryId);
        baseMapper.hardDeleteByIds(descendantIds, libraryId);

        if (!filePaths.isEmpty()) {
            eventPublisher.publishEvent(new NodeDeleteEvent(this, filePaths, libraryId));
        }
        return true;
    }

    // 辅助方法：检查 targetId 是否是 nodeId 的后代
    private boolean isDescendant(Long nodeId, Long targetId, Long libraryId) {
        return nodeClosureMapper.existsDescendant(nodeId, targetId, libraryId) > 0;
    }

    // 类型转换
    private String mapType(String code) {
        if ("0".equals(code)) {
            return "dir";
        } else if ("1".equals(code)) {
            return "file";
        } else {
            return "unknown";
        }
    }
}
