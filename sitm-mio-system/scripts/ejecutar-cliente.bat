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

REM Verificar que el JAR existe
if not exist "integration\build\libs\datagram-client-1.0.0-all.jar" (
    echo ERROR: JAR del cliente no encontrado
    echo.
    echo Ejecuta primero: scripts\generar-jars-distribuibles.bat
    pause
    exit /b 1
)

echo.
echo NOTA: Si los datagramas están en otro computador, puedes usar:
echo   - Ruta UNC: \\\\servidor\\carpeta
echo   - Unidad de red: Z:\\
echo   Ver ACCESO-ARCHIVOS-REMOTOS.md para más detalles
echo.

REM Filtros básicos para evitar cálculos absurdos (valores por defecto razonables)
REM Tiempo: 0.1 min (6 seg) a 120 min (2 horas)
REM Velocidad: 1.0 km/h a 120.0 km/h (transporte urbano)
REM Distancia mínima: 0.01 km (10 metros)

java -DIce.Config=%CONFIG_FILE% ^
     -Ddatagram.batch.size=%BATCH_SIZE% ^
     -Ddatagram.filter.tiempoMinimo=0.1 ^
     -Ddatagram.filter.tiempoMaximo=120.0 ^
     -Ddatagram.filter.velocidadMinima=1.0 ^
     -Ddatagram.filter.velocidadMaxima=120.0 ^
     -Ddatagram.filter.distanciaMinima=0.01 ^
     -jar integration\build\libs\datagram-client-1.0.0-all.jar %DATOS_PATH%

pause

