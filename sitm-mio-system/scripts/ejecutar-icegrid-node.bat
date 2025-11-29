@echo off
REM Script para ejecutar IceGrid Node
REM Uso: ejecutar-icegrid-node.bat [config-file]

set CONFIG_FILE=%~1
if "%CONFIG_FILE%"=="" set CONFIG_FILE=config\node.cfg

echo === Iniciando IceGrid Node ===
echo Config: %CONFIG_FILE%
echo.
echo NOTA: El Node debe estar corriendo para que IceGrid pueda desplegar servicios
echo       Asegúrate de que el Registry esté corriendo primero
echo.

REM Verificar si icegridnode está en el PATH o en ubicación estándar
set ICENODE=
where icegridnode >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set ICENODE=icegridnode
) else (
    REM Intentar ubicación estándar de Windows
    if exist "C:\Program Files\ZeroC\Ice-3.7.10\bin\icegridnode.exe" (
        set ICENODE="C:\Program Files\ZeroC\Ice-3.7.10\bin\icegridnode.exe"
    ) else if exist "C:\Program Files\ZeroC\Ice-3.7.9\bin\icegridnode.exe" (
        set ICENODE="C:\Program Files\ZeroC\Ice-3.7.9\bin\icegridnode.exe"
    ) else (
        echo ERROR: icegridnode no encontrado
        echo.
        echo Buscando en ubicaciones comunes...
        echo Por favor, ejecuta manualmente:
        echo   "C:\Program Files\ZeroC\Ice-3.7.x\bin\icegridnode.exe" --Ice.Config=%CONFIG_FILE%
        echo.
        pause
        exit /b 1
    )
)

REM Crear directorio de datos si no existe
if not exist db\node mkdir db\node

REM Ejecutar IceGrid Node desde el directorio del proyecto
REM Esto asegura que las rutas relativas en icegrid.xml funcionen correctamente
echo Ejecutando: %ICENODE% --Ice.Config=%CONFIG_FILE%
echo.
echo IMPORTANTE: El Node se ejecuta desde el directorio del proyecto
echo             para que las rutas relativas en icegrid.xml funcionen.
echo.
echo NOTA: Este proceso se quedará corriendo. Es NORMAL que no termine.
echo       Presiona Ctrl+C para detenerlo cuando quieras.
echo.
echo Esperando inicio del Node...
echo.

REM Cambiar al directorio del proyecto (donde está el script)
cd /d "%~dp0.."
%ICENODE% --Ice.Config=%CONFIG_FILE%

REM Este código solo se ejecuta si el Node se detiene
echo.
echo Node detenido.
pause

