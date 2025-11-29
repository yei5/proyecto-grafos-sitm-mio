@echo off
REM Script para ejecutar Cliente
REM Uso: ejecutar-cliente.bat [datos-path] [config-file]

set DATOS_PATH=%~1
if "%DATOS_PATH%"=="" set DATOS_PATH=datos

set CONFIG_FILE=%~2
if "%CONFIG_FILE%"=="" set CONFIG_FILE=config\client.cfg

set BATCH_SIZE=%~3
if "%BATCH_SIZE%"=="" set BATCH_SIZE=1000

echo === Iniciando Cliente Distribuido ===
echo Datos: %DATOS_PATH%
echo Config: %CONFIG_FILE%
echo Batch Size: %BATCH_SIZE%

java -DIce.Config=%CONFIG_FILE% -Ddatagram.batch.size=%BATCH_SIZE% -jar integration\build\libs\datagram-client-1.0.0.jar %DATOS_PATH%

pause

