# Inspo-verse 企业级全栈落地方案（生产可部署）

> 适用范围：当前 `Vue3 + TypeScript` 前端页面（Home / Explore / AIChat / Workshop / Anime / Games / Forum / Login / UserCenter / VipCenter / Admin / CommunityRules / Contact）对应的后端与数据库落地设计。

## 1. 生产级全栈架构与请求链路设计

### 1.1 总体架构（逻辑分层）

```text
[Browser(Vue3 SPA)]
   │ HTTPS + JWT
   ▼
[Nginx/Ingress]
   ├─ /              -> Vue静态资源
   ├─ /api/**        -> Spring Cloud Gateway
   └─ /ai-stream/**  -> Python AI Service (SSE/WebSocket)

[Spring Cloud Gateway]
   ├─ auth-service (登录/注册/令牌刷新)
   ├─ user-service (个人中心/资料)
   ├─ vip-service (会员权益/订单)
   ├─ forum-service (帖子/评论/互动)
   ├─ content-service (首页/探索/动漫/游戏内容聚合)
   ├─ workshop-service (创意工坊作品管理)
   └─ admin-service (后台审核/运营配置)

[Python AI Cluster(FastAPI)]
   ├─ ai-chat-service (LLM流式对话)
   ├─ ai-agent-service (创意生成/多工具智能体)
   └─ ai-recommend-service (向量召回/排序推荐)

Infra:
  MySQL(主从) + Redis(缓存/会话/限流) + Kafka(异步事件总线) + MinIO(媒体资源) + Prometheus/Grafana + ELK
```

### 1.2 端到端请求链路（闭环）

1. Vue 页面通过 `axios` 调用 `/api/**`，携带 `Authorization: Bearer <JWT>`。  
2. Nginx 统一 TLS 终止并转发至 Gateway。  
3. Gateway 执行：限流、CORS、JWT 初验、灰度路由、Trace 注入。  
4. Java 微服务处理核心业务（权限、论坛、VIP、订单、后台管理等），读写 MySQL，热数据走 Redis。  
5. AI 场景（`AIChatView` / `WorkshopView`）：
   - 同步元数据请求：经 Gateway 到 Java（会话鉴权、额度校验、消息落库）。
   - 流式生成请求：Nginx 路由 `/ai-stream/**` 到 Python FastAPI（SSE/WebSocket），Python 校验内部签名后调用大模型并增量返回。
6. Java 与 Python 通过 Kafka/REST 做异步协同：
   - Java 发 `ai.generate.request` 事件（含用户、场景、限额、审计 ID）。
   - Python 消费后生成结果并回写 `ai.generate.result`；Java 消费后更新数据库状态并通知前端。

### 1.3 Java 与 Python 职责边界

#### Java（Spring Boot / Spring Cloud）负责
- 统一身份认证、RBAC 权限、菜单权限、后台管理；
- VIP 订阅、订单支付状态机、权益校验；
- 论坛帖子/评论/点赞/收藏等高并发事务型业务；
- 内容聚合、审核、运营配置、数据统计接口；
- 对外 API 统一协议、审计日志、风控限流。

#### Python（FastAPI / LangChain）负责
- 大模型对话（流式 token 输出）；
- 创意工坊生成（文案/提示词/标签/灵感卡片）；
- 推荐与召回（向量检索 + 规则重排）；
- 智能体编排（多步工具调用、函数调用）。

#### Java ↔ Python 通信建议
- **实时场景**：REST + 内网 mTLS（超时、重试、熔断）；
- **异步高吞吐**：Kafka（解耦峰值流量，失败可重放）；
- **契约治理**：OpenAPI + JSON Schema + 版本号（`v1`/`v2`）。

---

## 2. 数据库高可用设计（MySQL DDL）

> 规范：`utf8mb4`、`InnoDB`、统一 `created_at/updated_at/is_deleted`、关键业务表含唯一约束与高频查询索引。  
> 高可用：主从复制 + ProxySQL/VIP；读写分离；按 `user_id`/`created_at` 分库分表预留。

```sql
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

CREATE TABLE IF NOT EXISTS permissions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
  perm_code VARCHAR(128) NOT NULL COMMENT '权限编码',
  perm_name VARCHAR(128) NOT NULL COMMENT '权限名称',
  resource_type VARCHAR(32) NOT NULL COMMENT '资源类型:MENU/API/BUTTON',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  UNIQUE KEY uk_permissions_code (perm_code),
  KEY idx_permissions_type (resource_type)
) ENGINE=InnoDB COMMENT='权限点表';

CREATE TABLE IF NOT EXISTS user_role_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_urr_role (role_id)
) ENGINE=InnoDB COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS role_permission_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY uk_role_perm (role_id, permission_id),
  KEY idx_rpr_perm (permission_id)
) ENGINE=InnoDB COMMENT='角色权限关系';

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
```

### 2.1 索引与高并发说明（重点）
- 所有高频列表接口均采用“**过滤列 + 排序列**”联合索引，例如帖子列表 `(category, status, created_at)`。  
- 互动去重采用唯一索引 `(user_id, target_type, target_id, action_type)` 防止重复点赞。  
- 会话与消息按 `(session_id, created_at)` 支持 AI 聊天增量拉取。  
- 热点计数（浏览/点赞/评论）建议 Redis 计数 + 定时回写 MySQL，降低写放大。  

---

## 3. 前后端交互闭环与 API 规范

### 3.1 全局统一响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "d2e5d4f6-6f3f-4ca6-8f4d-2dc3f6f1f111",
  "timestamp": 1710748800000
}
```

#### 状态码建议
- `0`：成功  
- `40001`：参数错误  
- `40100`：未登录/Token 无效  
- `40300`：无权限  
- `40400`：资源不存在  
- `40900`：业务冲突（重复提交/状态不允许）  
- `42900`：请求过多（限流）  
- `50000`：系统内部错误  
- `50300`：下游服务不可用（如 LLM 服务超时）

### 3.2 API 路径与版本规范
- 前缀：`/api/v1/**`（Java 业务）  
- AI 流式：`/ai-stream/v1/**`（Python）  
- 示例：
  - `POST /api/v1/auth/login`
  - `GET /api/v1/users/me`
  - `POST /api/v1/forum/posts`
  - `GET /api/v1/vip/plans`
  - `POST /ai-stream/v1/chat/completions`（SSE）

### 3.3 跨域与鉴权方案（JWT）

#### Vue 侧
- 登录后将 `accessToken` 存储于内存 + `refreshToken` 存储 HttpOnly Cookie（推荐）；
- `axios` 请求拦截器统一注入 `Authorization`；
- 响应拦截器遇到 `40100` 自动触发刷新令牌并重试一次。

#### Gateway / Java 侧
- Gateway 统一校验 JWT（签名、过期、黑名单）；
- 透传用户声明（`x-user-id`, `x-roles`, `x-trace-id`）到下游服务；
- Java 服务使用 `@PreAuthorize` + RBAC 进行细粒度权限控制。

#### Python 侧
- 仅允许内网调用，或要求 Gateway 转发的短期签名头：`x-internal-sign`；
- 校验签名 + 时间戳窗口（防重放）；
- 可选二次校验 JWT `sub/user_id` 与会话归属。

### 3.4 AIChatView 流式交互（SSE 方案）

#### Python（FastAPI）伪代码
```python
@app.post("/ai-stream/v1/chat/completions")
async def chat_stream(req: ChatReq, user=Depends(verify_token)):
    async def event_gen():
        yield "event: start\ndata: {\"sessionId\":\"...\"}\n\n"
        async for chunk in llm.stream(req.messages):
            yield f"event: token\ndata: {json.dumps({'delta': chunk})}\n\n"
        yield "event: end\ndata: {\"finishReason\":\"stop\"}\n\n"
    return StreamingResponse(event_gen(), media_type="text/event-stream")
```

#### Vue（AIChatView）伪代码
```ts
const es = new EventSource(`/ai-stream/v1/chat/completions?sessionId=${id}`, { withCredentials: true })
es.addEventListener('token', (e) => {
  const payload = JSON.parse((e as MessageEvent).data)
  currentAssistantMessage.value += payload.delta
})
es.addEventListener('end', () => {
  es.close()
  // 调用 /api/v1/ai/messages/persist 确认落库（或由后端异步自动落库）
})
es.onerror = () => {
  es.close()
  showToast('连接中断，请重试')
}
```

> 若需双向控制（中途停止生成、心跳、重连恢复）可升级到 WebSocket。

---

## 4. 企业级部署与上线方案（DevOps）

### 4.1 Docker 容器化策略（Compose 大纲）

```yaml
version: "3.9"
services:
  nginx:
    image: nginx:1.27
    ports: ["80:80", "443:443"]
    volumes:
      - ./deploy/nginx/conf.d:/etc/nginx/conf.d
      - ./frontend/dist:/usr/share/nginx/html
      - ./deploy/certs:/etc/nginx/certs
    depends_on: [gateway, ai-service]

  gateway:
    image: inspo/gateway:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JWT_PUBLIC_KEY_PATH=/run/secrets/jwt_pub
    depends_on: [redis, mysql]

  user-service:
    image: inspo/user-service:latest
    depends_on: [mysql, redis]

  forum-service:
    image: inspo/forum-service:latest
    depends_on: [mysql, redis, kafka]

  vip-service:
    image: inspo/vip-service:latest
    depends_on: [mysql, redis]

  ai-service:
    image: inspo/ai-service:latest
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - REDIS_URL=redis://redis:6379/1
    depends_on: [redis, kafka]

  mysql:
    image: mysql:8.4
    command: ["--default-authentication-plugin=mysql_native_password"]
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
      - MYSQL_DATABASE=inspo_verse
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7.4
    command: ["redis-server", "--appendonly", "yes"]
    volumes:
      - redis_data:/data

  kafka:
    image: bitnami/kafka:3.8
    environment:
      - KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true

volumes:
  mysql_data:
  redis_data:
```

### 4.2 Nginx 配置要点

```nginx
server {
  listen 443 ssl http2;
  server_name inspo-verse.com;

  ssl_certificate     /etc/nginx/certs/fullchain.pem;
  ssl_certificate_key /etc/nginx/certs/privkey.pem;

  # Vue 静态资源
  root /usr/share/nginx/html;
  index index.html;
  location / {
    try_files $uri $uri/ /index.html;
  }

  # Java API 反向代理
  location /api/ {
    proxy_pass http://gateway:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Request-Id $request_id;
  }

  # Python 流式接口
  location /ai-stream/ {
    proxy_pass http://ai-service:8000/ai-stream/;
    proxy_http_version 1.1;
    proxy_set_header Connection '';
    proxy_buffering off; # SSE关键配置
    chunked_transfer_encoding on;
    proxy_read_timeout 3600s;
  }
}
```

### 4.3 生产环境上线必做清单

1. **安全**
   - 全站 HTTPS + HSTS；
   - JWT 私钥、DB 密码、LLM API Key 使用 K8s Secret / Vault；
   - 登录与 AI 接口限流（Redis + Lua，按 IP + user_id 双维度）。
2. **稳定性**
   - 服务健康检查（`/actuator/health`、`/healthz`）；
   - 熔断重试（Resilience4j）与超时治理；
   - 关键链路压测（登录、发帖、AI 对话并发）。
3. **可观测**
   - 日志：结构化 JSON + TraceId；
   - 指标：QPS、P95/P99、错误率、Token 消耗；
   - 告警：CPU/内存、慢 SQL、5xx 突增、消息堆积。
4. **数据**
   - MySQL 主从 + 定期备份 + 恢复演练；
   - Redis 持久化与主从哨兵；
   - 大表归档策略（聊天消息按月分区/归档）。

---

## 5. 与前端页面模块的接口映射（摘要）

- `HomeView/ExploreView`：`/api/v1/content/*`（推荐流、分类、banner）  
- `AIChatView`：`/ai-stream/v1/chat/completions` + `/api/v1/ai/sessions|messages`  
- `WorkshopView`：`/api/v1/workshop/projects` + `ai-agent` 生成接口  
- `AnimeView/GamesView`：`/api/v1/content/anime|games`  
- `ForumView`：`/api/v1/forum/posts|comments|interactions`  
- `LoginView`：`/api/v1/auth/login|register|refresh`  
- `UserCenterView`：`/api/v1/users/me|security|orders`  
- `VipCenterView`：`/api/v1/vip/plans|orders|membership`  
- `admin/*`：`/api/v1/admin/*`（权限校验 + 审计日志）  
- `CommunityRulesView/ContactView`：`/api/v1/public/rules|contact`

该映射确保“页面 → API → 服务 → 数据表”形成可落地的完整闭环。
