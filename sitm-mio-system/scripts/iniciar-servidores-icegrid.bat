@echo off
REM Script para iniciar servidores en IceGrid
REM Uso: iniciar-servidores-icegrid.bat [master] [worker1] [worker2] ...

echo === Iniciando servidores en IceGrid ===
echo.

REM Verificar que icegridadmin esté disponible
set ICEADMIN=icegridadmin
where %ICEADMIN% >nul 2>&1
if %ERRORLEVEL% neq 0 (
    set ICE_HOME="C:\Program Files\ZeroC\Ice-3.7.10"
    if exist "%ICE_HOME%\bin\icegridadmin.exe" (
        set ICEADMIN="%ICE_HOME%\bin\icegridadmin.exe"
    ) else (
        echo ERROR: icegridadmin no encontrado
        pause
        exit /b 1
    )
)

REM Usar archivo de configuración
set ADMIN_CFG=config\admin.cfg
if not exist "%ADMIN_CFG%" (
    echo ERROR: Archivo de configuración no encontrado: %ADMIN_CFG%
    pause
    exit /b 1
)

REM Crear archivo temporal con credenciales vacías
set TEMP_INPUT=%TEMP%\icegridadmin_input_%RANDOM%.txt
echo. > "%TEMP_INPUT%"
echo. >> "%TEMP_INPUT%"

REM Verificar conexión al Registry
echo Verificando conexión al Registry...
type "%TEMP_INPUT%" | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application list" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    del "%TEMP_INPUT%" 2>nul
    echo ERROR: No se pudo conectar al Registry
    echo Asegúrate de que el Registry esté corriendo
    pause
    exit /b 1
)
echo ✓ Registry conectado
echo.

REM Iniciar Master si se especifica o por defecto
if "%1"=="master" goto :start_master
if "%1"=="" goto :start_all

:start_master
echo Iniciando Master...
type "%TEMP_INPUT%" | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "server start DatagramMaster1" 2>&1
if %ERRORLEVEL% equ 0 (
    echo ✓ Master iniciado
) else (
    echo ✗ Error iniciando Master
    echo Verifica que la aplicación esté desplegada y el Node esté corriendo
)
echo.

if "%1"=="master" goto :cleanup

:start_all
REM Iniciar Workers
echo Iniciando Workers...
echo.
echo Iniciando Worker1...
type "%TEMP_INPUT%" | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "server start DatagramWorker1" 2>&1
if %ERRORLEVEL% equ 0 (
    echo ✓ Worker1 iniciado
) else (
    echo ✗ Error iniciando Worker1
    echo Verifica que la aplicación esté desplegada y el Node esté corriendo
)
echo.

echo Iniciando Worker2...
type "%TEMP_INPUT%" | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "server start DatagramWorker2" 2>&1
if %ERRORLEVEL% equ 0 (
    echo ✓ Worker2 iniciado
) else (
    echo ✗ Error iniciando Worker2
    echo Verifica que la aplicación esté desplegada y el Node esté corriendo
)

:cleanup
REM Limpiar archivo temporal
del "%TEMP_INPUT%" 2>nul

:end
echo.
echo Para ver el estado de los servidores:
echo   icegridadmin --Ice.Default.Locator="IceGrid/Locator:tcp -h localhost -p 4061"
echo   server list
echo.
pause

