-- ============================================================
-- 创意工坊模块升级迁移脚本
-- 执行前提：已执行过 init-db.sql 且存在旧版 workshop_project 表
-- 如果是全新部署，直接执行最新的 init-db.sql 即可，无需本脚本
-- ============================================================

USE inspo_verse;

-- 1. 对旧版 workshop_project 表添加新字段（幂等执行）
-- 如果表不存在旧字段则跳过，如果已有同名列也不会报错

-- 删除旧列（如果存在）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='prompt_text');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE workshop_project DROP COLUMN prompt_text', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='result_content');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE workshop_project DROP COLUMN result_content', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 添加新列（幂等）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='tags');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN tags VARCHAR(512) NULL COMMENT ''标签(JSON数组)'' AFTER cover_url', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='version');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN version VARCHAR(32) NULL DEFAULT ''v1.0.0'' COMMENT ''版本号'' AFTER tags', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='file_url');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN file_url VARCHAR(512) NULL COMMENT ''文件下载URL'' AFTER version', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='file_size');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN file_size VARCHAR(32) NULL COMMENT ''文件大小描述'' AFTER file_url', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='category');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN category VARCHAR(32) NULL COMMENT ''分类'' AFTER file_size', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='download_count');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN download_count INT NOT NULL DEFAULT 0 COMMENT ''下载/订阅数'' AFTER favorite_count', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='rating_sum');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN rating_sum INT NOT NULL DEFAULT 0 COMMENT ''评分总和'' AFTER download_count', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='rating_count');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN rating_count INT NOT NULL DEFAULT 0 COMMENT ''评分人数'' AFTER rating_sum', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA='inspo_verse' AND TABLE_NAME='workshop_project' AND COLUMN_NAME='status');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE workshop_project ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''状态:1正常0下架2审核中'' AFTER rating_count', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 修改 description 列类型为 TEXT
ALTER TABLE workshop_project MODIFY COLUMN description TEXT NULL COMMENT '项目描述';

-- 2. 创建评分表
CREATE TABLE IF NOT EXISTS workshop_rating (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评分ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  project_id BIGINT NOT NULL COMMENT '工坊项目ID',
  score TINYINT NOT NULL COMMENT '评分1-10',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_wr_user_project (user_id, project_id),
  KEY idx_wr_project (project_id)
) ENGINE=InnoDB COMMENT='工坊项目评分表';

-- 3. 创建订阅表
CREATE TABLE IF NOT EXISTS workshop_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订阅ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  project_id BIGINT NOT NULL COMMENT '工坊项目ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_ws_user_project (user_id, project_id),
  KEY idx_ws_project (project_id),
  KEY idx_ws_user (user_id)
) ENGINE=InnoDB COMMENT='工坊项目订阅表';
