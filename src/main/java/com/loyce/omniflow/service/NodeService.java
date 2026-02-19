package com.loyce.omniflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.loyce.omniflow.dao.entity.NodeDO;
import com.loyce.omniflow.dto.req.NodeCreateReqDTO;
import com.loyce.omniflow.dto.req.NodeMoveReqDTO;
import com.loyce.omniflow.dto.req.NodeRenameReqDTO;
import com.loyce.omniflow.dto.req.NodeUpdateReqDTO;
import com.loyce.omniflow.dto.resp.NodePathRespDTO;
import com.loyce.omniflow.dto.resp.NodeRespDTO;

import java.util.List;

public interface NodeService extends IService<NodeDO> {

    /**
     * 创建节点
     *
     * @param requestParam
     * @return
     */
    NodeDO createNode(NodeCreateReqDTO requestParam);

    /**
     * 查询节点的所有子节点（包括子文件夹和文件）
     *
     * @param nodeId
     * @param libraryId
     * @return
     */
    List<NodeRespDTO> getAllDescendants(Long nodeId, Long libraryId);

    /**
     * 查询节点的直接子节点
     *
     * @param nodeId
     * @param libraryId
     * @return
     */
    List<NodeRespDTO> getDirectChildren(Long nodeId, Long libraryId);

    /**
     * 查询节点的祖先路径
     *
     * @param nodeId
     * @param libraryId
     * @return
     */
    List<NodePathRespDTO> getAncestors(Long nodeId, Long libraryId);

    /**
     * 获取节点的对象存储key
     *
     * @param nodeId
     * @return
     */
    String getStorageKey(Long nodeId);

    /**
     * 查询节点的完整路径
     *
     * @param nodeId
     * @param libraryId
     * @return
     */
    String getFullPath(Long nodeId, Long libraryId);

    /**
     * 修改节点配置信息
     *
     * @param requestParam
     */
    void updateNode(Long nodeId, NodeUpdateReqDTO requestParam);

    /**
     * 重命名
     *
     * @param requestParam
     */
    void rename(Long nodeId, NodeRenameReqDTO requestParam);

    /**
     * 移动节点(改变顺序)
     */
    void reorderNode();

    /**
     * 移动节点(改变父节点)
     *
     * @param requestParam
     */
    void moveNode(Long nodeId, NodeMoveReqDTO requestParam);

    /**
     * 删除节点以及子节点
     *
     * @param ancestorId
     * @param libraryId
     * @return
     */
    boolean deleteNodeAndChildren(Long ancestorId, Long libraryId);
}
