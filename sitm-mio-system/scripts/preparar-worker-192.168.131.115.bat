@echo off
REM Script para preparar el host Worker 1 (192.168.131.115 - Node2)
REM Usuario: swarch
REM Contraseña: swarch

setlocal enabledelayedexpansion

set USER=swarch
set HOST=192.168.131.115
set NODE_NAME=Node2
set PASSWORD=swarch
set REMOTE_DIR=~/sitm-mio-distribuido
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..

echo ==========================================
echo Preparando Host Worker: %HOST% (%NODE_NAME%)
echo ==========================================
echo.

REM Verificar que los JARs existan
if not exist "%PROJECT_DIR%\datagram-worker-service\build\libs\datagram-worker-service-1.0.0-all.jar" (
    echo ERROR: No se encuentra el JAR del Worker
    echo Ruta esperada: %PROJECT_DIR%\datagram-worker-service\build\libs\datagram-worker-service-1.0.0-all.jar
    pause
    exit /b 1
)

echo 1. Creando estructura de directorios en %HOST%...
echo    [INFO] Por favor, ingresa la contraseña cuando se solicite: %PASSWORD%
ssh -o StrictHostKeyChecking=no %USER%@%HOST% "mkdir -p ~/sitm-mio-distribuido/datagram-worker-service/build/libs && mkdir -p ~/sitm-mio-distribuido/config && mkdir -p ~/sitm-mio-distribuido/db/node && echo '   [OK] Directorios creados'"

if errorlevel 1 (
    echo    [ERROR] Fallo al crear directorios
    pause
    exit /b 1
)
echo    [OK] Directorios creados

echo.
echo 2. Copiando JAR del Worker...
echo    [INFO] Por favor, ingresa la contraseña cuando se solicite: %PASSWORD%
scp -o StrictHostKeyChecking=no "%PROJECT_DIR%\datagram-worker-service\build\libs\datagram-worker-service-1.0.0-all.jar" %USER%@%HOST%:%REMOTE_DIR%/datagram-worker-service/build/libs/

if errorlevel 1 (
    echo    [ERROR] Fallo al copiar JAR
    pause
    exit /b 1
)
echo    [OK] JAR copiado

echo.
echo 3. Copiando y configurando archivos de configuración...

REM Crear archivo node.cfg temporal con el nombre correcto del Node
powershell -Command "(Get-Content '%PROJECT_DIR%\config\node-worker.cfg') -replace 'IceGrid.Node.Name=NodeX', 'IceGrid.Node.Name=%NODE_NAME%' | Set-Content '%TEMP%\node-worker-temp.cfg'"

REM Copiar worker-remote.cfg como worker.cfg
echo    [INFO] Por favor, ingresa la contraseña cuando se solicite: %PASSWORD%
scp -o StrictHostKeyChecking=no "%PROJECT_DIR%\config\worker-remote.cfg" %USER%@%HOST%:%REMOTE_DIR%/config/worker.cfg

REM Copiar node.cfg con el nombre correcto
scp -o StrictHostKeyChecking=no "%TEMP%\node-worker-temp.cfg" %USER%@%HOST%:%REMOTE_DIR%/config/node.cfg

if errorlevel 1 (
    echo    [ERROR] Fallo al copiar archivos de configuración
    pause
    exit /b 1
)
echo    [OK] Archivos de configuración copiados (Node: %NODE_NAME%)

REM Limpiar archivo temporal
del "%TEMP%\node-worker-temp.cfg" 2>nul

echo.
echo ==========================================
echo Preparación completada exitosamente
echo ==========================================
echo.
echo Para iniciar el Node Worker, ejecuta en %HOST%:
echo   ssh %USER%@%HOST%
echo   cd ~/sitm-mio-distribuido
echo   icegridnode --Ice.Config=config/node.cfg
echo.

pause
