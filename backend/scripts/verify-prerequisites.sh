#!/bin/bash
# System Prerequisites Verification Script for Fedora Linux / POSIX

echo "================================================================================"
echo "   LRO ECOSYSTEM - SYSTEM PREREQUISITES VERIFICATION (FEDORA LINUX)"
echo "================================================================================"

ALL_OK=1

# 1. Java 25
echo "[1/4] Checking Java 25 LTS..."
if command -v java &> /dev/null; then
    JAVA_VER=$(java -version 2>&1 | head -n 1)
    echo "      Found: $JAVA_VER"
else
    echo "      [FAIL] 'java' not found. Install with: sudo dnf install java-25-openjdk-devel"
    ALL_OK=0
fi

# 2. Maven
echo "[2/4] Checking Apache Maven..."
if command -v mvn &> /dev/null; then
    MVN_VER=$(mvn -version 2>&1 | head -n 1)
    echo "      Found: $MVN_VER"
else
    echo "      [FAIL] 'mvn' not found. Install with: sudo dnf install maven"
    ALL_OK=0
fi

# 3. Python 3
echo "[3/4] Checking Python 3..."
if command -v python3 &> /dev/null; then
    PY_VER=$(python3 --version 2>&1)
    echo "      Found: $PY_VER"
else
    echo "      [FAIL] 'python3' not found. Install with: sudo dnf install python3"
    ALL_OK=0
fi

# 4. PostgreSQL client
echo "[4/4] Checking PostgreSQL client (psql)..."
if command -v psql &> /dev/null; then
    echo "      Found: $(psql --version)"
else
    echo "      [INFO] 'psql' not found. Embedded in-memory DB topology will be used."
fi

echo "--------------------------------------------------------------------------------"
if [ "$ALL_OK" -eq 1 ]; then
    echo "[RESULT] All essential prerequisites (Java, Maven, Python) are satisfied!"
else
    echo "[RESULT] One or more essential prerequisites are missing. Please install them."
fi
echo "================================================================================"
