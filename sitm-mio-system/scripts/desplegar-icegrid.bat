@echo off
REM Script para desplegar la aplicación en IceGrid
REM Uso: desplegar-icegrid.bat [icegrid-xml]

set ICEGRID_XML=%~1
if "%ICEGRID_XML%"=="" set ICEGRID_XML=config\icegrid.xml

echo === Desplegando aplicacion en IceGrid ===
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

REM Usar archivo de configuración para evitar pedir credenciales
set ADMIN_CFG=config\admin.cfg
if not exist "%ADMIN_CFG%" (
    echo ERROR: Archivo de configuración no encontrado: %ADMIN_CFG%
    pause
    exit /b 1
)

REM Verificar que el Registry esté corriendo
echo Verificando conexión al Registry...
echo Ejecutando: %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application list"
echo.

REM Usar archivo de configuración y pasar credenciales (userid: admin, password: vacío)
(echo admin & echo.) | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application list" 2>&1
set REGISTRY_STATUS=%ERRORLEVEL%

if %REGISTRY_STATUS% neq 0 (
    echo.
    echo ERROR: No se pudo conectar al Registry (código: %REGISTRY_STATUS%)
    echo.
    echo Asegúrate de que el Registry esté corriendo:
    echo   1. Abre otra terminal
    echo   2. Ejecuta: scripts\ejecutar-icegrid-registry.bat
    echo   3. Espera a ver "Registry listening..."
    echo   4. Luego ejecuta este script de nuevo
    echo.
    pause
    exit /b 1
)

echo.
echo ✓ Registry conectado correctamente
echo.

REM Verificar que el archivo XML existe
if not exist "%ICEGRID_XML%" (
    echo ERROR: Archivo no encontrado: %ICEGRID_XML%
    pause
    exit /b 1
)

REM Desplegar la aplicación
echo Desplegando aplicación DatagramProcessing...
echo Archivo: %ICEGRID_XML%
echo Ejecutando: %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application add %ICEGRID_XML%"
echo NOTA: Si pide credenciales, presiona Enter para usuario y contraseña
echo.

REM Pasar credenciales directamente (userid: admin, password: vacío)
(echo admin & echo.) | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application add %ICEGRID_XML%" 2>&1
set DEPLOY_STATUS=%ERRORLEVEL%

if %DEPLOY_STATUS% equ 0 (
    echo.
    echo ✓ Aplicación desplegada exitosamente
    echo.
    echo Para iniciar los servidores, ejecuta:
    echo   scripts\iniciar-servidores-icegrid.bat
    echo.
    echo O manualmente:
    echo   %ICEADMIN% --Ice.Config=%ADMIN_CFG%
    echo   server start DatagramMaster1
    echo   server start DatagramWorker1
    echo   server start DatagramWorker2
) else (
    echo.
    echo ERROR: No se pudo desplegar la aplicación (código: %DEPLOY_STATUS%)
    echo.
    echo Posibles causas:
    echo   - El archivo %ICEGRID_XML% tiene errores de sintaxis
    echo   - La aplicación ya está desplegada (usa "application update" en lugar de "add")
    echo   - El Node no está corriendo
    echo.
    echo Para actualizar una aplicación existente:
    echo   (echo admin & echo.) | %ICEADMIN% --Ice.Config=%ADMIN_CFG% -e "application update %ICEGRID_XML%"
)

echo.
pause

