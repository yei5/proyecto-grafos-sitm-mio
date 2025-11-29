@echo off
echo ========================================
echo Empaquetando proyecto con Maven...
echo ========================================
echo Esto creara un JAR ejecutable con todas las dependencias
echo.
call mvn clean package
if %errorlevel% == 0 (
    echo.
    echo ========================================
    echo Empaquetado exitoso!
    echo ========================================
    echo JAR ejecutable creado en: target\proyecto-grafos-sitm-mio-1.0.0-jar-with-dependencies.jar
    echo.
    echo Para ejecutar el JAR:
    echo   java -jar target\proyecto-grafos-sitm-mio-1.0.0-jar-with-dependencies.jar datos
) else (
    echo.
    echo ========================================
    echo Error en el empaquetado
    echo ========================================
)
pause

