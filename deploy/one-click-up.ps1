$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")
$deployDir = Resolve-Path (Join-Path $repoRoot "deploy")
$envFile = Join-Path $deployDir ".env"
$envExampleFile = Join-Path $deployDir ".env.example"

Write-Host "[1/5] 检查 Docker CLI..."
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "未检测到 docker 命令，请先安装 Docker Desktop。"
    exit 1
}

Write-Host "[2/5] 检查 Docker 引擎状态..."
docker info 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Error @"
Docker 引擎未就绪，无法执行一键启动。
请先启动 Docker Desktop 并等待状态为 Running 后重试。

若出现以下错误：
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
通常表示 Docker Desktop 尚未启动，或尚未切换到 Linux 容器引擎。
"@
    exit 1
}

Write-Host "[3/5] 检查 deploy/.env..."
if (-not (Test-Path $envFile)) {
    Copy-Item $envExampleFile $envFile
    Write-Warning "未找到 deploy/.env，已从 .env.example 自动创建。"
    Write-Warning "请按需修改敏感配置后再次启动。"
}

Write-Host "[4/5] 构建前端静态资源..."
Set-Location $repoRoot
npm --prefix frontend install
npm --prefix frontend run build

Write-Host "[5/5] 启动 Docker Compose 服务栈..."
Set-Location $deployDir
docker compose up -d --build

Write-Host "✅ 一键启动完成。"
