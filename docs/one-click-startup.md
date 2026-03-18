# Inspo-Verse 手动启动说明（Docker Compose）

本文档用于解决并规避以下常见启动失败：

```text
unable to get image 'nginx:1.27-alpine': error during connect:
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
```

## 1. 问题原因分析

该报错不是 `docker-compose.yml` 语法问题，而是 **Docker 引擎未启动/不可用** 导致：

- Windows 下 `//./pipe/dockerDesktopLinuxEngine` 是 Docker Desktop 的 Linux 引擎管道；
- 当 Docker Desktop 没有启动，或未切换到 Linux 容器模式时，`docker compose` 无法连接引擎；
- 因为无法连接引擎，拉取 `nginx:1.27-alpine` 等镜像会直接失败。

## 2. 手动启动前需要了解

由于 `deploy/docker-compose.yml` 通过卷挂载使用 `frontend/dist`，启动前请先完成：

1. Docker 引擎可连接（`docker info` 正常）；
2. `deploy/.env` 已创建（可由 `.env.example` 复制）；
3. 前端静态资源已构建（`frontend/dist` 存在）。

## 3. 手动启动命令（终端输入）

在仓库根目录执行：

```bash
# 1) 检查 Docker 引擎
docker info

# 2) 安装并构建前端静态资源
npm --prefix ./frontend install
npm --prefix ./frontend run build

# 3) 首次创建 deploy/.env
cp ./deploy/.env.example ./deploy/.env

# 4) 启动容器
cd deploy
docker compose up -d --build
```

## 4. 启动前环境要求

- Node.js 16+（建议与前端工程保持一致）
- Docker Desktop（Windows/macOS）或 Docker Engine + Docker Compose（Linux）
- 可访问 Docker Hub（用于拉取基础镜像）

## 5. 启动成功后的验证

1. 查看容器状态：

   ```bash
   cd deploy
   docker compose ps
   ```

2. 打开前台页面：`http://localhost`

3. 可选健康检查：
   - Java API: `http://localhost/api/v1/public/ping`
   - Python AI: `http://localhost/ai/healthz`

## 6. 常见失败与处理

### 6.1 Docker 管道错误（本次问题）

错误特征：

```text
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
```

处理步骤：

1. 启动 Docker Desktop；
2. 等待状态显示 `Running`；
3. 确认已使用 Linux 容器引擎；
4. 重新执行手动启动命令。

### 6.2 `deploy/.env` 缺失或变量为空

可先手动创建 `.env`，再按需修改：

- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- `AI_INTERNAL_SIGN_KEY`
- `OPENAI_API_KEY`

### 6.3 镜像拉取超时（网络问题）

错误特征常见为 `TLS handshake timeout`。

处理建议：

- 重试 `docker compose up -d --build`；
- 检查网络质量与代理设置；
- 确认 Docker Hub 访问正常。

## 7. 停止与重启

停止：

```bash
cd deploy
docker compose down
```

重新启动：

```bash
docker compose up -d --build
```
