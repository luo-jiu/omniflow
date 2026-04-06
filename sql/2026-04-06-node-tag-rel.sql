-- 节点标签关系表（用于高性能标签检索）
-- 执行前请确认当前库为 omniflow

CREATE TABLE IF NOT EXISTS `node_tag_rel` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `node_id` bigint unsigned NOT NULL COMMENT '节点id',
  `tag_id` bigint unsigned NOT NULL COMMENT '标签id',
  `library_id` bigint NOT NULL COMMENT '所属库id',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_node_tag` (`node_id`,`tag_id`) USING BTREE,
  KEY `idx_lib_tag_node` (`library_id`,`tag_id`,`node_id`) USING BTREE,
  KEY `idx_lib_node` (`library_id`,`node_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- 从 nodes.view_meta.tagIds 回填历史关系（仅处理 JSON 合法的记录）
INSERT IGNORE INTO `node_tag_rel` (`node_id`, `tag_id`, `library_id`)
SELECT
  n.id AS node_id,
  jt.tag_id AS tag_id,
  n.library_id AS library_id
FROM `nodes` n
JOIN JSON_TABLE(
  JSON_EXTRACT(n.view_meta, '$.tagIds'),
  '$[*]' COLUMNS (
    tag_id BIGINT PATH '$'
  )
) jt
WHERE n.deleted_at IS NULL
  AND n.view_meta IS NOT NULL
  AND JSON_VALID(n.view_meta) = 1;
