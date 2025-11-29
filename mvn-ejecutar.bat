@echo off
echo ========================================
echo Ejecutando proyecto con Maven...
echo ========================================
call mvn exec:java
if %errorlevel% == 0 (
    echo.
    echo ========================================
    echo Ejecucion completada!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo Error en la ejecucion
    echo ========================================
)
pause

