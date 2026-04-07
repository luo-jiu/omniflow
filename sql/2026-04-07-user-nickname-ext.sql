-- 增量迁移：user 表新增 nickname / ext
-- 字段顺序：
-- 1) nickname 放在 username 后
-- 2) ext 放在 email 后

SET @schema_name := IFNULL(DATABASE(), 'omniflow');

-- 添加 nickname（若不存在）
SET @exists_nickname := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'nickname'
);
SET @sql_nickname := IF(
  @exists_nickname = 0,
  'ALTER TABLE `user` ADD COLUMN `nickname` varchar(100) DEFAULT NULL COMMENT ''昵称'' AFTER `username`',
  'SELECT 1'
);
PREPARE stmt_nickname FROM @sql_nickname;
EXECUTE stmt_nickname;
DEALLOCATE PREPARE stmt_nickname;

-- 添加 ext（若不存在）
SET @exists_ext := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @schema_name
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'ext'
);
SET @sql_ext := IF(
  @exists_ext = 0,
  'ALTER TABLE `user` ADD COLUMN `ext` text COMMENT ''扩展信息（JSON）'' AFTER `email`',
  'SELECT 1'
);
PREPARE stmt_ext FROM @sql_ext;
EXECUTE stmt_ext;
DEALLOCATE PREPARE stmt_ext;

-- 为历史数据补齐 nickname（仅空值）
UPDATE `user`
SET `nickname` = `username`
WHERE (`nickname` IS NULL OR `nickname` = '')
  AND `deleted_at` IS NULL;
