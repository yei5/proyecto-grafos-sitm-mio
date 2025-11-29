@echo off
REM Script para ejecutar IceGrid Registry
REM Uso: ejecutar-icegrid-registry.bat [config-file]

set CONFIG_FILE=%~1
if "%CONFIG_FILE%"=="" set CONFIG_FILE=config\registry.cfg

echo === Iniciando IceGrid Registry ===
echo Config: %CONFIG_FILE%
echo Puerto: 4061 (cliente), 4062 (servidor)
echo.

REM Verificar si icegridregistry está en el PATH o en ubicación estándar
set ICEREGISTRY=
where icegridregistry >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set ICEREGISTRY=icegridregistry
) else (
    REM Intentar ubicación estándar de Windows
    if exist "C:\Program Files\ZeroC\Ice-3.7.10\bin\icegridregistry.exe" (
        set ICEREGISTRY="C:\Program Files\ZeroC\Ice-3.7.10\bin\icegridregistry.exe"
    ) else if exist "C:\Program Files\ZeroC\Ice-3.7.9\bin\icegridregistry.exe" (
        set ICEREGISTRY="C:\Program Files\ZeroC\Ice-3.7.9\bin\icegridregistry.exe"
    ) else (
        echo ERROR: icegridregistry no encontrado
        echo.
        echo Buscando en ubicaciones comunes...
        echo Por favor, ejecuta manualmente:
        echo   "C:\Program Files\ZeroC\Ice-3.7.x\bin\icegridregistry.exe" --Ice.Config=%CONFIG_FILE%
        echo.
        pause
        exit /b 1
    )
)

REM Crear directorio de datos si no existe
if not exist db\registry mkdir db\registry

REM Ejecutar IceGrid Registry
echo Ejecutando: %ICEREGISTRY% --Ice.Config=%CONFIG_FILE%
echo.
echo NOTA: Este proceso se quedará corriendo. Es NORMAL que no termine.
echo       Presiona Ctrl+C para detenerlo cuando quieras.
echo.
echo Esperando inicio del Registry...
echo.

%ICEREGISTRY% --Ice.Config=%CONFIG_FILE%

REM Este código solo se ejecuta si el Registry se detiene
echo.
echo Registry detenido.
pause

