@echo off
REM Script para preparar el host Master (192.168.131.121)
REM Usuario: swarch
REM Contraseña: swarch

setlocal enabledelayedexpansion

set USER=swarch
set HOST=192.168.131.121
set PASSWORD=swarch
set REMOTE_DIR=~/sitm-mio-distribuido
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..

echo ==========================================
echo Preparando Host Master: %HOST%
echo ==========================================
echo.

REM Verificar que los JARs existan
if not exist "%PROJECT_DIR%\datagram-master-service\build\libs\datagram-master-service-1.0.0-all.jar" (
    echo ERROR: No se encuentra el JAR del Master
    echo Ruta esperada: %PROJECT_DIR%\datagram-master-service\build\libs\datagram-master-service-1.0.0-all.jar
    pause
    exit /b 1
)

echo 1. Creando estructura de directorios en %HOST%...
echo    [INFO] Por favor, ingresa la contraseña cuando se solicite: %PASSWORD%
ssh -o StrictHostKeyChecking=no %USER%@%HOST% "mkdir -p ~/sitm-mio-distribuido/datagram-master-service/build/libs && mkdir -p ~/sitm-mio-distribuido/config && mkdir -p ~/sitm-mio-distribuido/db/node && echo '   [OK] Directorios creados'"

if errorlevel 1 (
    echo    [ERROR] Fallo al crear directorios
    echo    Asegúrate de que SSH esté disponible y la conexión sea posible
    pause
    exit /b 1
)
echo    [OK] Directorios creados

echo.
echo 2. Copiando JAR del Master...
echo    [INFO] Por favor, ingresa la contraseña cuando se solicite: %PASSWORD%
scp -o StrictHostKeyChecking=no "%PROJECT_DIR%\datagram-master-service\build\libs\datagram-master-service-1.0.0-all.jar" %USER%@%HOST%:%REMOTE_DIR%/datagram-master-service/build/libs/

if errorlevel 1 (
    echo    [ERROR] Fallo al copiar JAR
    pause
    exit /b 1
)
echo    [OK] JAR copiado

echo.
echo 3. Copiando archivos de configuración...
echo    [INFO] Por favor, ingresa la contraseña cuando se solicite: %PASSWORD%

REM Copiar master-remote.cfg como master.cfg
scp -o StrictHostKeyChecking=no "%PROJECT_DIR%\config\master-remote.cfg" %USER%@%HOST%:%REMOTE_DIR%/config/master.cfg

REM Copiar node-master.cfg como node.cfg
scp -o StrictHostKeyChecking=no "%PROJECT_DIR%\config\node-master.cfg" %USER%@%HOST%:%REMOTE_DIR%/config/node.cfg

if errorlevel 1 (
    echo    [ERROR] Fallo al copiar archivos de configuración
    pause
    exit /b 1
)
echo    [OK] Archivos de configuración copiados

echo.
echo ==========================================
echo Preparación completada exitosamente
echo ==========================================
echo.
echo Para iniciar el Node Master, ejecuta en %HOST%:
echo   ssh %USER%@%HOST%
echo   cd ~/sitm-mio-distribuido
echo   icegridnode --Ice.Config=config/node.cfg
echo.

pause
