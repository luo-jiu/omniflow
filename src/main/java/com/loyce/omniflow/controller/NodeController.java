package com.loyce.omniflow.controller;

import com.loyce.omniflow.annotation.CheckLibraryPermission;
import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.dao.entity.NodeDO;
import com.loyce.omniflow.dto.req.NodeCreateReqDTO;
import com.loyce.omniflow.dto.req.NodeMoveReqDTO;
import com.loyce.omniflow.dto.req.NodeRenameReqDTO;
import com.loyce.omniflow.dto.req.NodeSearchReqDTO;
import com.loyce.omniflow.dto.req.NodeUpdateReqDTO;
import com.loyce.omniflow.dto.resp.NodeRecycleRespDTO;
import com.loyce.omniflow.dto.resp.NodePathRespDTO;
import com.loyce.omniflow.dto.resp.NodeRespDTO;
import com.loyce.omniflow.service.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nodes")
public class NodeController {

    private final NodeService nodeService;

    /**
     * 创建节点
     */
    @CheckLibraryPermission(libraryId = "#requestParam.libraryId")
    @PostMapping()
    public Result<NodeDO> createNode(@RequestBody NodeCreateReqDTO requestParam) {
        return Results.success(nodeService.createNode(requestParam));
    }

    /**
     * 查询节点详情
     */
    @CheckLibraryPermission(libraryId = "@nodePermissionService.getLibraryIdByNodeId(#nodeId)")
    @GetMapping("/{nodeId}")
    public Result<NodeRespDTO> getNodeDetail(@PathVariable Long nodeId) {
        return Results.success(nodeService.getNodeDetail(nodeId));
    }

    /**
     * 查询该节点的所有子节点(包括子文件夹和子文件)
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @GetMapping("/{nodeId}/descendants")
    public Result<List<NodeRespDTO>> getAllDescendants(
            @PathVariable Long nodeId,
            @RequestParam Long libraryId) {
        return Results.success(nodeService.getAllDescendants(nodeId, libraryId));
    }

    /**
     * 查询该节点的直接子节点
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @GetMapping("/{nodeId}/children")
    public Result<List<NodeRespDTO>> getDirectChildren(
            @PathVariable Long nodeId,
            @RequestParam Long libraryId) {
        return Results.success(nodeService.getDirectChildren(nodeId, libraryId));
    }

    /**
     * 获取库根节点ID（必要时自动修复树结构与闭包关系）
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @GetMapping("/library/{libraryId}/root")
    public Result<Long> getLibraryRootNodeId(@PathVariable Long libraryId) {
        return Results.success(nodeService.getLibraryRootNodeId(libraryId));
    }

    /**
     * 节点搜索（名称 + 标签组合）
     */
    @CheckLibraryPermission(libraryId = "#requestParam.libraryId")
    @PostMapping("/search")
    public Result<List<NodeRespDTO>> searchNodes(@RequestBody NodeSearchReqDTO requestParam) {
        return Results.success(nodeService.searchNodes(requestParam));
    }

    /**
     * 查询该节点的祖先路径
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @GetMapping("/{nodeId}/ancestors")
    public Result<List<NodePathRespDTO>> getAncestors(
            @PathVariable Long nodeId,
            @RequestParam Long libraryId) {
        return Results.success(nodeService.getAncestors(nodeId, libraryId));
    }

    /**
     * 查询该节点的完整路径
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @GetMapping("/{nodeId}/path")
    public Result<String> getFullPath(@PathVariable Long nodeId, @RequestParam Long libraryId) {
        return Results.success(nodeService.getFullPath(nodeId, libraryId));
    }

    /**
     * 修改节点配置信息
     */
    @CheckLibraryPermission(libraryId = "@nodePermissionService.getLibraryIdByNodeId(#nodeId)")
    @PutMapping("/{nodeId}")
    public Result<Void> updateNode(@PathVariable Long nodeId, @RequestBody NodeUpdateReqDTO requestParam) {
        nodeService.updateNode(nodeId, requestParam);
        return Results.success();
    }

    /**
     * 重命名节点
     */
    @CheckLibraryPermission(libraryId = "@nodePermissionService.getLibraryIdByNodeId(#nodeId)")
    @PatchMapping("/{nodeId}/rename")
    public Result<Void> rename(@PathVariable Long nodeId, @RequestBody NodeRenameReqDTO requestParam) {
        nodeService.rename(nodeId, requestParam);
        return Results.success();
    }

    /**
     * 移动节点(改变顺序)
     */
    @PatchMapping("")
    public Result<Void> reorderNode() {
        nodeService.reorderNode();
        return Results.success();
    }

    /**
     * 移动节点(改变父节点)
     */
    @CheckLibraryPermission(libraryId = "#requestParam.libraryId")
    @PatchMapping("/{nodeId}/move")
    public Result<Void> moveNode(@PathVariable Long nodeId, @RequestBody NodeMoveReqDTO requestParam) {
        nodeService.moveNode(nodeId, requestParam);
        return Results.success();
    }

    /**
     * 漫画目录按名称排序（直接子项）
     */
    @CheckLibraryPermission(libraryId = "@nodePermissionService.getLibraryIdByNodeId(#nodeId)")
    @PatchMapping("/{nodeId}/comic/sort-by-name")
    public Result<Void> sortComicChildrenByName(@PathVariable Long nodeId) {
        nodeService.sortComicChildrenByName(nodeId);
        return Results.success();
    }

    /**
     * 删除单个节点以及其后代
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @DeleteMapping("/{ancestorId}/library/{libraryId}")
    public Result<Boolean> deleteNodeAndChildren(
            @PathVariable Long ancestorId,
            @PathVariable Long libraryId) {
        return Results.success(nodeService.deleteNodeAndChildren(ancestorId, libraryId));
    }

    /**
     * 回收站列表（顶层条目）
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @GetMapping("/recycle/library/{libraryId}")
    public Result<List<NodeRecycleRespDTO>> getRecycleBinItems(@PathVariable Long libraryId) {
        return Results.success(nodeService.getRecycleBinItems(libraryId));
    }

    /**
     * 从回收站恢复节点及其后代
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @PatchMapping("/{ancestorId}/library/{libraryId}/restore")
    public Result<Boolean> restoreNodeAndChildren(
            @PathVariable Long ancestorId,
            @PathVariable Long libraryId) {
        return Results.success(nodeService.restoreNodeAndChildren(ancestorId, libraryId));
    }

    /**
     * 彻底删除回收站中的节点及其后代
     */
    @CheckLibraryPermission(libraryId = "#libraryId")
    @DeleteMapping("/{ancestorId}/library/{libraryId}/hard")
    public Result<Boolean> hardDeleteNodeAndChildren(
            @PathVariable Long ancestorId,
            @PathVariable Long libraryId) {
        return Results.success(nodeService.hardDeleteNodeAndChildren(ancestorId, libraryId));
    }
}
