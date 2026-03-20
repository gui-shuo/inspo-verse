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
  description TEXT NULL COMMENT '项目描述',
  cover_url VARCHAR(512) NULL COMMENT '封面图',
  tags VARCHAR(512) NULL COMMENT '标签(JSON数组)',
  version VARCHAR(32) NULL DEFAULT 'v1.0.0' COMMENT '版本号',
  file_url VARCHAR(512) NULL COMMENT '文件/资源下载URL',
  file_size VARCHAR(32) NULL COMMENT '文件大小描述(如15.4 MB)',
  category VARCHAR(32) NULL COMMENT '分类:ui/tool/shader/theme/audio/model/other',
  visibility TINYINT NOT NULL DEFAULT 1 COMMENT '可见性:1公开2私有',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数',
  download_count INT NOT NULL DEFAULT 0 COMMENT '下载/订阅数',
  rating_sum INT NOT NULL DEFAULT 0 COMMENT '评分总和(实际分x10存储,如4.9分存49)',
  rating_count INT NOT NULL DEFAULT 0 COMMENT '评分人数',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常0下架2审核中',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_workshop_project_no (project_no),
  KEY idx_workshop_user_created (user_id, created_at),
  KEY idx_workshop_visibility_hot (visibility, is_deleted, status, download_count),
  KEY idx_workshop_category (category, status, is_deleted),
  KEY idx_workshop_deleted_updated (is_deleted, updated_at)
) ENGINE=InnoDB COMMENT='创意工坊项目表';

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

CREATE TABLE IF NOT EXISTS workshop_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订阅ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  project_id BIGINT NOT NULL COMMENT '工坊项目ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_ws_user_project (user_id, project_id),
  KEY idx_ws_project (project_id),
  KEY idx_ws_user (user_id)
) ENGINE=InnoDB COMMENT='工坊项目订阅表';

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
-- 发现内容模块
-- =========================
CREATE TABLE IF NOT EXISTS content_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '内容ID',
  content_no VARCHAR(64) NOT NULL COMMENT '内容编号',
  user_id BIGINT NOT NULL COMMENT '发布者ID',
  category VARCHAR(32) NOT NULL COMMENT '分类:ai-art/game/anime',
  title VARCHAR(256) NOT NULL COMMENT '标题',
  description TEXT NULL COMMENT '详细描述',
  cover_url VARCHAR(512) NULL COMMENT '封面图URL',
  images TEXT NULL COMMENT '图片URL列表(JSON数组)',
  tag VARCHAR(128) NULL COMMENT '标签',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览数',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常0下架2审核中',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_content_no (content_no),
  KEY idx_ci_category_created (category, status, created_at),
  KEY idx_ci_user_created (user_id, created_at),
  KEY idx_ci_hot (status, like_count DESC, comment_count DESC, view_count DESC)
) ENGINE=InnoDB COMMENT='发现内容表';

CREATE TABLE IF NOT EXISTS content_comments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  content_id BIGINT NOT NULL COMMENT '内容ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  parent_comment_id BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID(0为根评论)',
  reply_to_user_id BIGINT NULL COMMENT '回复目标用户ID',
  content TEXT NOT NULL COMMENT '评论内容',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常0删除',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  KEY idx_cc_content_created (content_id, status, created_at),
  KEY idx_cc_user_created (user_id, created_at)
) ENGINE=InnoDB COMMENT='内容评论表';

CREATE TABLE IF NOT EXISTS user_follows (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关注ID',
  follower_id BIGINT NOT NULL COMMENT '关注者ID',
  following_id BIGINT NOT NULL COMMENT '被关注者ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_follow (follower_id, following_id),
  KEY idx_uf_following (following_id)
) ENGINE=InnoDB COMMENT='用户关注关系表';

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
-- 动漫番剧模块
-- =========================
CREATE TABLE IF NOT EXISTS anime_series (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '番剧ID',
  series_no VARCHAR(64) NOT NULL COMMENT '番剧编号',
  title VARCHAR(256) NOT NULL COMMENT '番剧名称',
  description TEXT COMMENT '番剧介绍',
  cover_url VARCHAR(512) COMMENT '封面图URL',
  hero_url VARCHAR(512) COMMENT '详情页大图URL',
  author_name VARCHAR(128) COMMENT '发布作者名称',
  user_id BIGINT NOT NULL COMMENT '发布者用户ID',
  score DECIMAL(3,1) NOT NULL DEFAULT 0.0 COMMENT '评分(满分10)',
  schedule_day TINYINT NOT NULL COMMENT '更新日(0=周一 6=周日)',
  update_time VARCHAR(16) COMMENT '更新时间描述如23:00',
  current_episode VARCHAR(64) COMMENT '当前进度如第58话',
  status VARCHAR(32) NOT NULL DEFAULT 'ONGOING' COMMENT 'ONGOING更新中/COMPLETED已完结/UPCOMING即将上映/AIRING长篇连载',
  is_paid TINYINT NOT NULL DEFAULT 0 COMMENT '是否收费:0免费1收费',
  free_episodes INT NOT NULL DEFAULT 3 COMMENT '免费试看集数',
  price_cents INT NOT NULL DEFAULT 0 COMMENT '付费价格(分)',
  total_episodes INT NOT NULL DEFAULT 0 COMMENT '总集数',
  view_count INT NOT NULL DEFAULT 0 COMMENT '播放量',
  subscribe_count INT NOT NULL DEFAULT 0 COMMENT '追番人数',
  link_url VARCHAR(512) NOT NULL DEFAULT '' COMMENT '番剧链接URL',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_anime_series_no (series_no),
  KEY idx_anime_schedule (schedule_day, is_deleted, status),
  KEY idx_anime_user (user_id),
  KEY idx_anime_hot (subscribe_count DESC, score DESC)
) ENGINE=InnoDB COMMENT='动漫番剧表';

CREATE TABLE IF NOT EXISTS anime_subscriptions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '追番ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  anime_id BIGINT NOT NULL COMMENT '番剧ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_anime_sub (user_id, anime_id),
  KEY idx_anime_sub_anime (anime_id)
) ENGINE=InnoDB COMMENT='追番订阅表';

CREATE TABLE IF NOT EXISTS anime_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '番剧订单ID',
  order_no VARCHAR(64) NOT NULL COMMENT '订单号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  anime_id BIGINT NOT NULL COMMENT '番剧ID',
  anime_title VARCHAR(256) COMMENT '番剧名称快照',
  amount_cents INT NOT NULL COMMENT '支付金额(分)',
  pay_method VARCHAR(20) NOT NULL COMMENT 'ALIPAY/WECHAT',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/EXPIRED/FAILED',
  pay_url VARCHAR(1024) NULL COMMENT '支付链接',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  expired_at DATETIME NOT NULL COMMENT '订单过期时间',
  paid_at DATETIME NULL COMMENT '实际支付时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_anime_order_no (order_no),
  KEY idx_anime_order_user (user_id, status),
  KEY idx_anime_order_anime (anime_id)
) ENGINE=InnoDB COMMENT='番剧支付订单表';

CREATE TABLE IF NOT EXISTS anime_purchases (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '购买记录ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  anime_id BIGINT NOT NULL COMMENT '番剧ID',
  order_no VARCHAR(64) NOT NULL COMMENT '关联订单号',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_anime_purchase (user_id, anime_id),
  KEY idx_anime_purchase_user (user_id)
) ENGINE=InnoDB COMMENT='番剧购买记录表';

-- =========================
-- 热门游戏模块
-- =========================
CREATE TABLE IF NOT EXISTS games (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '游戏ID',
  game_no VARCHAR(64) NOT NULL COMMENT '游戏编号',
  user_id BIGINT NOT NULL COMMENT '发布者ID',
  title VARCHAR(256) NOT NULL COMMENT '游戏名称',
  genre VARCHAR(64) NOT NULL COMMENT '游戏类型',
  description TEXT NULL COMMENT '游戏介绍',
  cover_url VARCHAR(512) NULL COMMENT '封面图URL',
  hero_url VARCHAR(512) NULL COMMENT '横版大图URL',
  game_url VARCHAR(1024) NULL COMMENT '游戏链接/嵌入地址',
  tags VARCHAR(512) NULL COMMENT '标签(JSON数组)',
  developer VARCHAR(128) NULL COMMENT '开发者/作者',
  release_date VARCHAR(32) NULL COMMENT '发布日期',
  rating DECIMAL(3,1) NOT NULL DEFAULT 0.0 COMMENT '评分(满分10)',
  rating_count INT NOT NULL DEFAULT 0 COMMENT '评分人数',
  play_count INT NOT NULL DEFAULT 0 COMMENT '游玩次数',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数',
  is_paid TINYINT NOT NULL DEFAULT 0 COMMENT '是否收费:0免费1收费',
  price_cents INT NOT NULL DEFAULT 0 COMMENT '价格(分)',
  trial_minutes INT NOT NULL DEFAULT 0 COMMENT '试玩时长(分钟,0=无限制)',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1上架0下架2审核中',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_game_no (game_no),
  KEY idx_game_user (user_id),
  KEY idx_game_status_rating (status, is_deleted, rating DESC),
  KEY idx_game_genre (genre, status)
) ENGINE=InnoDB COMMENT='热门游戏表';

CREATE TABLE IF NOT EXISTS game_orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
  order_no VARCHAR(64) NOT NULL COMMENT '订单号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  game_id BIGINT NOT NULL COMMENT '游戏ID',
  game_title VARCHAR(256) NULL COMMENT '游戏名称(冗余)',
  amount_cents INT NOT NULL COMMENT '支付金额(分)',
  pay_method VARCHAR(20) NOT NULL COMMENT '支付方式:ALIPAY/WECHAT',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING/PAID/EXPIRED/FAILED',
  pay_url VARCHAR(1024) NULL COMMENT '支付链接',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  expired_at DATETIME NOT NULL COMMENT '过期时间',
  paid_at DATETIME NULL COMMENT '支付时间',
  UNIQUE KEY uk_game_order_no (order_no),
  KEY idx_go_user_status (user_id, status),
  KEY idx_go_game (game_id),
  KEY idx_go_created (created_at)
) ENGINE=InnoDB COMMENT='游戏购买订单表';

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
  biz_type    VARCHAR(20) NOT NULL DEFAULT 'RECHARGE' COMMENT '业务类型：RECHARGE（充值）/ VIP（会员开通）',
  biz_ref_id  VARCHAR(64) NULL COMMENT '关联业务ID（VIP订单号等）',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  expired_at  DATETIME NOT NULL COMMENT '订单过期时间',
  paid_at     DATETIME NULL COMMENT '实际支付时间',
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_po_user_status (user_id, status),
  KEY idx_po_created (created_at)
) ENGINE=InnoDB COMMENT='充值支付订单';

-- =========================
-- 用户经验等级模块
-- =========================
CREATE TABLE IF NOT EXISTS user_experience (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT NOT NULL UNIQUE COMMENT '用户ID',
  exp_points  INT NOT NULL DEFAULT 0 COMMENT '当前经验值',
  level       INT NOT NULL DEFAULT 1 COMMENT '当前等级',
  level_name  VARCHAR(32) NOT NULL DEFAULT '灵感新手' COMMENT '等级称号',
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY idx_ue_user (user_id)
) ENGINE=InnoDB COMMENT='用户经验等级表';

CREATE TABLE IF NOT EXISTS exp_records (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT NOT NULL COMMENT '用户ID',
  amount      INT NOT NULL COMMENT '经验变动值',
  source      VARCHAR(32) NOT NULL COMMENT '来源：SIGNIN/POST/AI_CHAT/VIP_BUY/INVITE/CREATION/TASK',
  description VARCHAR(255) NULL COMMENT '描述',
  ref_id      VARCHAR(64) NULL COMMENT '关联业务ID',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_er_user_created (user_id, created_at),
  KEY idx_er_source (source, created_at)
) ENGINE=InnoDB COMMENT='经验值流水表';

-- =========================
-- 每日任务模块
-- =========================
CREATE TABLE IF NOT EXISTS daily_tasks (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  task_code     VARCHAR(32) NOT NULL COMMENT '任务编码',
  task_name     VARCHAR(64) NOT NULL COMMENT '任务名称',
  description   VARCHAR(255) NULL COMMENT '任务描述',
  reward_points INT NOT NULL DEFAULT 0 COMMENT '奖励积分',
  reward_exp    INT NOT NULL DEFAULT 0 COMMENT '奖励经验',
  daily_limit   INT NOT NULL DEFAULT 1 COMMENT '每日完成次数上限',
  task_type     VARCHAR(32) NOT NULL DEFAULT 'ACTION' COMMENT '类型：ACTION（需主动完成）/ AUTO（自动检测）',
  route_path    VARCHAR(128) NULL COMMENT '跳转路径（前端路由）',
  sort_order    INT NOT NULL DEFAULT 0 COMMENT '排序',
  status        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_task_code (task_code),
  KEY idx_dt_status_sort (status, sort_order)
) ENGINE=InnoDB COMMENT='每日任务定义表';

CREATE TABLE IF NOT EXISTS user_task_progress (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT NOT NULL COMMENT '用户ID',
  task_code   VARCHAR(32) NOT NULL COMMENT '任务编码',
  task_date   DATE NOT NULL COMMENT '任务日期',
  progress    INT NOT NULL DEFAULT 0 COMMENT '当前完成次数',
  completed   TINYINT NOT NULL DEFAULT 0 COMMENT '是否全部完成',
  rewarded    TINYINT NOT NULL DEFAULT 0 COMMENT '是否已领取奖励',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_user_task_date (user_id, task_code, task_date),
  KEY idx_utp_user_date (user_id, task_date)
) ENGINE=InnoDB COMMENT='用户每日任务进度表';

-- 初始化每日任务
INSERT IGNORE INTO daily_tasks (task_code, task_name, description, reward_points, reward_exp, daily_limit, task_type, route_path, sort_order) VALUES
  ('DAILY_SIGNIN', '每日签到', '完成每日签到', 10, 20, 1, 'ACTION', '/vip', 1),
  ('POST_CONTENT', '发布一条动态', '在发现频道发布内容', 50, 30, 1, 'AUTO', '/explore', 2),
  ('INVITE_USER', '邀请新用户', '邀请新用户注册', 100, 50, 5, 'ACTION', '/about', 3),
  ('USE_AI_DRAW', '使用 AI 绘图', '使用AI助手进行创作', 20, 15, 3, 'AUTO', '/ai', 4),
  ('FORUM_POST', '发布论坛帖子', '在论坛发布帖子', 30, 25, 1, 'AUTO', '/forum', 5),
  ('AI_CHAT', '与AI对话', '使用AI助手进行对话', 10, 10, 5, 'AUTO', '/ai', 6);
