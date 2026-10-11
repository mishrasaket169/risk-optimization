@echo off
setlocal

echo ================================================================================
echo    LRO ECOSYSTEM - SYSTEM PREREQUISITES VERIFICATION (WINDOWS)
echo ================================================================================

set ALL_OK=1

:: Check Java
echo [1/4] Checking Java 25 LTS...
where java >nul 2>&1
if errorlevel 1 (
    echo       [FAIL] 'java' not found in PATH.
    set ALL_OK=0
) else (
    for /f "tokens=*" %%v in ('java -version 2^>^&1') do (
        echo       Found: %%v
        goto :after_java
    )
)
:after_java

:: Check Maven
echo [2/4] Checking Apache Maven...
where mvn >nul 2>&1
if errorlevel 1 (
    echo       [FAIL] 'mvn' not found in PATH.
    set ALL_OK=0
) else (
    for /f "tokens=*" %%v in ('mvn -version 2^>^&1') do (
        echo       Found: %%v
        goto :after_mvn
    )
)
:after_mvn

:: Check Python
echo [3/4] Checking Python 3...
where python >nul 2>&1
if errorlevel 1 (
    echo       [FAIL] 'python' not found in PATH.
    set ALL_OK=0
) else (
    for /f "tokens=*" %%v in ('python --version 2^>^&1') do (
        echo       Found: %%v
        goto :after_py
    )
)
:after_py

:: Check PostgreSQL (Optional for production / H2 fallback built-in)
echo [4/4] Checking PostgreSQL Client (psql)...
where psql >nul 2>&1
if errorlevel 1 (
    echo       [INFO] 'psql' not found in PATH. In-memory / embedded DB fallback active.
) else (
    echo       [PASS] psql available.
)

echo --------------------------------------------------------------------------------
if "%ALL_OK%"=="1" (
    echo [RESULT] All essential prerequisites (Java, Maven, Python) are satisfied!
) else (
    echo [RESULT] One or more essential prerequisites are missing. Please install them.
)
echo ================================================================================

pause
