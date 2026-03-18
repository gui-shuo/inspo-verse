#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEPLOY_DIR="${REPO_ROOT}/deploy"
ENV_FILE="${DEPLOY_DIR}/.env"
ENV_EXAMPLE_FILE="${DEPLOY_DIR}/.env.example"

echo "[1/5] 检查 Docker CLI..."
if ! command -v docker >/dev/null 2>&1; then
  echo "❌ 未检测到 docker 命令，请先安装 Docker Desktop / Docker Engine。"
  exit 1
fi

echo "[2/5] 检查 Docker 引擎状态..."
if ! docker info >/dev/null 2>&1; then
  cat <<'EOF'
❌ Docker 引擎未就绪，无法执行一键启动。
请先启动 Docker Desktop（Windows/macOS）或 Docker 服务（Linux），然后重试。

Windows 常见报错：
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
这通常表示 Docker Desktop 尚未启动或尚未切换到 Linux 容器引擎。
EOF
  exit 1
fi

echo "[3/5] 检查 deploy/.env..."
if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${ENV_EXAMPLE_FILE}" "${ENV_FILE}"
  echo "⚠️  未找到 deploy/.env，已从 .env.example 自动创建。"
  echo "⚠️  请按需修改敏感配置后再次启动。"
fi

echo "[4/5] 构建前端静态资源..."
if ! npm --prefix "${REPO_ROOT}/frontend" install; then
  echo "❌ 前端依赖安装失败，请检查 Node.js 与 npm 环境。"
  exit 1
fi
if ! npm --prefix "${REPO_ROOT}/frontend" run build; then
  echo "❌ 前端构建失败，请检查 Node.js 版本与依赖安装情况。"
  exit 1
fi

echo "[5/5] 启动 Docker Compose 服务栈..."
cd "${DEPLOY_DIR}"
docker compose up -d --build

echo "✅ 一键启动完成。"
