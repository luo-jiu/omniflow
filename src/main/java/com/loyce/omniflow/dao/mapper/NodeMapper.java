package com.loyce.omniflow.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.loyce.omniflow.dao.entity.NodeDO;
import com.loyce.omniflow.dto.req.NodeRenameReqDTO;
import com.loyce.omniflow.dto.req.NodeUpdateReqDTO;
import com.loyce.omniflow.dto.resp.NodePathRespDTO;
import com.loyce.omniflow.dto.resp.NodeRespDTO;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 节点持久层
 */
public interface NodeMapper extends BaseMapper<NodeDO> {

    // 查询节点的所有子节点（包括子文件夹和文件）
    @Select("SELECT n.id, n.name, n.parent_id AS parentId, n.type, n.library_id AS libraryId, n.created_at AS createdAt " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.descendant " +
            "WHERE nc.ancestor = #{nodeId} AND nc.library_id = #{libraryId}")
    List<NodeRespDTO> findAllDescendants(Integer nodeId, Integer libraryId);

    // 查询节点的直接子节点（depth=1）
    @Select("SELECT n.id, n.name, n.parent_id AS parentId, n.type, n.library_id AS libraryId, n.created_at AS createdAt " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.descendant " +
            "WHERE nc.ancestor = #{nodeId} AND nc.depth = 1 AND nc.library_id = #{libraryId}")
    List<NodeRespDTO> findDirectChildren(Integer nodeId, Integer libraryId);

    // 查询节点的祖先路径
    @Select("SELECT n.id, n.name, nc.depth " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.ancestor " +
            "WHERE nc.descendant = #{nodeId} AND nc.library_id = #{libraryId} " +
            "ORDER BY nc.depth DESC")
    List<NodePathRespDTO> findAncestors(Integer nodeId, Integer libraryId);

    // 查询节点的完整路径
    @Select("SELECT n.id, n.name, nc.depth " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.ancestor " +
            "WHERE nc.descendant = #{nodeId} AND nc.library_id = #{libraryId} " +
            "ORDER BY nc.depth DESC")
    List<NodePathRespDTO> findFullPath(Integer nodeId, Integer libraryId);

    @Select("SELECT * FROM nodes WHERE id = #{id} AND library_id = #{libraryId}")
    NodeDO selectByIdAndLibraryId(Integer id, Integer libraryId);

    @Update("UPDATE nodes SET parent_id = #{newParentId} WHERE id = #{nodeId} AND library_id = #{libraryId}")
    void updateParentId(Integer nodeId, Integer newParentId, Integer libraryId);

    @Update("UPDATE nodes " +
            "SET built_in_type = #{requestParam.builtInType}, archive_mode = #{requestParam.archiveMode} " +
            "WHERE id = #{requestParam.id}")
    void updateNode(NodeUpdateReqDTO requestParam);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM node",
            "WHERE name = #{name}",
            "AND parent_id = #{parentId}",
            "AND library_id = #{libraryId}",
            "<if test='excludeId != null'>",
            "AND id != #{excludeId}",
            "</if>",
            "</script>"})
    int countByNameAndParent(String name, Integer parentId, Integer libraryId, Integer excludeId);

    @Update("UPDATE nodes " +
            "SET name = #{requestParam.name} " +
            "WHERE id = #{requestParam.id}")
    void rename(NodeRenameReqDTO requestParam);
}
