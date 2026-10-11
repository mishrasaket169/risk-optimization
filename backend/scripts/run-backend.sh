#!/bin/bash
set -e

echo "================================================================================"
echo "   LIQUIDITY RISK OPTIMIZER (LRO) - BACKEND APPLICATION LAUNCHER"
echo "   Fedora Linux Environment | OpenJDK 25 | In-Memory / PostgreSQL Persistence"
echo "================================================================================"

if ! command -v java &> /dev/null; then
    echo "[ERROR] OpenJDK 25 is not found in PATH. Please install Java 25 (e.g. dnf install java-25-openjdk-devel)."
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_ROOT="$(dirname "$SCRIPT_DIR")"

echo "[INFO] Compiling backend multi-module reactor..."
mvn -f "$BACKEND_ROOT/pom.xml" compile -q

echo "[INFO] Launching com.lro.app.BackendLauncher..."
mvn -f "$BACKEND_ROOT/lro-app/pom.xml" exec:java -Dexec.mainClass="com.lro.app.BackendLauncher"
