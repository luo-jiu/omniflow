package com.loyce.omniflow.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.loyce.omniflow.dao.entity.NodeClosureDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface NodeClosureMapper extends BaseMapper<NodeClosureDO> {

    @Select("SELECT * FROM node_closure WHERE descendant = #{parentId} AND library_id = #{libraryId}")
    List<NodeClosureDO> findAncestorsByParentId(Integer parentId, Integer libraryId);

    @Select("SELECT COUNT(*) FROM node_closure WHERE ancestor = #{nodeId} AND descendant = #{targetId} AND library_id = #{libraryId}")
    Integer existsDescendant(Integer nodeId, Integer targetId, Integer libraryId);

    @Delete("DELETE FROM node_closure " +
            "WHERE (descendant, ancestor) IN (" +
            "   SELECT * FROM (" +
            "       SELECT nc1.descendant, nc2.ancestor " +
            "       FROM node_closure nc1 " +
            "       JOIN node_closure nc2 ON 1=1 " +
            "       WHERE nc1.ancestor = #{nodeId} AND nc1.library_id = #{libraryId} " +
            "       AND nc2.descendant = #{nodeId} AND nc2.ancestor != #{nodeId} AND nc2.library_id = #{libraryId}" +
            "   ) AS tmp" +
            ")")
    void deleteOldRelations(Integer nodeId, Integer libraryId);

    @Insert("INSERT INTO node_closure (ancestor, descendant, depth, library_id) " +
            "SELECT a.ancestor, d.descendant, a.depth + d.depth + 1, #{libraryId} " +
            "FROM node_closure a " +
            "CROSS JOIN node_closure d " +
            "WHERE a.descendant = #{newParentId} AND a.library_id = #{libraryId} " +
            "AND d.ancestor = #{nodeId} AND d.library_id = #{libraryId}")
    void insertNewRelations(Integer nodeId, Integer newParentId, Integer libraryId);

    @Delete("DELETE FROM node_closure " +
            "WHERE descendant IN (" +
            "   SELECT * FROM (" +
            "       SELECT descendant FROM node_closure " +
            "       WHERE ancestor = #{ancestorId} AND library_id = #{libraryId}" +
            "   ) AS tmp" +
            ") AND library_id = #{libraryId}")
    int deleteClosuresByAncestorAndLibrary(Long ancestorId, Long libraryId);

    @Select("SELECT descendant FROM node_closure WHERE ancestor = #{ancestorId} AND library_id = #{libraryId}")
    List<Long> selectDescendantIdsByAncestorAndLibrary(Long ancestorId, Long libraryId);
}
