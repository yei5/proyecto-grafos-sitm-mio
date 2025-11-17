@echo off
echo Compilando proyecto SITM-MIO...
if not exist bin mkdir bin
javac -d bin -encoding UTF-8 src\main\java\com\sitm\mio\grafos\*.java
if %errorlevel% == 0 (
    echo Compilacion exitosa!
    echo Los archivos .class se encuentran en la carpeta bin\
) else (
    echo Error en la compilacion
)
pause

