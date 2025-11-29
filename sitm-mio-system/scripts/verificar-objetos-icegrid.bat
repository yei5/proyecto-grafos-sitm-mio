@echo off
REM Script para verificar objetos registrados en IceGrid
REM Usa icegridadmin para listar objetos

echo === Verificando objetos registrados en IceGrid ===
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

echo [1] Listando todos los objetos registrados:
echo.
(echo. & echo.) | %ICEADMIN% --Ice.Config=config\admin.cfg -e "object list" 2>&1
echo.

echo [2] Buscando objeto con identity "DatagramMaster":
echo.
(echo. & echo.) | %ICEADMIN% --Ice.Config=config\admin.cfg -e "object find DatagramMaster" 2>&1
echo.

echo [3] Información del adapter "DatagramMaster.Adapter":
echo.
(echo. & echo.) | %ICEADMIN% --Ice.Config=config\admin.cfg -e "adapter list" 2>&1 | findstr /i "DatagramMaster"
echo.

echo [4] Servidores activos:
echo.
(echo. & echo.) | %ICEADMIN% --Ice.Config=config\admin.cfg -e "server list" 2>&1
echo.

echo === Notas ===
echo - Si "DatagramMaster" no aparece, el objeto no está registrado explícitamente
echo - El objeto puede estar disponible vía Locator aunque no aparezca aquí
echo - Verifica que el Master esté corriendo y que use el Locator correcto
echo.

pause

