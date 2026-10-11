# PowerShell Ecosystem Orchestration Script (Prompt 28)
# Launches ML inference server, runs backend initialization, and launches JavaFX desktop interface

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "   LIQUIDITY RISK OPTIMIZER (LRO) - FULL ECOSYSTEM ORCHESTRATOR" -ForegroundColor Cyan
Write-Host "   Windows 11 | Java 25 LTS | Python 3.14 | JavaFX 23.0.1 | PostgreSQL Topology" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan

$WorkspaceRoot = (Get-Item $PSScriptRoot).Parent.Parent.FullName
$ModelDir = Join-Path $WorkspaceRoot "model"
$BackendDir = Join-Path $WorkspaceRoot "backend"
$FrontendDir = Join-Path $WorkspaceRoot "frontend"

# 1. Start ML Inference Server
Write-Host "[1/3] Starting Python ML inference microservice on http://127.0.0.1:8082..." -ForegroundColor Yellow
$PythonProc = Start-Process python -ArgumentList "$ModelDir\inference_server.py" -PassThru -WindowStyle Hidden
Start-Sleep -Seconds 2

# Verify ML health endpoint
try {
    $Health = Invoke-RestMethod -Uri "http://127.0.0.1:8082/health" -TimeoutSec 3
    Write-Host "      ML Service Online: $($Health.status) (Model 1: $($Health.models.model_1), Model 2: $($Health.models.model_2))" -ForegroundColor Green
} catch {
    Write-Host "      [WARNING] ML microservice not answering on port 8082; backend will use deterministic fallback." -ForegroundColor DarkYellow
}

# 2. Run Backend Initialization & Schema Migrations
Write-Host "[2/3] Initializing backend engines, Flyway migrations, and synthetic populations..." -ForegroundColor Yellow
Start-Process -FilePath "mvn" -ArgumentList "-f `"$BackendDir\lro-app\pom.xml`" exec:java -Dexec.mainClass=`"com.lro.app.BackendLauncher`"" -Wait -NoNewWindow
Write-Host "      Backend engines initialized successfully." -ForegroundColor Green

# 3. Launch Frontend JavaFX Application
Write-Host "[3/3] Launching JavaFX Desktop Application (Bank, Exchange, and LRO views)..." -ForegroundColor Yellow
Set-Location -Path $FrontendDir
mvn javafx:run

# Cleanup ML process on exit
if ($PythonProc -and -not $PythonProc.HasExited) {
    Stop-Process -Id $PythonProc.Id -Force
}
Write-Host "Ecosystem session concluded." -ForegroundColor Cyan
