package com.loyce.omniflow.service.helper;

import com.loyce.omniflow.common.convention.exception.ClientException;
import com.loyce.omniflow.dao.mapper.NodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NodeNameConflictChecker {

    private final NodeMapper nodeMapper;

    /**
     * 校验指定目录下是否有重名节点（排除自身）
     *
     * @param name      要校验的节点名
     * @param parentId  父节点 ID（目录）
     * @param libraryId 所属库 ID
     * @param excludeId 要排除的节点 ID（如重命名或移动时排除自己），可为 null
     */
    public void checkDuplicateName(String name, Integer parentId, Integer libraryId, Integer excludeId) {
        int count = nodeMapper.countByNameAndParent(name, parentId, libraryId, excludeId);
        if (count > 0) {
            throw new ClientException("同级目录下已存在同名节点：" + name);
        }
    }
}
