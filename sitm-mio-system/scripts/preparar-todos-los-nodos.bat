@echo off
REM Script para preparar todos los nodos remotos
REM Ejecuta todos los scripts de preparación en secuencia

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0

echo ==========================================
echo Preparando Todos los Nodos Remotos
echo ==========================================
echo.

echo Preparando Master (192.168.131.121)...
call "%SCRIPT_DIR%preparar-master-192.168.131.121.bat"
if errorlevel 1 (
    echo ERROR: Fallo al preparar Master
    pause
    exit /b 1
)
echo.

echo Preparando Worker 1 (192.168.131.115)...
call "%SCRIPT_DIR%preparar-worker-192.168.131.115.bat"
if errorlevel 1 (
    echo ERROR: Fallo al preparar Worker 1
    pause
    exit /b 1
)
echo.

echo Preparando Worker 2 (192.168.131.117)...
call "%SCRIPT_DIR%preparar-worker-192.168.131.117.bat"
if errorlevel 1 (
    echo ERROR: Fallo al preparar Worker 2
    pause
    exit /b 1
)
echo.

echo Preparando Worker 3 (192.168.131.118)...
call "%SCRIPT_DIR%preparar-worker-192.168.131.118.bat"
if errorlevel 1 (
    echo ERROR: Fallo al preparar Worker 3
    pause
    exit /b 1
)
echo.

echo Preparando Worker 4 (192.168.131.119)...
call "%SCRIPT_DIR%preparar-worker-192.168.131.119.bat"
if errorlevel 1 (
    echo ERROR: Fallo al preparar Worker 4
    pause
    exit /b 1
)
echo.

echo Preparando Worker 5 (192.168.131.120)...
call "%SCRIPT_DIR%preparar-worker-192.168.131.120.bat"
if errorlevel 1 (
    echo ERROR: Fallo al preparar Worker 5
    pause
    exit /b 1
)
echo.

echo ==========================================
echo Todos los nodos han sido preparados
echo ==========================================
echo.
echo Siguiente paso: Iniciar los Nodes en cada host
echo.

pause

