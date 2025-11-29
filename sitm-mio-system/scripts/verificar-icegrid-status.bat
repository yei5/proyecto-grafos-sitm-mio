@echo off
REM Script para verificar el estado de IceGrid
REM Uso: verificar-icegrid-status.bat

echo === Verificando estado de IceGrid ===
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

REM Verificar conexión al Registry
echo [1] Verificando conexión al Registry...
(echo. & echo.) | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application list" >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo ✓ Registry está corriendo y accesible
) else (
    echo ✗ Registry NO está accesible
    echo   Asegúrate de ejecutar: scripts\ejecutar-icegrid-registry.bat
    echo.
    pause
    exit /b 1
)
echo.

REM Listar aplicaciones
echo [2] Aplicaciones desplegadas:
(echo. & echo.) | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application list" 2>&1
echo.

REM Listar servidores
echo [3] Servidores:
(echo. & echo.) | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "server list" 2>&1
echo.

REM Buscar Master
echo [4] Buscando Master...
(echo. & echo.) | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "object find DatagramMaster" 2>&1
echo.

echo === Verificación completada ===
echo.
pause

