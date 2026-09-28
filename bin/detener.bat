@echo off
setlocal enabledelayedexpansion

rem ===================================================================
rem  detener.bat
rem  Detiene el servicio de facturacion iniciado con iniciar.bat y deja
rem  constancia del paro en la bitacora logs\facturacion.log
rem
rem  Importante: el bloque [FIN] se escribe DESPUES de terminar el
rem  proceso. Mientras el servicio vive, Windows mantiene la bitacora
rem  abierta en exclusiva y ninguna otra escritura es posible.
rem
rem  Uso:  bin\detener.bat
rem ===================================================================

set "DIR_BIN=%~dp0"
set "DIR_PROYECTO=%~dp0.."
set "NOMBRE_JAR=facturacion-0.0.1-SNAPSHOT.jar"
set "ARCHIVO_PID=%DIR_BIN%facturacion.pid"
set "ARCHIVO_LOG=%DIR_PROYECTO%\logs\facturacion.log"

echo.
echo  ==================================================
echo   Servicio de Facturacion - Paro
echo  ==================================================

if not exist "%DIR_PROYECTO%\logs" mkdir "%DIR_PROYECTO%\logs"

rem --- 1. Obtener el PID registrado --------------------------------
set "PID="
if exist "%ARCHIVO_PID%" (
    set /p PID=<"%ARCHIVO_PID%"
)

rem --- 2. Si no hay PID, buscar el proceso por el nombre del JAR ---
if "!PID!"=="" (
    echo  [INFO ] No hay archivo de PID; se busca el proceso por el nombre del JAR.
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$p = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*%NOMBRE_JAR%*' } | Select-Object -First 1; if ($p) { Set-Content -Path '%ARCHIVO_PID%' -Value $p.ProcessId }"
    if exist "%ARCHIVO_PID%" set /p PID=<"%ARCHIVO_PID%"
)

if "!PID!"=="" (
    echo  [AVISO] El servicio no parece estar en ejecucion.
    exit /b 0
)

rem --- 3. Verificar que el proceso siga vivo -----------------------
tasklist /FI "PID eq !PID!" 2>nul | find "!PID!" >nul
if errorlevel 1 (
    echo  [AVISO] El proceso !PID! ya no existe; se limpia el archivo de PID.
    if exist "%ARCHIVO_PID%" del "%ARCHIVO_PID%" >nul 2>&1
    exit /b 0
)

rem --- 4. Detener el proceso ---------------------------------------
echo  [INFO ] Deteniendo el proceso !PID!...
taskkill /PID !PID! /T /F >nul 2>&1

rem --- 5. Esperar a que Windows libere la bitacora -----------------
timeout /t 3 /nobreak >nul

tasklist /FI "PID eq !PID!" 2>nul | find "!PID!" >nul
if not errorlevel 1 (
    echo  [ERROR] No se pudo detener el proceso !PID!.
    exit /b 1
)

if exist "%ARCHIVO_PID%" del "%ARCHIVO_PID%" >nul 2>&1

rem --- 6. Registrar el paro en la bitacora, ya liberada ------------
>>"%ARCHIVO_LOG%" echo.
>>"%ARCHIVO_LOG%" echo ===============================================================
>>"%ARCHIVO_LOG%" echo [FIN] Paro del servicio de facturacion ^(PID !PID!^) - %DATE% %TIME%
>>"%ARCHIVO_LOG%" echo [FIN] Servicio detenido correctamente
>>"%ARCHIVO_LOG%" echo ===============================================================

echo  [INFO ] Servicio detenido correctamente.
echo  [INFO ] Bitacora: %ARCHIVO_LOG%
echo.

endlocal
exit /b 0
