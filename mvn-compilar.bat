@echo off
echo ========================================
echo Compilando proyecto con Maven...
echo ========================================
call mvn clean compile
if %errorlevel% == 0 (
    echo.
    echo ========================================
    echo Compilacion exitosa!
    echo ========================================
    echo Los archivos compilados se encuentran en: target\classes
) else (
    echo.
    echo ========================================
    echo Error en la compilacion
    echo ========================================
)
pause

