package com.loyce.omniflow.dao.mapper;

import com.loyce.omniflow.dao.entity.NodeTagRelDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NodeTagRelMapper {

    @Delete("DELETE FROM node_tag_rel WHERE node_id = #{nodeId}")
    int deleteByNodeId(@Param("nodeId") Long nodeId);

    @Delete({
            "<script>",
            "DELETE FROM node_tag_rel",
            "WHERE node_id IN",
            "<foreach collection='nodeIds' item='nodeId' open='(' separator=',' close=')'>",
            "#{nodeId}",
            "</foreach>",
            "</script>"
    })
    int deleteByNodeIds(@Param("nodeIds") List<Long> nodeIds);

    @Insert({
            "<script>",
            "INSERT INTO node_tag_rel (node_id, tag_id, library_id) VALUES",
            "<foreach collection='rels' item='item' separator=','>",
            "(#{item.nodeId}, #{item.tagId}, #{item.libraryId})",
            "</foreach>",
            "</script>"
    })
    int batchInsert(@Param("rels") List<NodeTagRelDO> rels);
}
