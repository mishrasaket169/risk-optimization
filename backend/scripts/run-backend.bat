@echo off
setlocal enabledelayedexpansion

echo ================================================================================
echo    LIQUIDITY RISK OPTIMIZER (LRO) - BACKEND APPLICATION LAUNCHER
echo    Windows Environment | Java 25 LTS | In-Memory / PostgreSQL Persistence
echo ================================================================================

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java 25 is not found in PATH. Please install Java 25 and set JAVA_HOME.
    exit /b 1
)

echo [INFO] Building and starting LRO backend foundation...
call mvn -f "%~dp0..\pom.xml" compile -q
if errorlevel 1 (
    echo [ERROR] Maven compilation failed.
    exit /b 1
)

echo [INFO] Launching com.lro.app.BackendLauncher...
call mvn -f "%~dp0..\lro-app\pom.xml" exec:java -Dexec.mainClass="com.lro.app.BackendLauncher"

pause
