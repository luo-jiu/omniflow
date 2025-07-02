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
import com.loyce.omniflow.service.NodeService;
import com.loyce.omniflow.service.helper.NodeNameConflictChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeServiceImpl extends ServiceImpl<NodeMapper, NodeDO> implements NodeService {

    private final NodeClosureMapper nodeClosureMapper;
    private final NodeNameConflictChecker checker;

    @Override
    @Transactional
    public NodeDO createNode(NodeCreateReqDTO req) {
        // 1. 判断是否有重名文件
        checker.checkDuplicateName(req.getName(), req.getParentId(), req.getLibraryId(), null);

        // 2. 构建 node节点并插入 node表
        NodeDO node = getNodeDO(req);
        baseMapper.insert(node);  // 插入后 node.id会自动设置为新值
        Integer newNodeId = node.getId();

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

    private static NodeDO getNodeDO(NodeCreateReqDTO requestParam) {
        NodeDO node = new NodeDO();
        node.setName(requestParam.getName());
        node.setParentId(requestParam.getParentId());
        node.setType(requestParam.getType());
        node.setBuiltInType("DEF");  // 默认类型
        if (requestParam.getType() != null && requestParam.getType() == 1) {  // 不为空且为file, 需要设置其他参数
            node.setFileSize(requestParam.getFileSize());
            node.setMineType(requestParam.getMineType());
        }
        node.setArchiveMode(0);  // 归档模式 0=关闭 1=开启
        node.setLibraryId(requestParam.getLibraryId());
        return node;
    }

    public List<NodeRespDTO> getAllDescendants(Integer nodeId, Integer libraryId) {
        List<NodeRespDTO> rawList = baseMapper.findAllDescendants(nodeId, libraryId);
        for (NodeRespDTO dto : rawList) {
            dto.setType(mapType(dto.getType()));
        }
        return rawList;
    }

    public List<NodeRespDTO> getDirectChildren(Integer nodeId, Integer libraryId) {
        List<NodeRespDTO> rawList = baseMapper.findDirectChildren(nodeId, libraryId);
        for (NodeRespDTO dto : rawList) {
            dto.setType(mapType(dto.getType()));
        }
        return rawList;
    }

    public List<NodePathRespDTO> getAncestors(Integer nodeId, Integer libraryId) {
        return baseMapper.findAncestors(nodeId, libraryId);
    }

    public String getFullPath(Integer nodeId, Integer libraryId) {
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
    public void updateNode(NodeUpdateReqDTO requestParam) {
        baseMapper.updateNode(requestParam);
    }

    @Override
    public void rename(NodeRenameReqDTO req) {
        // 重命名时需判断是否有相同文件名
        checker.checkDuplicateName(req.getName(), req.getNewParentId(), req.getLibraryId(), req.getId());
        baseMapper.rename(req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveNode(NodeMoveReqDTO req) {
        Integer nodeId = req.getNodeId();
        Integer newParentId = req.getNewParentId();
        Integer libraryId = req.getLibraryId();
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

        // 4. 更新 nodes 表中的 parent_id
        baseMapper.updateParentId(nodeId, newParentId, libraryId);

        // 5. 删除旧关系
        nodeClosureMapper.deleteOldRelations(nodeId, libraryId);

        // 6. 插入新关系
        nodeClosureMapper.insertNewRelations(nodeId, newParentId, libraryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNodeAndChildren(Long ancestorId, Long libraryId) {
        try {
            // 1. 先获取需要删除的 descendant ID 列表
            List<Long> descendantIds = nodeClosureMapper.selectDescendantIdsByAncestorAndLibrary(ancestorId, libraryId);
            if (CollectionUtils.isEmpty(descendantIds)) {
                return true;
            }

            // 2. 删除 node_closure 表中的相关记录
            nodeClosureMapper.deleteClosuresByAncestorAndLibrary(ancestorId, libraryId);

            // 3. 删除 nodes 表中的相关记录
            // 由于已经有了 descendantIds 列表，可以直接删除，而不需要再次查询 node_closure
            if (!descendantIds.isEmpty()) {
                baseMapper.deleteBatchIds(descendantIds); // MyBatis-Plus 提供的批量删除方法
            }

            return true;
        } catch (Exception e) {
            throw new ClientException("删除节点及其子节点失败: " + e.getMessage());
        }
    }

    // 辅助方法：检查 targetId 是否是 nodeId 的后代
    private boolean isDescendant(Integer nodeId, Integer targetId, Integer libraryId) {
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
