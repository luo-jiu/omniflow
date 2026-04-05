package com.loyce.omniflow.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.loyce.omniflow.dao.entity.NodeDO;
import com.loyce.omniflow.dto.req.NodeRenameReqDTO;
import com.loyce.omniflow.dto.req.NodeUpdateReqDTO;
import com.loyce.omniflow.dto.resp.NodeRecycleRespDTO;
import com.loyce.omniflow.dto.resp.NodePathRespDTO;
import com.loyce.omniflow.dto.resp.NodeRespDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
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
            "n.built_in_type AS builtInType," +
            "n.archive_mode AS archiveMode," +
            "n.created_at AS createdAt " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.descendant " +
            "WHERE nc.ancestor = #{nodeId} AND nc.library_id = #{libraryId} AND n.deleted_at IS NULL")
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
            "n.built_in_type AS builtInType," +
            "n.archive_mode AS archiveMode," +
            "n.created_at AS createdAt " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.descendant " +
            "WHERE nc.ancestor = #{nodeId} AND nc.depth = 1 AND nc.library_id = #{libraryId} AND n.deleted_at IS NULL " +
            "ORDER BY n.sort_order ASC, n.id ASC")
    List<NodeRespDTO> findDirectChildren(Long nodeId, Long libraryId);

    // 查询节点的祖先路径
    @Select("SELECT n.id, n.name, nc.depth " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.ancestor " +
            "WHERE nc.descendant = #{nodeId} AND nc.library_id = #{libraryId} " +
            "AND n.deleted_at IS NULL " +
            "ORDER BY nc.depth DESC")
    List<NodePathRespDTO> findAncestors(Long nodeId, Long libraryId);

    // 查询节点的完整路径
    @Select("SELECT n.id, n.name, nc.depth " +
            "FROM nodes n " +
            "JOIN node_closure nc ON n.id = nc.ancestor " +
            "WHERE nc.descendant = #{nodeId} AND nc.library_id = #{libraryId} " +
            "AND n.deleted_at IS NULL " +
            "ORDER BY nc.depth DESC")
    List<NodePathRespDTO> findFullPath(Long nodeId, Long libraryId);

    @Select("SELECT * FROM nodes WHERE id = #{id} AND library_id = #{libraryId} AND deleted_at IS NULL")
    NodeDO selectByIdAndLibraryId(Long id, Long libraryId);

    @Select("SELECT * FROM nodes WHERE id = #{id} AND library_id = #{libraryId}")
    NodeDO selectByIdAndLibraryIdIncludeDeleted(Long id, Long libraryId);

    @Update("UPDATE nodes SET parent_id = #{newParentId}, sort_order = #{newOrder} " +
            "WHERE id = #{nodeId} AND library_id = #{libraryId}")
    void updateParentId(Long nodeId, Long newParentId, Integer newOrder, Long libraryId);

    @Update("UPDATE nodes SET sort_order = #{sortOrder} " +
            "WHERE id = #{nodeId} AND library_id = #{libraryId}")
    void updateSortOrder(Long nodeId, Long libraryId, Integer sortOrder);

    @Update("UPDATE nodes " +
            "SET built_in_type = #{builtInType}, archive_mode = #{archiveMode} " +
            "WHERE id = #{id}")
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
            "AND deleted_at IS NULL " +
            "ORDER BY sort_order DESC LIMIT 1")
    Integer getSortByLibraryIdAndParentId(Long parentId, Long libraryId);

    @Update("UPDATE nodes SET sort_order = sort_order + 1 " +
            "WHERE parent_id = #{parentId} " +
            "AND library_id = #{libraryId} " +
            "AND deleted_at IS NULL " +
            "AND sort_order >= #{sortOrder}")
    void incrementSortOrderAfter(Long parentId, Long libraryId, Integer sortOrder);

    @Select("SELECT sort_order FROM nodes " +
            "WHERE parent_id = #{parentId} " +
            "AND library_id = #{libraryId} " +
            "AND deleted_at IS NULL " +
            "AND (sort_order < #{beforeSortOrder} OR (sort_order = #{beforeSortOrder} AND id < #{beforeNodeId})) " +
            "ORDER BY sort_order DESC, id DESC LIMIT 1")
    Integer getPrevSortOrderBeforeNode(Long parentId, Long libraryId, Integer beforeSortOrder, Long beforeNodeId);

    @Select("SELECT id, sort_order AS sortOrder FROM nodes " +
            "WHERE parent_id = #{parentId} " +
            "AND library_id = #{libraryId} " +
            "AND deleted_at IS NULL " +
            "ORDER BY sort_order ASC, id ASC")
    List<NodeDO> selectActiveChildrenForReindex(Long parentId, Long libraryId);

    @Select("SELECT id, name, ext, type FROM nodes " +
            "WHERE parent_id = #{parentId} " +
            "AND library_id = #{libraryId} " +
            "AND deleted_at IS NULL " +
            "ORDER BY id ASC")
    List<NodeDO> selectActiveChildrenForSortByName(Long parentId, Long libraryId);

    @Select("SELECT id FROM nodes " +
            "WHERE id = #{nodeId} " +
            "AND library_id = #{libraryId} " +
            "AND deleted_at IS NULL " +
            "FOR UPDATE")
    Long lockNode(Long nodeId, Long libraryId);

    @Select("SELECT id FROM nodes " +
            "WHERE parent_id = #{parentId} " +
            "AND library_id = #{libraryId} " +
            "AND deleted_at IS NULL " +
            "ORDER BY sort_order ASC, id ASC " +
            "FOR UPDATE")
    List<Long> lockActiveChildrenByParent(Long parentId, Long libraryId);

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

    @Select("SELECT " +
            "n.id, n.name, n.ext, n.mime_type AS mimeType, n.file_size AS fileSize, n.type, " +
            "n.parent_id AS parentId, n.library_id AS libraryId, n.deleted_at AS deletedAt, " +
            "(" +
            "  SELECT COUNT(1) " +
            "  FROM node_closure nc " +
            "  JOIN nodes child ON child.id = nc.descendant AND child.library_id = n.library_id " +
            "  WHERE nc.ancestor = n.id " +
            "    AND nc.library_id = n.library_id " +
            "    AND nc.depth > 0 " +
            "    AND child.deleted_at IS NOT NULL" +
            ") AS deletedDescendantCount " +
            "FROM nodes n " +
            "WHERE n.library_id = #{libraryId} AND n.deleted_at IS NOT NULL " +
            "ORDER BY n.deleted_at DESC, n.id DESC")
    List<NodeRecycleRespDTO> selectDeletedByLibraryId(Long libraryId);

    @Select("SELECT DISTINCT library_id FROM nodes WHERE deleted_at IS NOT NULL")
    List<Long> selectLibraryIdsWithDeletedNodes();

    @Update({
            "<script>",
            "UPDATE nodes",
            "SET deleted_at = NULL",
            "WHERE library_id = #{libraryId}",
            "AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND deleted_at IS NOT NULL",
            "</script>"})
    int restoreByIds(@Param("ids") List<Long> ids, @Param("libraryId") Long libraryId);

    @Delete({
            "<script>",
            "DELETE FROM nodes",
            "WHERE library_id = #{libraryId}",
            "AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"})
    int hardDeleteByIds(@Param("ids") List<Long> ids, @Param("libraryId") Long libraryId);
}
