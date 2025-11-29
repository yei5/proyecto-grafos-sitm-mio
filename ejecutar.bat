@echo off
echo Ejecutando proyecto SITM-MIO...
if not exist bin (
    echo Error: No se encontraron archivos compilados.
    echo Por favor ejecute primero compilar.bat
    pause
    exit /b 1
)

REM Configurar classpath
set CLASSPATH=bin
if exist libs\jmapviewer.jar (
    set CLASSPATH=%CLASSPATH%;libs\jmapviewer.jar
)

java -cp %CLASSPATH% com.sitm.mio.grafos.Main datos
pause

