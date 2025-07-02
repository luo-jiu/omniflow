package com.loyce.omniflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.dao.entity.LibraryDO;
import com.loyce.omniflow.dto.req.LibraryCreateReqDTO;
import com.loyce.omniflow.dto.req.LibraryUpdateReqDTO;

import java.util.List;

public interface LibraryService extends IService<LibraryDO> {

    /**
     * 分页查询仓库（基于游标）
     *
     * @param lastId 上一次加载的最后一条数据id
     * @param size 读取数量
     */
    Result<List<LibraryDO>> scrollLibrary(Long lastId, int size, String userId);

    /**
     * 创建仓库
     *
     * @param requestParam
     */
    void create(LibraryCreateReqDTO requestParam);

    /**
     * 修改仓库
     *
     * @param requestParam
     */
    void update(LibraryUpdateReqDTO requestParam, String userId);

    /**
     * 删除仓库 (逻辑删除)
     *
     * @param id
     */
    void delete(Long id, String userId);

    /**
     * 判断库是否属于该用户
     *
     * @param userId
     * @param libraryId
     * @return
     */
    boolean hasPermission(String userId, Long libraryId);
}
