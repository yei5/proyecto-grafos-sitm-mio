@echo off
REM Script para verificar que los JARs existen antes de desplegar
REM Uso: verificar-jars.bat

echo === Verificando JARs distribuibles ===
echo.

set JAR_MASTER=datagram-master-service\build\libs\datagram-master-service-1.0.0-all.jar
set JAR_WORKER=datagram-worker-service\build\libs\datagram-worker-service-1.0.0-all.jar
set JAR_CLIENT=integration\build\libs\datagram-client-1.0.0-all.jar

set ERROR=0

if not exist "%JAR_MASTER%" (
    echo ✗ ERROR: No se encuentra: %JAR_MASTER%
    set ERROR=1
) else (
    echo ✓ Encontrado: %JAR_MASTER%
)

if not exist "%JAR_WORKER%" (
    echo ✗ ERROR: No se encuentra: %JAR_WORKER%
    set ERROR=1
) else (
    echo ✓ Encontrado: %JAR_WORKER%
)

if not exist "%JAR_CLIENT%" (
    echo ⚠ ADVERTENCIA: No se encuentra: %JAR_CLIENT%
    echo   (No es crítico para desplegar servidores)
) else (
    echo ✓ Encontrado: %JAR_CLIENT%
)

echo.

if %ERROR% equ 1 (
    echo ERROR: Faltan JARs necesarios.
    echo.
    echo Para generar los JARs, ejecuta:
    echo   scripts\generar-jars-distribuibles.bat
    echo.
    pause
    exit /b 1
) else (
    echo ✓ Todos los JARs necesarios están presentes.
    echo.
)

pause

