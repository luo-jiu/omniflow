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
import com.loyce.omniflow.dto.resp.NodePathRespDTO;
import com.loyce.omniflow.dto.resp.NodeRespDTO;
import com.loyce.omniflow.event.NodeDeleteEvent;
import com.loyce.omniflow.service.NodeService;
import com.loyce.omniflow.service.helper.NodeNameConflictChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeServiceImpl extends ServiceImpl<NodeMapper, NodeDO> implements NodeService {

    private final NodeClosureMapper nodeClosureMapper;
    private final NodeNameConflictChecker checker;
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
        // 重命名时需判断是否有相同文件名
        checker.checkDuplicateName(req.getName(), nodeDO.getParentId(), nodeDO.getLibraryId(), nodeDO.getId());
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
            // 1. 先获取需要删除的 descendant ID 列表（包括自身）
            List<Long> descendantIds = nodeClosureMapper.selectDescendantIdsByAncestorAndLibrary(ancestorId, libraryId);
            if (CollectionUtils.isEmpty(descendantIds)) {
                return true;
            }

            // 2. 查询所有要删除的节点信息，用于构建文件路径
            List<NodeDO> nodesToDelete = baseMapper.selectByIdsAndLibraryId(descendantIds, libraryId);
            
            // 3. 筛选出文件节点（type=1），并构建文件路径列表
            List<String> filePaths = new ArrayList<>();
            for (NodeDO node : nodesToDelete) {
                // 只处理文件节点（type=1），文件夹不需要删除MinIO文件
                if (node.getType() != null && node.getType() == 1) {
                    try {
                        String fullPath = getStorageKey(node.getId());
                        filePaths.add(fullPath);
                    } catch (Exception e) {
                        // 如果获取路径失败，记录日志但不影响删除流程
                        // 可能节点已经被删除或路径不存在，继续处理其他节点
                    }
                }
            }

            // 4. 删除 node_closure 表中的相关记录
            nodeClosureMapper.deleteClosuresByAncestorAndLibrary(ancestorId, libraryId);

            // 5. 删除 nodes 表中的相关记录（使用逻辑删除）
            if (!descendantIds.isEmpty()) {
                this.removeByIds(descendantIds); // MyBatis-Plus 提供的批量删除方法（逻辑删除）
            }

            // 6. 发布删除事件，在事务提交后删除MinIO文件
            if (!filePaths.isEmpty()) {
                eventPublisher.publishEvent(new NodeDeleteEvent(this, filePaths, libraryId));
            }
            return true;
        } catch (Exception e) {
            throw new ClientException("删除节点及其子节点失败: " + e.getMessage());
        }
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
