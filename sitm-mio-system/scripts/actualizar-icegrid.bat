@echo off
REM Script para actualizar la aplicación en IceGrid (en lugar de agregar)
REM Uso: actualizar-icegrid.bat [icegrid-xml]

set ICEGRID_XML=%~1
if "%ICEGRID_XML%"=="" set ICEGRID_XML=config\icegrid.xml

echo === Actualizando aplicacion en IceGrid ===
echo Archivo: %ICEGRID_XML%
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

REM Verificar que el archivo XML existe
if not exist "%ICEGRID_XML%" (
    echo ERROR: Archivo no encontrado: %ICEGRID_XML%
    pause
    exit /b 1
)

REM Actualizar la aplicación (en lugar de agregar)
echo Actualizando aplicación DatagramProcessing...
echo Archivo: %ICEGRID_XML%
echo Ejecutando: %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application update %ICEGRID_XML%"
echo.

REM Pasar credenciales (userid: admin, password: vacío)
(echo admin & echo.) | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application update %ICEGRID_XML%" 2>&1
set UPDATE_STATUS=%ERRORLEVEL%

if %UPDATE_STATUS% equ 0 (
    echo.
    echo ✓ Aplicación actualizada exitosamente
    echo.
    echo Para iniciar los servidores, ejecuta:
    echo   scripts\iniciar-servidores-icegrid.bat
) else (
    echo.
    echo ERROR: No se pudo actualizar la aplicación (código: %UPDATE_STATUS%)
    echo.
    echo Si la aplicación no existe, primero desplégala con:
    echo   scripts\desplegar-icegrid.bat
)

echo.
pause

