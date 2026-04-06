package com.loyce.omniflow.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
import com.loyce.omniflow.dto.req.NodeSearchReqDTO;
import com.loyce.omniflow.dto.req.NodeUpdateReqDTO;
import com.loyce.omniflow.dto.resp.NodeRecycleRespDTO;
import com.loyce.omniflow.dto.resp.NodePathRespDTO;
import com.loyce.omniflow.dto.resp.NodeRespDTO;
import com.loyce.omniflow.event.NodeDeleteEvent;
import com.loyce.omniflow.service.NodeTagRelService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NodeServiceImpl extends ServiceImpl<NodeMapper, NodeDO> implements NodeService {

    private static final int SORT_STEP = 1024;
    private static final int MIN_SORT_GAP = 2;
    private static final int MAX_SAFE_SORT_ORDER = Integer.MAX_VALUE - SORT_STEP * 4;

    private final NodeClosureMapper nodeClosureMapper;
    private final NodeNameConflictChecker checker;
    private final WindowsFileNameValidator windowsFileNameValidator;
    private final NodeTagRelService nodeTagRelService;
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

    @Override
    public List<NodeRespDTO> searchNodes(NodeSearchReqDTO requestParam) {
        if (requestParam == null) {
            throw new ClientException("search request cannot be null");
        }
        Long libraryId = requestParam.getLibraryId();
        if (libraryId == null || libraryId <= 0) {
            throw new ClientException("libraryId is invalid");
        }

        String keyword = requestParam.getKeyword() == null ? "" : requestParam.getKeyword().trim();
        if (keyword.isEmpty()) {
            keyword = null;
        }

        List<Long> tagIds = normalizePositiveLongList(requestParam.getTagIds());
        String tagMatchMode = normalizeTagMatchMode(requestParam.getTagMatchMode());
        int limit = normalizeSearchLimit(requestParam.getLimit());
        int tagCount = tagIds.size();

        List<NodeRespDTO> rawList = baseMapper.searchNodes(
                libraryId,
                keyword,
                tagIds,
                tagMatchMode,
                tagCount,
                limit
        );
        for (NodeRespDTO dto : rawList) {
            dto.setType(mapType(dto.getType()));
        }
        return rawList;
    }

    public List<NodePathRespDTO> getAncestors(Long nodeId, Long libraryId) {
        return baseMapper.findAncestors(nodeId, libraryId);
    }

    @Override
    public NodeRespDTO getNodeDetail(Long nodeId) {
        NodeRespDTO detail = baseMapper.selectNodeRespById(nodeId);
        if (detail == null) {
            throw new ClientException("Node not found with ID: " + nodeId);
        }
        detail.setType(mapType(detail.getType()));
        return detail;
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
    @Transactional(rollbackFor = Exception.class)
    public void updateNode(Long nodeId, NodeUpdateReqDTO requestParam) {
        NodeDO node = baseMapper.selectById(nodeId);
        if (node == null) {
            throw new ClientException("Node not found with ID: " + nodeId);
        }

        String builtInType = requestParam.getBuiltInType();
        if (builtInType == null || builtInType.trim().isEmpty()) {
            builtInType = node.getBuiltInType() == null || node.getBuiltInType().isBlank()
                    ? "DEF"
                    : node.getBuiltInType();
        } else {
            builtInType = builtInType.trim().toUpperCase();
        }

        Integer archiveMode = requestParam.getArchiveMode();
        if (archiveMode == null) {
            archiveMode = node.getArchiveMode() == null ? 0 : node.getArchiveMode();
        }
        if (archiveMode != 0 && archiveMode != 1) {
            throw new ClientException("archiveMode only supports 0 or 1");
        }

        String viewMeta = requestParam.getViewMeta();
        if (viewMeta == null) {
            viewMeta = node.getViewMeta();
        } else {
            viewMeta = viewMeta.trim();
            if (viewMeta.isEmpty()) {
                viewMeta = null;
            }
        }

        List<Long> tagIds = extractTagIdsFromViewMeta(viewMeta);

        requestParam.setId(nodeId);
        requestParam.setBuiltInType(builtInType);
        requestParam.setArchiveMode(archiveMode);
        requestParam.setViewMeta(viewMeta);
        baseMapper.updateNode(requestParam);

        nodeTagRelService.replaceNodeTags(nodeId, node.getLibraryId(), tagIds);
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
        Long newParentId = normalizeParentId(req.getNewParentId());
        Long libraryId = req.getLibraryId();
        Long beforeNodeId = req.getBeforeNodeId();

        // 1. 校验节点是否存在
        NodeDO node = baseMapper.selectByIdAndLibraryId(nodeId, libraryId);
        if (node == null) {
            throw new ClientException("Node not found with ID: " + nodeId);
        }
        Long oldParentId = normalizeParentId(node.getParentId());

        if (nodeId.equals(newParentId)) {
            throw new ClientException("Cannot move node under itself");
        }
        if (newParentId <= 0) {
            throw new ClientException("Target parent is invalid: " + newParentId);
        }

        // 2. 并发保护：按固定顺序锁定受影响父目录与其直接子节点
        lockMoveScope(libraryId, oldParentId, newParentId);
        validateTargetParentAsDirectory(newParentId, libraryId);

        // 3. 判断移动目标下是否有重名文件
        checker.checkDuplicateName(node.getName(), newParentId, libraryId, nodeId);

        // 4. 防止将节点移动到自身或其后代下（避免循环）
        if (isDescendant(nodeId, newParentId, libraryId)) {
            throw new ClientException("Cannot move node to its descendant");
        }

        // 5. 计算新的 sort_order（优先使用间隔插入，空间不足时仅重排目标父目录）
        Integer newOrder = null;
        if (beforeNodeId != null) {
            NodeDO beforeNode = baseMapper.selectByIdAndLibraryId(beforeNodeId, libraryId);
            if (beforeNode == null) {
                throw new ClientException("Before-node not found: " + beforeNodeId);
            }
            if (beforeNode.getId().equals(nodeId)) {
                return;
            }
            if (!normalizeParentId(beforeNode.getParentId()).equals(newParentId)) {
                throw new ClientException("Before-node must belong to target parent");
            }
            newOrder = computeOrderBefore(newParentId, libraryId, beforeNode);
        } else {
            newOrder = computeOrderAtEnd(newParentId, libraryId);
        }

        // 6. 更新 nodes 表中的 parent_id 与 sort_order
        baseMapper.updateParentId(nodeId, newParentId, newOrder, libraryId);

        // 7. 父目录变化时，更新 closure 关系；同父排序无需改 closure
        if (!oldParentId.equals(newParentId)) {
            nodeClosureMapper.deleteOldRelations(nodeId, libraryId);
            nodeClosureMapper.insertNewRelations(nodeId, newParentId, libraryId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortComicChildrenByName(Long nodeId) {
        NodeDO parentNode = baseMapper.selectById(nodeId);
        if (parentNode == null || parentNode.getDeletedAt() != null) {
            throw new ClientException("Node not found with ID: " + nodeId);
        }
        if (parentNode.getType() == null || parentNode.getType() != 0) {
            throw new ClientException("Target node must be a directory");
        }

        String builtInType = parentNode.getBuiltInType() == null
                ? "DEF"
                : parentNode.getBuiltInType().trim().toUpperCase();
        if (!"COMIC".equals(builtInType)) {
            throw new ClientException("Only COMIC directories support name sorting");
        }

        Long libraryId = parentNode.getLibraryId();
        if (libraryId == null) {
            throw new ClientException("Target node library is invalid");
        }

        lockParentScope(libraryId, nodeId);
        List<NodeDO> children = baseMapper.selectActiveChildrenForSortByName(nodeId, libraryId);
        if (CollectionUtils.isEmpty(children)) {
            return;
        }

        children.sort(NodeServiceImpl::compareChildrenByNaturalName);
        int order = SORT_STEP;
        for (NodeDO child : children) {
            baseMapper.updateSortOrder(child.getId(), libraryId, order);
            if (order > Integer.MAX_VALUE - SORT_STEP) {
                throw new ClientException("sort_order range exhausted under parent: " + nodeId);
            }
            order += SORT_STEP;
        }
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
        nodeTagRelService.deleteByNodeIds(descendantIds);
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

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private void lockMoveScope(Long libraryId, Long oldParentId, Long newParentId) {
        Long first = oldParentId <= newParentId ? oldParentId : newParentId;
        Long second = oldParentId <= newParentId ? newParentId : oldParentId;

        lockParentScope(libraryId, first);
        if (!second.equals(first)) {
            lockParentScope(libraryId, second);
        }
    }

    private void lockParentScope(Long libraryId, Long parentId) {
        if (parentId > 0) {
            Long locked = baseMapper.lockNode(parentId, libraryId);
            if (locked == null) {
                throw new ClientException("Target parent node not found with ID: " + parentId);
            }
        }
        baseMapper.lockActiveChildrenByParent(parentId, libraryId);
    }

    private void validateTargetParentAsDirectory(Long parentId, Long libraryId) {
        NodeDO parentNode = baseMapper.selectByIdAndLibraryId(parentId, libraryId);
        if (parentNode == null) {
            throw new ClientException("Target parent node not found with ID: " + parentId);
        }
        if (parentNode.getType() == null || parentNode.getType() != 0) {
            throw new ClientException("Target parent must be a directory");
        }
    }

    private Integer computeOrderBefore(Long parentId, Long libraryId, NodeDO beforeNode) {
        Integer prev = baseMapper.getPrevSortOrderBeforeNode(
                parentId,
                libraryId,
                beforeNode.getSortOrder(),
                beforeNode.getId()
        );
        int left = prev == null ? 0 : prev;
        int right = beforeNode.getSortOrder();

        if (right - left <= MIN_SORT_GAP) {
            reindexParent(parentId, libraryId);
            NodeDO refreshedBefore = baseMapper.selectByIdAndLibraryId(beforeNode.getId(), libraryId);
            if (refreshedBefore == null) {
                throw new ClientException("Before-node not found after reindex: " + beforeNode.getId());
            }
            prev = baseMapper.getPrevSortOrderBeforeNode(
                    parentId,
                    libraryId,
                    refreshedBefore.getSortOrder(),
                    refreshedBefore.getId()
            );
            left = prev == null ? 0 : prev;
            right = refreshedBefore.getSortOrder();
        }

        if (right - left <= 1) {
            // 极端冲突兜底：保持稳定插入语义
            baseMapper.incrementSortOrderAfter(parentId, libraryId, right);
            return right;
        }
        return left + ((right - left) / 2);
    }

    private Integer computeOrderAtEnd(Long parentId, Long libraryId) {
        Integer maxOrder = baseMapper.getSortByLibraryIdAndParentId(parentId, libraryId);
        if (maxOrder == null) {
            return SORT_STEP;
        }
        if (maxOrder >= MAX_SAFE_SORT_ORDER) {
            reindexParent(parentId, libraryId);
            maxOrder = baseMapper.getSortByLibraryIdAndParentId(parentId, libraryId);
        }
        int safeBase = maxOrder == null ? 0 : maxOrder;
        return safeBase + SORT_STEP;
    }

    /**
     * 仅重排某个父目录的第一层子节点，恢复间隔号空间。
     */
    private void reindexParent(Long parentId, Long libraryId) {
        List<NodeDO> siblings = baseMapper.selectActiveChildrenForReindex(parentId, libraryId);
        if (CollectionUtils.isEmpty(siblings)) {
            return;
        }
        int order = SORT_STEP;
        for (NodeDO sibling : siblings) {
            baseMapper.updateSortOrder(sibling.getId(), libraryId, order);
            if (order > Integer.MAX_VALUE - SORT_STEP) {
                throw new ClientException("sort_order range exhausted under parent: " + parentId);
            }
            order += SORT_STEP;
        }
    }

    private static int compareChildrenByNaturalName(NodeDO left, NodeDO right) {
        String leftName = buildSortableName(left);
        String rightName = buildSortableName(right);
        int cmp = naturalCompare(leftName, rightName);
        if (cmp != 0) {
            return cmp;
        }
        return Long.compare(left.getId(), right.getId());
    }

    private static String buildSortableName(NodeDO node) {
        String baseName = node.getName() == null ? "" : node.getName().trim();
        if (!isFile(node.getType())) {
            return baseName;
        }
        String ext = node.getExt();
        if (ext == null || ext.isBlank()) {
            return baseName;
        }
        return baseName + "." + ext.trim().toLowerCase();
    }

    private static int naturalCompare(String left, String right) {
        int li = 0;
        int ri = 0;
        final int leftLength = left.length();
        final int rightLength = right.length();

        while (li < leftLength && ri < rightLength) {
            char leftChar = left.charAt(li);
            char rightChar = right.charAt(ri);
            boolean leftIsDigit = Character.isDigit(leftChar);
            boolean rightIsDigit = Character.isDigit(rightChar);

            if (leftIsDigit && rightIsDigit) {
                int leftNumStart = li;
                int rightNumStart = ri;
                while (li < leftLength && Character.isDigit(left.charAt(li))) {
                    li++;
                }
                while (ri < rightLength && Character.isDigit(right.charAt(ri))) {
                    ri++;
                }

                int leftNoZero = leftNumStart;
                while (leftNoZero < li && left.charAt(leftNoZero) == '0') {
                    leftNoZero++;
                }
                int rightNoZero = rightNumStart;
                while (rightNoZero < ri && right.charAt(rightNoZero) == '0') {
                    rightNoZero++;
                }

                int leftNumericLength = li - leftNoZero;
                int rightNumericLength = ri - rightNoZero;
                if (leftNumericLength != rightNumericLength) {
                    return Integer.compare(leftNumericLength, rightNumericLength);
                }

                for (int i = 0; i < leftNumericLength; i++) {
                    int diff = left.charAt(leftNoZero + i) - right.charAt(rightNoZero + i);
                    if (diff != 0) {
                        return diff;
                    }
                }

                int leftRawLength = li - leftNumStart;
                int rightRawLength = ri - rightNumStart;
                if (leftRawLength != rightRawLength) {
                    return Integer.compare(leftRawLength, rightRawLength);
                }
                continue;
            }

            char leftLower = Character.toLowerCase(leftChar);
            char rightLower = Character.toLowerCase(rightChar);
            if (leftLower != rightLower) {
                return Character.compare(leftLower, rightLower);
            }
            li++;
            ri++;
        }
        return Integer.compare(leftLength, rightLength);
    }

    private String normalizeTagMatchMode(String rawMode) {
        String mode = rawMode == null ? "ANY" : rawMode.trim().toUpperCase();
        return "ALL".equals(mode) ? "ALL" : "ANY";
    }

    private int normalizeSearchLimit(Integer rawLimit) {
        if (rawLimit == null) {
            return 200;
        }
        if (rawLimit <= 0) {
            return 200;
        }
        return Math.min(rawLimit, 500);
    }

    private List<Long> normalizePositiveLongList(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null && value > 0) {
                unique.add(value);
            }
        }
        return new ArrayList<>(unique);
    }

    private List<Long> extractTagIdsFromViewMeta(String viewMeta) {
        if (viewMeta == null || viewMeta.isBlank()) {
            return List.of();
        }
        JSONObject meta;
        try {
            meta = JSON.parseObject(viewMeta);
        } catch (Exception ex) {
            throw new ClientException("viewMeta JSON 格式非法");
        }
        if (meta == null) {
            return List.of();
        }
        JSONArray tagIds = meta.getJSONArray("tagIds");
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Object item : tagIds) {
            Long tagId = parsePositiveLong(item);
            if (tagId != null) {
                unique.add(tagId);
            }
        }
        return new ArrayList<>(unique);
    }

    private Long parsePositiveLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(String.valueOf(value));
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
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
