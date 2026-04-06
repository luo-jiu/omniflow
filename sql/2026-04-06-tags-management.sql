-- 标签管理模型升级（适配颜色、归属、排序、启停、软删除）
-- 执行前请确认当前库为 omniflow

ALTER TABLE `tags`
  MODIFY COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '标签id',
  MODIFY COLUMN `name` varchar(64) NOT NULL COMMENT '标签名称',
  MODIFY COLUMN `type` varchar(32) NOT NULL DEFAULT 'GENERAL' COMMENT '标签场景类型（ASMR/FILE_TAB/COMIC/GENERAL）',
  MODIFY COLUMN `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  ADD COLUMN `target_key` varchar(64) DEFAULT NULL COMMENT '场景目标键（FILE_TAB 使用）' AFTER `type`,
  ADD COLUMN `owner_user_id` bigint DEFAULT NULL COMMENT '归属用户ID，NULL=系统标签' AFTER `target_key`,
  ADD COLUMN `color` varchar(32) NOT NULL DEFAULT '#4F8CFF' COMMENT '标签主色（HEX）' AFTER `owner_user_id`,
  ADD COLUMN `text_color` varchar(32) DEFAULT NULL COMMENT '标签文字色（HEX，可空）' AFTER `color`,
  ADD COLUMN `sort_order` int NOT NULL DEFAULT '0' COMMENT '同场景排序' AFTER `text_color`,
  ADD COLUMN `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '启用状态 1启用 0停用' AFTER `sort_order`,
  ADD COLUMN `description` varchar(255) DEFAULT NULL COMMENT '标签描述' AFTER `enabled`,
  ADD COLUMN `updated_at` timestamp NULL DEFAULT NULL COMMENT '修改时间' AFTER `created_at`,
  ADD COLUMN `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间' AFTER `updated_at`,
  ADD COLUMN `active_unique_key` tinyint GENERATED ALWAYS AS (
    CASE WHEN `deleted_at` IS NULL THEN 1 ELSE NULL END
  ) VIRTUAL COMMENT '活跃记录唯一索引辅助列' AFTER `deleted_at`;

CREATE INDEX `idx_tags_owner_type_order`
  ON `tags` (`owner_user_id`, `type`, `sort_order`, `id`);

CREATE INDEX `idx_tags_type_order`
  ON `tags` (`type`, `sort_order`, `id`);

CREATE INDEX `idx_tags_type_target_order`
  ON `tags` (`type`, `target_key`, `sort_order`, `id`);

CREATE UNIQUE INDEX `uniq_tags_owner_type_target_active`
  ON `tags` (`owner_user_id`, `type`, `target_key`, `active_unique_key`);
