-- 增量迁移：为旧版 tags 表补齐 target_key 与活跃唯一索引
-- 可重复执行：已存在的列/索引会自动跳过

SET @db_name := DATABASE();

SET @has_target_key := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'tags'
    AND COLUMN_NAME = 'target_key'
);
SET @sql_target_key := IF(
  @has_target_key = 0,
  'ALTER TABLE `tags` ADD COLUMN `target_key` varchar(64) DEFAULT NULL COMMENT ''场景目标键（FILE_TAB 使用）'' AFTER `type`',
  'SELECT 1'
);
PREPARE stmt_target_key FROM @sql_target_key;
EXECUTE stmt_target_key;
DEALLOCATE PREPARE stmt_target_key;

SET @has_deleted_at := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'tags'
    AND COLUMN_NAME = 'deleted_at'
);
SET @sql_deleted_at := IF(
  @has_deleted_at = 0,
  'ALTER TABLE `tags` ADD COLUMN `deleted_at` timestamp NULL DEFAULT NULL COMMENT ''删除时间''',
  'SELECT 1'
);
PREPARE stmt_deleted_at FROM @sql_deleted_at;
EXECUTE stmt_deleted_at;
DEALLOCATE PREPARE stmt_deleted_at;

SET @has_active_unique_key := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'tags'
    AND COLUMN_NAME = 'active_unique_key'
);
SET @sql_active_unique_key := IF(
  @has_active_unique_key = 0,
  'ALTER TABLE `tags` ADD COLUMN `active_unique_key` tinyint GENERATED ALWAYS AS (CASE WHEN `deleted_at` IS NULL THEN 1 ELSE NULL END) VIRTUAL COMMENT ''活跃记录唯一索引辅助列'' AFTER `deleted_at`',
  'SELECT 1'
);
PREPARE stmt_active_unique_key FROM @sql_active_unique_key;
EXECUTE stmt_active_unique_key;
DEALLOCATE PREPARE stmt_active_unique_key;

SET @has_idx_type_target_order := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'tags'
    AND INDEX_NAME = 'idx_tags_type_target_order'
);
SET @sql_idx_type_target_order := IF(
  @has_idx_type_target_order = 0,
  'CREATE INDEX `idx_tags_type_target_order` ON `tags` (`type`, `target_key`, `sort_order`, `id`)',
  'SELECT 1'
);
PREPARE stmt_idx_type_target_order FROM @sql_idx_type_target_order;
EXECUTE stmt_idx_type_target_order;
DEALLOCATE PREPARE stmt_idx_type_target_order;

SET @has_old_unique := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'tags'
    AND INDEX_NAME = 'uniq_tags_owner_type_target'
);
SET @sql_drop_old_unique := IF(
  @has_old_unique > 0,
  'DROP INDEX `uniq_tags_owner_type_target` ON `tags`',
  'SELECT 1'
);
PREPARE stmt_drop_old_unique FROM @sql_drop_old_unique;
EXECUTE stmt_drop_old_unique;
DEALLOCATE PREPARE stmt_drop_old_unique;

SET @has_new_unique := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'tags'
    AND INDEX_NAME = 'uniq_tags_owner_type_target_active'
);
SET @sql_new_unique := IF(
  @has_new_unique = 0,
  'CREATE UNIQUE INDEX `uniq_tags_owner_type_target_active` ON `tags` (`owner_user_id`, `type`, `target_key`, `active_unique_key`)',
  'SELECT 1'
);
PREPARE stmt_new_unique FROM @sql_new_unique;
EXECUTE stmt_new_unique;
DEALLOCATE PREPARE stmt_new_unique;
