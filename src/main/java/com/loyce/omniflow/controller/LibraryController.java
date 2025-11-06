package com.loyce.omniflow.controller;

import com.loyce.omniflow.common.biz.user.UserContext;
import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.dao.entity.LibraryDO;
import com.loyce.omniflow.dto.req.LibraryCreateReqDTO;
import com.loyce.omniflow.dto.req.LibraryUpdateReqDTO;
import com.loyce.omniflow.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/libraries")
public class LibraryController {

    private final LibraryService libraryService;

    /**
     * 分页查询仓库（基于游标）
     */
    @GetMapping("/scroll")
    public Result<List<LibraryDO>> scrollLibrary(
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size) {
        return libraryService.scrollLibrary(lastId, size, UserContext.getUserId());
    }

    /**
     * 新建仓库
     */
    @PostMapping()
    public Result<Void> create(@RequestBody LibraryCreateReqDTO requestParam) {
        libraryService.create(requestParam);
        return Results.success();
    }

    /**
     * 修改仓库
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody LibraryUpdateReqDTO requestParam) {
        libraryService.update(id, requestParam, UserContext.getUserId());
        return Results.success();
    }

    /**
     * 删除仓库
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        libraryService.delete(id, UserContext.getUserId());
        return Results.success();
    }

    // TODO 获取单个仓库详细信息 详细信息存放在其他表中
}
