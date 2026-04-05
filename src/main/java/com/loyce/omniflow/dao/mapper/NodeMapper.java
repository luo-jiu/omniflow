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
    @Select("SELECT  " +
            "n.id," +
            "n.name," +
            "n.parent_id AS parentId," +
            "n.type," +
            "n.library_id AS libraryId," +
            "n.ext," +
            "n.mime_type AS mimeType," +
            "n.file_size AS fileSize," +
            "n.created_at AS createdAt " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.descendant " +
            "WHERE nc.ancestor = #{nodeId} AND nc.library_id = #{libraryId}")
    List<NodeRespDTO> findAllDescendants(Long nodeId, Long libraryId);

    // 查询节点的直接子节点（depth=1）
    @Select("SELECT  " +
            "n.id," +
            "n.name," +
            "n.parent_id AS parentId," +
            "n.type," +
            "n.library_id AS libraryId," +
            "n.ext," +
            "n.mime_type AS mimeType," +
            "n.file_size AS fileSize," +
            "n.created_at AS createdAt " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.descendant " +
            "WHERE nc.ancestor = #{nodeId} AND nc.depth = 1 AND nc.library_id = #{libraryId}")
    List<NodeRespDTO> findDirectChildren(Long nodeId, Long libraryId);

    // 查询节点的祖先路径
    @Select("SELECT n.id, n.name, nc.depth " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.ancestor " +
            "WHERE nc.descendant = #{nodeId} AND nc.library_id = #{libraryId} " +
            "ORDER BY nc.depth DESC")
    List<NodePathRespDTO> findAncestors(Long nodeId, Long libraryId);

    // 查询节点的完整路径
    @Select("SELECT n.id, n.name, nc.depth " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.ancestor " +
            "WHERE nc.descendant = #{nodeId} AND nc.library_id = #{libraryId} " +
            "ORDER BY nc.depth DESC")
    List<NodePathRespDTO> findFullPath(Long nodeId, Long libraryId);

    @Select("SELECT * FROM nodes WHERE id = #{id} AND library_id = #{libraryId}")
    NodeDO selectByIdAndLibraryId(Long id, Long libraryId);

    @Update("UPDATE nodes SET parent_id = #{newParentId}, sort_order = #{sortOrder} " +
            "WHERE id = #{nodeId} AND library_id = #{libraryId}")
    void updateParentId(Long nodeId, Long newParentId, Integer newOrder, Long libraryId);

    @Update("UPDATE nodes " +
            "SET built_in_type = #{requestParam.builtInType}, archive_mode = #{requestParam.archiveMode} " +
            "WHERE id = #{requestParam.id}")
    void updateNode(NodeUpdateReqDTO requestParam);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM nodes",
            "WHERE name = #{name}",
            "AND parent_id = #{parentId}",
            "AND library_id = #{libraryId}",
            "AND deleted_at IS NULL",
            "<if test='excludeId != null'>",
            "AND id != #{excludeId}",
            "</if>",
            "</script>"})
    Integer countByNameAndParent(String name, Long parentId, Long libraryId, Long excludeId);

    @Update("UPDATE nodes " +
            "SET name = #{name}, ext = #{ext} " +
            "WHERE id = #{id}")
    void rename(NodeRenameReqDTO requestParam);

    @Select("SELECT sort_order FROM nodes " +
            "WHERE parent_id = #{parentId} " +
            "AND library_id = #{libraryId} " +
            "ORDER BY sort_order DESC LIMIT 1")
    Integer getSortByLibraryIdAndParentId(Long parentId, Long libraryId);

    @Update("UPDATE nodes SET sort_order = sort_order + 1 " +
            "WHERE parent_id = #{parentId} " +
            "AND library_id = #{libraryId} " +
            "AND sort_order >= #{sortOrder}")
    void incrementSortOrderAfter(Long parentId, Long libraryId, Integer sortOrder);

    // 批量查询节点信息（用于删除时获取文件路径）
    @Select({
            "<script>",
            "SELECT * FROM nodes",
            "WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND library_id = #{libraryId}",
            "</script>"})
    List<NodeDO> selectByIdsAndLibraryId(List<Long> ids, Long libraryId);
}
