@echo off
echo Ejecutando proyecto SITM-MIO...
if not exist bin (
    echo Error: No se encontraron archivos compilados.
    echo Por favor ejecute primero compilar.bat
    pause
    exit /b 1
)
java -cp bin com.sitm.mio.grafos.Main
pause

