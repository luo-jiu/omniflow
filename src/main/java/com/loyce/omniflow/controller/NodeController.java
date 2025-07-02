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
public class NodeController {

    private final NodeService nodeService;

    /**
     * 创建节点
     */
    @LibraryPermission
    @PostMapping("/api/omniflow/v1/node/create")
    public Result<NodeDO> createNode(@RequestBody NodeCreateReqDTO requestParam) {
        return Results.success(nodeService.createNode(requestParam));
    }

    /**
     * 查询该节点的所有子节点(包括子文件夹和子文件)
     */
    @LibraryPermission
    @GetMapping("/api/omniflow/v1/{nodeId}/descendants")
    public Result<List<NodeRespDTO>> getAllDescendants(@PathVariable Integer nodeId, @RequestParam Integer libraryId) {
        return Results.success(nodeService.getAllDescendants(nodeId, libraryId));
    }

    /**
     * 查询该节点的直接子节点
     */
    @LibraryPermission
    @GetMapping("/api/omniflow/v1/{nodeId}/children")
    public Result<List<NodeRespDTO>> getDirectChildren(@PathVariable Integer nodeId, @RequestParam Integer libraryId) {
        return Results.success(nodeService.getDirectChildren(nodeId, libraryId));
    }

    /**
     * 查询该节点的祖先路径
     */
    @LibraryPermission
    @GetMapping("/api/omniflow/v1/{nodeId}/ancestors")
    public Result<List<NodePathRespDTO>> getAncestors(@PathVariable Integer nodeId, @RequestParam Integer libraryId) {
        return Results.success(nodeService.getAncestors(nodeId, libraryId));
    }

    /**
     * 查询该节点的完整路径
     */
    @LibraryPermission
    @GetMapping("/api/omniflow/v1/{nodeId}/path")
    public Result<String> getFullPath(@PathVariable Integer nodeId, @RequestParam Integer libraryId) {
        return Results.success(nodeService.getFullPath(nodeId, libraryId));
    }

    /**
     * 修改节点配置信息
     */
    @LibraryPermission
    @PostMapping("/api/omniflow/v1/update")
    public Result<Void> updateNode(@RequestBody NodeUpdateReqDTO requestParam) {
        nodeService.updateNode(requestParam);
        return Results.success();
    }

    /**
     * 重命名
     */
    @LibraryPermission
    @PostMapping("/api/omniflow/v1/rename")
    public Result<Void> rename(@RequestBody NodeRenameReqDTO requestParam) {
        nodeService.rename(requestParam);
        return Results.success();
    }

    /**
     * 移动节点(改变父节点)
     */
    @LibraryPermission
    @PostMapping("/api/omniflow/v1/move")
    public Result<Void> moveNode(@RequestBody NodeMoveReqDTO requestParam) {
        nodeService.moveNode(requestParam);
        return Results.success();
    }

    /**
     * 删除单个节点以及其后代
     */
    @LibraryPermission
    @DeleteMapping("/api/omniflow/v1/{ancestorId}/library/{libraryId}")
    public Result<Boolean> deleteNodeAndChildren(
            @PathVariable Long ancestorId,
            @PathVariable Long libraryId) {
        return Results.success(nodeService.deleteNodeAndChildren(ancestorId, libraryId));
    }
}
