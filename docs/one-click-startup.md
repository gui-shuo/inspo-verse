# Inspo-Verse 一键启动说明（Docker Compose）

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

## 2. 已做修复（仓库内）

为了避免“直接 compose 启动但报错难定位”，仓库新增了启动脚本：

- `deploy/one-click-up.ps1`（Windows PowerShell）
- `deploy/one-click-up.sh`（Linux/macOS Bash）

脚本会按顺序执行：

1. 检查 `docker` 命令是否存在；
2. 检查 Docker 引擎是否可连接（`docker info`）；
3. 缺失 `deploy/.env` 时自动从 `.env.example` 生成；
4. 自动安装前端依赖并构建 `frontend/dist`；
5. 执行 `docker compose up -d --build`。

当 Docker 引擎不可用时，会输出明确的修复提示，不再直接抛出难理解的连接错误。

## 3. 一键启动（推荐）

### Windows（PowerShell）

在仓库根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\deploy\one-click-up.ps1
```

> 首次可能因为执行策略拦截，使用 `-ExecutionPolicy Bypass` 可临时放行当前命令。

### Linux / macOS

在仓库根目录执行：

```bash
bash ./deploy/one-click-up.sh
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
4. 重新执行一键脚本。

### 6.2 `deploy/.env` 缺失或变量为空

脚本会自动创建 `.env`，但你仍需按需修改：

- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- `AI_INTERNAL_SIGN_KEY`
- `OPENAI_API_KEY`

### 6.3 镜像拉取超时（网络问题）

错误特征常见为 `TLS handshake timeout`。

处理建议：

- 重试一键脚本；
- 检查网络质量与代理设置；
- 确认 Docker Hub 访问正常。

## 7. 停止与重启

停止：

```bash
cd deploy
docker compose down
```

重启：

```bash
# Windows
powershell -ExecutionPolicy Bypass -File .\deploy\one-click-up.ps1

# Linux/macOS
bash ./deploy/one-click-up.sh
```
