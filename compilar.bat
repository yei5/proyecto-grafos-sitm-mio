@echo off
echo Compilando proyecto SITM-MIO...
if not exist bin mkdir bin

REM Verificar si existe JMapViewer
set CLASSPATH=.
if exist libs\jmapviewer.jar (
    set CLASSPATH=%CLASSPATH%;libs\jmapviewer.jar
    echo JMapViewer encontrado, incluyendo en classpath...
) else (
    echo JMapViewer no encontrado, compilando sin dependencias de mapa...
    echo (Para usar mapas, descarga jmapviewer.jar y colocalo en libs\)
)

javac -d bin -encoding UTF-8 -cp %CLASSPATH% src\main\java\com\sitm\mio\grafos\*.java
if %errorlevel% == 0 (
    echo Compilacion exitosa!
    echo Los archivos .class se encuentran en la carpeta bin\
) else (
    echo Error en la compilacion
)
pause

