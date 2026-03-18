CREATE DATABASE IF NOT EXISTS inspo_verse
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE inspo_verse;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL COMMENT '登录用户名',
  email VARCHAR(128) NULL COMMENT '邮箱',
  phone VARCHAR(32) NULL COMMENT '手机号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  nickname VARCHAR(64) NOT NULL COMMENT '昵称',
  avatar_url VARCHAR(512) NULL COMMENT '头像URL',
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
  UNIQUE KEY uk_roles_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
  perm_code VARCHAR(128) NOT NULL COMMENT '权限编码',
  perm_name VARCHAR(128) NOT NULL COMMENT '权限名称',
  resource_type VARCHAR(32) NOT NULL COMMENT '资源类型:MENU/API/BUTTON',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_permissions_code (perm_code)
) ENGINE=InnoDB COMMENT='权限点表';

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
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态:0待支付1成功2失败3退款',
  paid_at DATETIME NULL COMMENT '支付完成时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_vip_order_no (order_no),
  KEY idx_vip_orders_user_status (user_id, pay_status, created_at)
) ENGINE=InnoDB COMMENT='VIP订单表';

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
  tokens INT NOT NULL DEFAULT 0 COMMENT 'token数量',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  KEY idx_ai_msg_session_created (session_id, created_at),
  KEY idx_ai_msg_user_created (user_id, created_at)
) ENGINE=InnoDB COMMENT='AI消息明细表';

CREATE TABLE IF NOT EXISTS workshop_project (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工坊项目ID',
  project_no VARCHAR(64) NOT NULL COMMENT '项目编号',
  user_id BIGINT NOT NULL COMMENT '创建用户ID',
  title VARCHAR(128) NOT NULL COMMENT '项目标题',
  description VARCHAR(1024) NULL COMMENT '项目描述',
  prompt_text TEXT NULL COMMENT '生成提示词',
  result_content MEDIUMTEXT NULL COMMENT '生成结果',
  visibility TINYINT NOT NULL DEFAULT 1 COMMENT '可见性:1公开2私有',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_workshop_project_no (project_no),
  KEY idx_workshop_user_created (user_id, created_at),
  KEY idx_workshop_visibility_hot (visibility, like_count, favorite_count)
) ENGINE=InnoDB COMMENT='创意工坊项目表';

CREATE TABLE IF NOT EXISTS forum_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
  post_no VARCHAR(64) NOT NULL COMMENT '帖子编号',
  user_id BIGINT NOT NULL COMMENT '发帖人ID',
  category VARCHAR(32) NOT NULL COMMENT '分类:anime/game/chat/share',
  title VARCHAR(256) NOT NULL COMMENT '标题',
  content MEDIUMTEXT NOT NULL COMMENT '正文内容',
  view_count INT NOT NULL DEFAULT 0 COMMENT '浏览数',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  comment_count INT NOT NULL DEFAULT 0 COMMENT '评论数',
  is_top TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常0下架2审核中',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_forum_post_no (post_no),
  KEY idx_forum_post_category_created (category, status, created_at),
  KEY idx_forum_post_hot (status, is_top, like_count, comment_count)
) ENGINE=InnoDB COMMENT='论坛帖子表';

CREATE TABLE IF NOT EXISTS forum_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  post_id BIGINT NOT NULL COMMENT '帖子ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  parent_comment_id BIGINT NOT NULL DEFAULT 0 COMMENT '父评论ID(0为根评论)',
  content TEXT NOT NULL COMMENT '评论内容',
  like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1正常0删除2审核中',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  KEY idx_forum_comment_post_created (post_id, status, created_at),
  KEY idx_forum_comment_parent (parent_comment_id, created_at)
) ENGINE=InnoDB COMMENT='论坛评论表';

CREATE TABLE IF NOT EXISTS forum_interaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '互动ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  target_type VARCHAR(16) NOT NULL COMMENT '目标类型:post/comment/project',
  target_id BIGINT NOT NULL COMMENT '目标ID',
  action_type VARCHAR(16) NOT NULL COMMENT '行为:like/favorite/report',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_forum_interaction (user_id, target_type, target_id, action_type),
  KEY idx_forum_interaction_target (target_type, target_id, action_type)
) ENGINE=InnoDB COMMENT='社区互动行为表';

CREATE TABLE IF NOT EXISTS sys_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  config_key VARCHAR(128) NOT NULL COMMENT '配置键',
  config_value TEXT NOT NULL COMMENT '配置值',
  value_type VARCHAR(32) NOT NULL DEFAULT 'string' COMMENT '值类型',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB COMMENT='系统配置表';
