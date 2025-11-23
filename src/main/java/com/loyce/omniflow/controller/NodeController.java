package com.loyce.omniflow.controller;

import com.loyce.omniflow.annotation.LibraryPermission;
import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.dao.entity.NodeDO;
import com.loyce.omniflow.dto.req.NodeCreateReqDTO;
import com.loyce.omniflow.dto.req.NodeMoveReqDTO;
import com.loyce.omniflow.dto.req.NodeRenameReqDTO;
import com.loyce.omniflow.dto.req.NodeUpdateReqDTO;
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
    @LibraryPermission
    @PostMapping()
    public Result<NodeDO> createNode(@RequestBody NodeCreateReqDTO requestParam) {
        return Results.success(nodeService.createNode(requestParam));
    }

    /**
     * 查询该节点的所有子节点(包括子文件夹和子文件)
     */
    @LibraryPermission
    @GetMapping("/{nodeId}/descendants")
    public Result<List<NodeRespDTO>> getAllDescendants(
            @PathVariable Long nodeId,
            @RequestParam Long libraryId) {
        return Results.success(nodeService.getAllDescendants(nodeId, libraryId));
    }

    /**
     * 查询该节点的直接子节点
     */
    @LibraryPermission
    @GetMapping("/{nodeId}/children")
    public Result<List<NodeRespDTO>> getDirectChildren(
            @PathVariable Long nodeId,
            @RequestParam Long libraryId) {
        return Results.success(nodeService.getDirectChildren(nodeId, libraryId));
    }

    /**
     * 查询该节点的祖先路径
     */
    @LibraryPermission
    @GetMapping("/{nodeId}/ancestors")
    public Result<List<NodePathRespDTO>> getAncestors(
            @PathVariable Long nodeId,
            @RequestParam Long libraryId) {
        return Results.success(nodeService.getAncestors(nodeId, libraryId));
    }

    /**
     * 查询该节点的完整路径
     */
    @LibraryPermission
    @GetMapping("/{nodeId}/path")
    public Result<String> getFullPath(@PathVariable Long nodeId, @RequestParam Long libraryId) {
        return Results.success(nodeService.getFullPath(nodeId, libraryId));
    }

    /**
     * 修改节点配置信息
     */
    @LibraryPermission
    @PutMapping("/{nodeId}")
    public Result<Void> updateNode(@PathVariable Long nodeId, @RequestBody NodeUpdateReqDTO requestParam) {
        nodeService.updateNode(nodeId, requestParam);
        return Results.success();
    }

    /**
     * 重命名节点
     */
    @LibraryPermission
    @PatchMapping("/{nodeId}/rename")
    public Result<Void> rename(@PathVariable Long nodeId, @RequestBody NodeRenameReqDTO requestParam) {
        nodeService.rename(nodeId, requestParam);
        return Results.success();
    }

    /**
     * 移动节点(改变顺序)
     */
    @LibraryPermission
    @PatchMapping("")
    public Result<Void> reorderNode() {
        nodeService.reorderNode();
        return Results.success();
    }

    /**
     * 移动节点(改变父节点)
     */
    @LibraryPermission
    @PatchMapping("/{nodeId}/move")
    public Result<Void> moveNode(@PathVariable Long nodeId, @RequestBody NodeMoveReqDTO requestParam) {
        nodeService.moveNode(nodeId, requestParam);
        return Results.success();
    }

    /**
     * 删除单个节点以及其后代
     */
    @LibraryPermission
    @DeleteMapping("/{ancestorId}/library/{libraryId}")
    public Result<Boolean> deleteNodeAndChildren(
            @PathVariable Long ancestorId,
            @PathVariable Long libraryId) {
        return Results.success(nodeService.deleteNodeAndChildren(ancestorId, libraryId));
    }
}
