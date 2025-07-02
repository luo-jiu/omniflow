package com.loyce.omniflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.common.convention.result.Result;
import com.loyce.omniflow.common.convention.result.Results;
import com.loyce.omniflow.common.enums.UserErrorCodeEnum;
import com.loyce.omniflow.dao.entity.LibraryDO;
import com.loyce.omniflow.dao.mapper.LibraryMapper;
import com.loyce.omniflow.dto.req.LibraryCreateReqDTO;
import com.loyce.omniflow.dto.req.LibraryUpdateReqDTO;
import com.loyce.omniflow.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryServiceImpl extends ServiceImpl<LibraryMapper, LibraryDO> implements LibraryService {

    @Override
    public Result<List<LibraryDO>> scrollLibrary(Long lastId, int size, String userId) {
        LambdaQueryWrapper<LibraryDO> queryWrapper = Wrappers.lambdaQuery(LibraryDO.class);
            // 如果 lastId 不为空，查询 ID 大于 lastId 的数据
            if (lastId != null) {
                queryWrapper.gt(LibraryDO::getId, lastId);
            }
            queryWrapper.eq(LibraryDO::getUserId, userId)
                    .eq(LibraryDO::getDelFlag, 0);
            queryWrapper.orderByAsc(LibraryDO::getId);  // 按 ID 升序排序
            queryWrapper.last("LIMIT " + size);  // 限制返回的数据量

            List<LibraryDO> users = baseMapper.selectList(queryWrapper);
            // 判断是否有更多数据：如果返回的数据量等于请求的大小，说明可能还有更多数据
            boolean hasMore = users.size() == size;
            return Results.success(users, hasMore? "true": "false");
        }

    @Override
    public void create(LibraryCreateReqDTO requestParam) {
        int inserted = baseMapper.insert(BeanUtil.toBean(requestParam, LibraryDO.class));
        if (inserted < 1) {
            // 新增失败
            throw new ClientException(UserErrorCodeEnum.USER_SAVE_ERROR);
        }
    }

    @Override
    public void update(LibraryUpdateReqDTO requestParam, String userId) {
        LambdaUpdateWrapper<LibraryDO> updateWrapper = Wrappers.lambdaUpdate(LibraryDO.class)
                .eq(LibraryDO::getId, requestParam.getId())
                .eq(LibraryDO::getUserId, userId);
        baseMapper.update(BeanUtil.toBean(requestParam, LibraryDO.class), updateWrapper);
    }

    @Override
    public void delete(Long id, String userId) {
        LambdaUpdateWrapper<LibraryDO> updateWrapper = Wrappers.lambdaUpdate(LibraryDO.class)
                .eq(LibraryDO::getId, id)
                .eq(LibraryDO::getUserId, userId)
                .eq(LibraryDO::getDelFlag, 1);
        baseMapper.update(updateWrapper);
    }

    @Override
    public boolean hasPermission(String userId, Long libraryId) {
        LibraryDO library = baseMapper.selectById(libraryId);
        if (library == null) {
            throw new ClientException("库不存在");
        }
        if (!library.getUserId().toString().equals(userId)) {
            throw new ClientException("非法访问：该库不属于当前用户");
        }
        return true;
    }
}
