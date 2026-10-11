#!/bin/bash
# Bash Ecosystem Orchestration Script (Prompt 28)
# Launches ML inference server, runs backend initialization, and launches JavaFX desktop interface on Fedora Linux

set -e

echo "================================================================================"
echo "   LIQUIDITY RISK OPTIMIZER (LRO) - FULL ECOSYSTEM ORCHESTRATOR"
echo "   Fedora Linux | OpenJDK 25 | Python 3 | JavaFX 23.0.1 | Multi-Schema DB"
echo "================================================================================"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(dirname "$SCRIPT_DIR")"
WORKSPACE_ROOT="$(dirname "$BACKEND_DIR")"
MODEL_DIR="$WORKSPACE_ROOT/model"
FRONTEND_DIR="$WORKSPACE_ROOT/frontend"

# 1. Start Python ML Server in background
echo "[1/3] Starting Python ML inference microservice on http://127.0.0.1:8082..."
python3 "$MODEL_DIR/inference_server.py" &
PY_PID=$!
sleep 2

# Trap to kill Python background job upon exit
trap "kill -9 $PY_PID 2>/dev/null || true" EXIT

# Check health
if curl -s -f http://127.0.0.1:8082/health > /dev/null; then
    echo "      ML Service Online (Model 1 & Model 2 active)"
else
    echo "      [WARNING] ML microservice offline; backend will use deterministic fallback."
fi

# 2. Run Backend Launcher
echo "[2/3] Initializing backend engines, Flyway migrations, and synthetic populations..."
mvn -f "$BACKEND_DIR/lro-app/pom.xml" exec:java -Dexec.mainClass="com.lro.app.BackendLauncher"

# 3. Launch Frontend JavaFX Application
echo "[3/3] Launching JavaFX Desktop Application..."
cd "$FRONTEND_DIR"
mvn javafx:run
