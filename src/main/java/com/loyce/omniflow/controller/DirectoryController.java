package com.loyce.omniflow.controller;

import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.dto.resp.NodeRespDTO;
import com.loyce.omniflow.service.DirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.loyce.omniflow.common.convention.result.Result;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    /**
     * 上传文件并创建节点
     */
    @PostMapping("/upload")
    public Result<NodeRespDTO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("parent_id") Long parentId,
            @RequestParam("library_id") Long libraryId) {
        return Results.success(directoryService.uploadAndCreateNode(file, parentId, libraryId));
    }

    /**
     * 获取文件临时访问链接（通过节点 ID）
     */
    @GetMapping("/link")
    public Result<String> getFileLink(
            @RequestParam("node_id") Long nodeId,
            @RequestParam("library_id") Long libraryId,
            @RequestParam(value = "expiry", defaultValue = "60") Integer expiryMinutes
    ) {
        return Results.success(directoryService.getPresignedUrl(nodeId, libraryId, expiryMinutes));
    }

}
