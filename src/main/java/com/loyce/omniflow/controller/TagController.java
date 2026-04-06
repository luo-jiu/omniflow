package com.loyce.omniflow.controller;

import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.dto.req.TagCreateReqDTO;
import com.loyce.omniflow.dto.req.TagUpdateReqDTO;
import com.loyce.omniflow.dto.resp.TagRespDTO;
import com.loyce.omniflow.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    /**
     * 获取搜索类型
     */
    @GetMapping("/search-types")
    public Result<String> getSearchTypes() {
        return Results.success(tagService.print());
    }

    /**
     * 查询标签（支持按 type 过滤）
     */
    @GetMapping
    public Result<List<TagRespDTO>> listTags(@RequestParam(required = false) String type) {
        return Results.success(tagService.listTags(type));
    }

    /**
     * 新建标签
     */
    @PostMapping
    public Result<TagRespDTO> createTag(@RequestBody TagCreateReqDTO requestParam) {
        return Results.success(tagService.createTag(requestParam));
    }

    /**
     * 修改标签
     */
    @PutMapping("/{tagId}")
    public Result<TagRespDTO> updateTag(@PathVariable Long tagId, @RequestBody TagUpdateReqDTO requestParam) {
        return Results.success(tagService.updateTag(tagId, requestParam));
    }

    /**
     * 删除标签（软删除）
     */
    @DeleteMapping("/{tagId}")
    public Result<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return Results.success();
    }
}
