@echo off
REM Script para generar los JARs distribuibles (fat JARs con todas las dependencias)
REM Uso: generar-jars-distribuibles.bat

echo === Generando JARs distribuibles ===
echo.

REM Generar JARs con todas las dependencias
echo Generando JAR del Master...
gradle :datagram-master-service:distJar
if %ERRORLEVEL% neq 0 (
    echo ERROR: No se pudo generar JAR del Master
    pause
    exit /b 1
)

echo Generando JAR del Worker...
gradle :datagram-worker-service:distJar
if %ERRORLEVEL% neq 0 (
    echo ERROR: No se pudo generar JAR del Worker
    pause
    exit /b 1
)

echo Generando JAR del Cliente...
gradle :integration:distJar
if %ERRORLEVEL% neq 0 (
    echo ERROR: No se pudo generar JAR del Cliente
    pause
    exit /b 1
)

echo.
echo ✓ JARs distribuibles generados exitosamente:
echo.
echo   Master Service:
echo     datagram-master-service/build/libs/datagram-master-service-1.0.0-all.jar
echo.
echo   Worker Service:
echo     datagram-worker-service/build/libs/datagram-worker-service-1.0.0-all.jar
echo.
echo   Cliente:
echo     integration/build/libs/datagram-client-1.0.0-all.jar
echo.
echo Estos JARs incluyen todas las dependencias y pueden ejecutarse independientemente.
echo.
echo IMPORTANTE: Estos JARs son necesarios para desplegar los servidores en IceGrid.
echo.
pause

