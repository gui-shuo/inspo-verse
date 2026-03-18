$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptDir "..")
$deployDir = Resolve-Path (Join-Path $repoRoot "deploy")
$envFile = Join-Path $deployDir ".env"
$envExampleFile = Join-Path $deployDir ".env.example"

Write-Host "[1/5] Checking Docker CLI..."
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker command not found. Please install Docker Desktop first."
    exit 1
}

Write-Host "[2/5] Checking Docker engine status..."
docker info 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Error @"
Docker engine is not ready, so one-click startup cannot continue.
Please start Docker Desktop and wait until it is Running, then try again.

If you see this error:
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
It usually means Docker Desktop is not started yet, or it has not switched to the Linux container engine.
"@
    exit 1
}

Write-Host "[3/5] Checking deploy/.env..."
if (-not (Test-Path $envFile)) {
    Copy-Item $envExampleFile $envFile
    Write-Warning "deploy/.env was not found. It has been created from .env.example."
    Write-Warning "Please review sensitive configuration values before restarting."
}

Write-Host "[4/5] Building frontend static assets..."
Set-Location $repoRoot
try {
    npm --prefix frontend install
} catch {
    Write-Error "Failed to install frontend dependencies. Please check Node.js and npm."
    exit 1
}

try {
    npm --prefix frontend run build
} catch {
    Write-Error "Frontend build failed. Please check Node.js version and dependency installation."
    exit 1
}

Write-Host "[5/5] Starting Docker Compose services..."
Set-Location $deployDir
docker compose up -d --build

Write-Host "One-click startup completed."
