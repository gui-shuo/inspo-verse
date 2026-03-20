CREATE DATABASE IF NOT EXISTS inspo_verse
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE inspo_verse;

-- =========================
-- 用户与权限模块
-- =========================
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL COMMENT '登录用户名',
  email VARCHAR(128) NULL COMMENT '邮箱',
  phone VARCHAR(32) NULL COMMENT '手机号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  nickname VARCHAR(64) NOT NULL COMMENT '昵称',
  avatar_url VARCHAR(512) NULL COMMENT '头像URL',
  bio TEXT NULL COMMENT '个人简介',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1启用 0禁用',
  last_login_at DATETIME NULL COMMENT '最后登录时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0否1是',
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_phone (phone),
  KEY idx_users_status_created (status, created_at),
  KEY idx_users_deleted_created (is_deleted, created_at)
) ENGINE=InnoDB COMMENT='用户主表';

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_roles_code (role_code),
  KEY idx_roles_deleted (is_deleted)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE IF NOT EXISTS user_role_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_urr_role (role_id)
) ENGINE=InnoDB COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS vip_plans (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'VIP方案ID',
  plan_code VARCHAR(64) NOT NULL COMMENT '方案编码',
  plan_name VARCHAR(64) NOT NULL COMMENT '方案名称',
  price_cents INT NOT NULL COMMENT '价格(分)',
  duration_days INT NOT NULL COMMENT '有效天数',
  level TINYINT NOT NULL COMMENT '会员等级',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1上架0下架',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_vip_plan_code (plan_code),
  KEY idx_vip_plan_status (status, is_deleted)
) ENGINE=InnoDB COMMENT='VIP套餐表';

CREATE TABLE IF NOT EXISTS vip_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
  order_no VARCHAR(64) NOT NULL COMMENT '订单号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  vip_plan_id BIGINT NOT NULL COMMENT '套餐ID',
  amount_cents INT NOT NULL COMMENT '支付金额(分)',
  pay_channel VARCHAR(32) NULL COMMENT '支付渠道',
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态:0待支付1成功2失败3退款',
  paid_at DATETIME NULL COMMENT '支付完成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_vip_order_no (order_no),
  KEY idx_vip_orders_user_status (user_id, pay_status, created_at),
  KEY idx_vip_orders_created (created_at)
) ENGINE=InnoDB COMMENT='VIP订单表';

CREATE TABLE IF NOT EXISTS vip_memberships (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会员记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  vip_level TINYINT NOT NULL COMMENT '会员等级',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  source_order_no VARCHAR(64) NULL COMMENT '来源订单号',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1有效0失效',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  KEY idx_vip_member_user_time (user_id, status, end_time),
  KEY idx_vip_member_expire (status, end_time)
) ENGINE=InnoDB COMMENT='VIP会员有效期表';

-- =========================
-- AI 对话与创意工坊模块
-- =========================
CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
  session_no VARCHAR(64) NOT NULL COMMENT '会话编号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  title VARCHAR(128) NULL COMMENT '会话标题',
  model_name VARCHAR(64) NOT NULL COMMENT '模型名称',
  scene VARCHAR(32) NOT NULL COMMENT '场景:chat/workshop',
  token_used INT NOT NULL DEFAULT 0 COMMENT '累计消耗token',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_ai_chat_session_no (session_no),
  KEY idx_ai_chat_user_updated (user_id, is_deleted, updated_at),
  KEY idx_ai_chat_scene_created (scene, created_at)
) ENGINE=InnoDB COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
  session_id BIGINT NOT NULL COMMENT '会话ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role VARCHAR(16) NOT NULL COMMENT '角色:user/assistant/system',
  content MEDIUMTEXT NOT NULL COMMENT '消息内容',
  content_md MEDIUMTEXT NULL COMMENT 'Markdown内容',
  tokens INT NOT NULL DEFAULT 0 COMMENT 'token数量',
  latency_ms INT NULL COMMENT '响应耗时毫秒',
  safety_flag TINYINT NOT NULL DEFAULT 0 COMMENT '安全标记:0正常1违规',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  KEY idx_ai_msg_session_created (session_id, created_at),
  KEY idx_ai_msg_user_created (user_id, created_at),
  KEY idx_ai_msg_role_created (role, created_at)
) ENGINE=InnoDB COMMENT='AI消息明细表';

CREATE TABLE IF NOT EXISTS workshop_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工坊项目ID',
  project_no VARCHAR(64) NOT NULL COMMENT '项目编号',
  user_id BIGINT NOT NULL COMMENT '创建用户ID',
  title VARCHAR(128) NOT NULL COMMENT '项目标题',
  description VARCHAR(1024) NULL COMMENT '项目描述',
  prompt_text TEXT NULL COMMENT '生成提示词',
  result_content MEDIUMTEXT NULL COMMENT '生成结果',
  cover_url VARCHAR(512) NULL COMMENT '封面图',
  visibility TINYINT NOT NULL DEFAULT 1 COMMENT '可见性:1公开2私有',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_workshop_project_no (project_no),
  KEY idx_workshop_user_created (user_id, created_at),
  KEY idx_workshop_visibility_hot (visibility, like_count, favorite_count),
  KEY idx_workshop_deleted_updated (is_deleted, updated_at)
) ENGINE=InnoDB COMMENT='创意工坊项目表';

-- =========================
-- 社区论坛模块
-- =========================
CREATE TABLE IF NOT EXISTS forum_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
  post_no VARCHAR(64) NOT NULL COMMENT '帖子编号',
  user_id BIGINT NOT NULL COMMENT '发帖人ID',
  category VARCHAR(32) NOT NULL COMMENT '分类:anime/game/chat/share',
  title VARCHAR(256) NOT NULL COMMENT '标题',
  content MEDIUMTEXT NOT NULL COMMENT '正文内容',
  tags VARCHAR(512) NULL COMMENT '标签(逗号分隔)',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览数',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数',
  is_top TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  is_essence TINYINT NOT NULL DEFAULT 0 COMMENT '是否精华',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常0下架2审核中',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_forum_post_no (post_no),
  KEY idx_forum_post_category_created (category, status, created_at),
  KEY idx_forum_post_hot (status, is_top, like_count, comment_count),
  KEY idx_forum_post_user_created (user_id, created_at)
) ENGINE=InnoDB COMMENT='论坛帖子表';

CREATE TABLE IF NOT EXISTS forum_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  post_id BIGINT NOT NULL COMMENT '帖子ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  parent_comment_id BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID(0为根评论)',
  reply_to_user_id BIGINT NULL COMMENT '回复目标用户ID',
  content TEXT NOT NULL COMMENT '评论内容',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常0删除2审核中',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  KEY idx_forum_comment_post_created (post_id, status, created_at),
  KEY idx_forum_comment_parent (parent_comment_id, created_at),
  KEY idx_forum_comment_user_created (user_id, created_at)
) ENGINE=InnoDB COMMENT='论坛评论表';

CREATE TABLE IF NOT EXISTS forum_interaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '互动ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  target_type VARCHAR(16) NOT NULL COMMENT '目标类型:post/comment/project',
  target_id BIGINT NOT NULL COMMENT '目标ID',
  action_type VARCHAR(16) NOT NULL COMMENT '行为:like/favorite/report',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_forum_interaction (user_id, target_type, target_id, action_type),
  KEY idx_forum_interaction_target (target_type, target_id, action_type),
  KEY idx_forum_interaction_user (user_id, created_at)
) ENGINE=InnoDB COMMENT='社区互动行为表';

-- =========================
-- 基础配置模块
-- =========================
CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  config_key VARCHAR(128) NOT NULL COMMENT '配置键',
  config_value TEXT NOT NULL COMMENT '配置值',
  value_type VARCHAR(32) NOT NULL DEFAULT 'string' COMMENT '值类型',
  remark VARCHAR(512) NULL COMMENT '备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_sys_config_key (config_key),
  KEY idx_sys_config_deleted (is_deleted)
) ENGINE=InnoDB COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID',
  operator_user_id BIGINT NULL COMMENT '操作者ID',
  module VARCHAR(64) NOT NULL COMMENT '模块',
  action VARCHAR(64) NOT NULL COMMENT '动作',
  request_uri VARCHAR(512) NOT NULL COMMENT '请求URI',
  request_method VARCHAR(16) NOT NULL COMMENT '请求方法',
  response_code VARCHAR(16) NOT NULL COMMENT '响应码',
  cost_ms INT NOT NULL COMMENT '耗时毫秒',
  ip VARCHAR(64) NULL COMMENT '来源IP',
  user_agent VARCHAR(512) NULL COMMENT 'UA',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_op_log_trace (trace_id),
  KEY idx_op_log_module_time (module, created_at),
  KEY idx_op_log_user_time (operator_user_id, created_at)
) ENGINE=InnoDB COMMENT='操作审计日志';

-- =========================
-- 用户创作模块
-- =========================
CREATE TABLE IF NOT EXISTS user_creations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '创作ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  title VARCHAR(255) NULL COMMENT '作品标题',
  description TEXT NULL COMMENT '作品描述',
  file_url VARCHAR(512) NOT NULL COMMENT '文件存储路径/URL',
  cover_url VARCHAR(512) NULL COMMENT '封面图URL',
  file_type VARCHAR(32) NOT NULL DEFAULT 'image' COMMENT '文件类型:image/video/audio/other',
  file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  visibility TINYINT NOT NULL DEFAULT 0 COMMENT '可见性:0私密 1公开',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常 0已删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  KEY idx_uc_user_created (user_id, status, created_at),
  KEY idx_uc_visibility (visibility, status, created_at)
) ENGINE=InnoDB COMMENT='用户创作表';

-- =========================
-- 数字钱包模块
-- =========================
CREATE TABLE IF NOT EXISTS user_points (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '钱包ID',
  user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
  balance INT NOT NULL DEFAULT 0 COMMENT '当前余额(点数)',
  total_earned INT NOT NULL DEFAULT 0 COMMENT '历史累计获得',
  total_spent INT NOT NULL DEFAULT 0 COMMENT '历史累计消耗',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_up_user (user_id)
) ENGINE=InnoDB COMMENT='用户灵感点数钱包';

CREATE TABLE IF NOT EXISTS point_transactions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  amount INT NOT NULL COMMENT '变动点数(正为收入负为支出)',
  type VARCHAR(32) NOT NULL COMMENT '类型:EARN_SIGNIN/EARN_RECHARGE/EARN_VIP_GIFT/SPEND_AI/SPEND_REDEEM',
  description VARCHAR(255) NULL COMMENT '描述',
  balance_after INT NOT NULL COMMENT '变动后余额',
  ref_id VARCHAR(64) NULL COMMENT '关联业务单号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_pt_user_created (user_id, created_at)
) ENGINE=InnoDB COMMENT='灵感点数流水表';

-- =========================
-- 第三方账号绑定
-- =========================
CREATE TABLE IF NOT EXISTS user_oauth_bindings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '绑定ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  provider VARCHAR(32) NOT NULL COMMENT '平台:github/discord/google',
  provider_user_id VARCHAR(128) NOT NULL COMMENT '平台用户ID',
  provider_username VARCHAR(128) NULL COMMENT '平台用户名',
  access_token TEXT NULL COMMENT 'Access Token(加密存储)',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_provider (user_id, provider),
  UNIQUE KEY uk_provider_uid (provider, provider_user_id),
  KEY idx_uob_user (user_id)
) ENGINE=InnoDB COMMENT='第三方OAuth绑定表';

-- =========================
-- 初始化数据
-- =========================
INSERT IGNORE INTO roles (role_code, role_name) VALUES
  ('ROLE_USER', '普通用户'),
  ('ROLE_VIP', 'VIP会员'),
  ('ROLE_ADMIN', '管理员');

INSERT IGNORE INTO vip_plans (plan_code, plan_name, price_cents, duration_days, level, status) VALUES
  ('SILVER_MONTH', '白银月卡', 1900, 30, 1, 1),
  ('GOLD_MONTH', '黄金月卡', 3900, 30, 2, 1),
  ('GOLD_QUARTER', '黄金季卡', 9900, 90, 2, 1),
  ('GOLD_YEAR', '黄金年卡', 29900, 365, 2, 1);

-- =========================
-- 充值支付订单表
-- =========================
CREATE TABLE IF NOT EXISTS payment_orders (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT NOT NULL COMMENT '用户ID',
  order_no    VARCHAR(64) NOT NULL COMMENT '商户订单号',
  package_id  VARCHAR(20) NOT NULL COMMENT '充值套餐ID',
  amount      DECIMAL(10,2) NOT NULL COMMENT '支付金额（元）',
  points      INT NOT NULL COMMENT '购买灵感点数',
  pay_method  VARCHAR(20) NOT NULL COMMENT '支付方式：ALIPAY / WECHAT',
  status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING / PAID / EXPIRED / FAILED',
  pay_url     VARCHAR(1024) NULL COMMENT '支付二维码链接或 mock 标识',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  expired_at  DATETIME NOT NULL COMMENT '订单过期时间',
  paid_at     DATETIME NULL COMMENT '实际支付时间',
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_po_user_status (user_id, status),
  KEY idx_po_created (created_at)
) ENGINE=InnoDB COMMENT='充值支付订单';
