@echo off
setlocal enabledelayedexpansion

rem ===================================================================
rem  iniciar.bat
rem  Arranca el servicio de facturacion como aplicacion de Spring Boot
rem  a partir del archivo JAR generado con "mvnw.cmd clean package".
rem
rem  La bitacora de consola del servicio se escribe en logs\facturacion.log
rem  y el identificador del proceso queda en bin\facturacion.pid para que
rem  detener.bat sepa a quien detener.
rem
rem  Uso:  bin\iniciar.bat
rem ===================================================================

set "DIR_BIN=%~dp0"
set "DIR_PROYECTO=%~dp0.."
set "NOMBRE_JAR=facturacion-0.0.1-SNAPSHOT.jar"
set "ARCHIVO_PID=%DIR_BIN%facturacion.pid"

echo.
echo  ==================================================
echo   Servicio de Facturacion - Arranque
echo  ==================================================

rem --- 1. Verificar que Java este disponible ------------------------
java -version >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] No se encontro Java en el PATH.
    echo          Instala el JDK 21 y vuelve a intentarlo.
    exit /b 1
)

rem --- 2. Verificar que exista el JAR ------------------------------
if not exist "%DIR_PROYECTO%\target\%NOMBRE_JAR%" (
    echo  [ERROR] No se encontro el archivo JAR:
    echo          %DIR_PROYECTO%\target\%NOMBRE_JAR%
    echo.
    echo          Generalo primero con:  mvnw.cmd clean package
    exit /b 1
)

rem --- 3. Verificar que no haya una instancia corriendo ------------
if exist "%ARCHIVO_PID%" (
    set /p PID_PREVIO=<"%ARCHIVO_PID%"
    tasklist /FI "PID eq !PID_PREVIO!" 2>nul | find "!PID_PREVIO!" >nul
    if not errorlevel 1 (
        echo  [AVISO] El servicio ya esta en ejecucion con el PID !PID_PREVIO!.
        echo          Ejecuta bin\detener.bat antes de volver a iniciarlo.
        exit /b 1
    )
    echo  [INFO ] Se encontro un PID obsoleto; se descarta.
    del "%ARCHIVO_PID%" >nul 2>&1
)

rem --- 4. Preparar la carpeta de bitacoras -------------------------
if not exist "%DIR_PROYECTO%\logs" mkdir "%DIR_PROYECTO%\logs"

pushd "%DIR_PROYECTO%"

echo.>> logs\facturacion.log
echo ===============================================================>> logs\facturacion.log
echo [INICIO] Arranque del servicio de facturacion - %DATE% %TIME%>> logs\facturacion.log
echo ===============================================================>> logs\facturacion.log

rem --- 5. Arrancar el servicio en segundo plano --------------------
echo  [INFO ] Iniciando el servicio...
start "facturacion" /B cmd /c "java -jar target\%NOMBRE_JAR% >> logs\facturacion.log 2>&1"

popd

rem --- 6. Registrar el PID del proceso Java ------------------------
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Sleep -Seconds 4; $p = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine -like '*%NOMBRE_JAR%*' } | Select-Object -First 1; if ($p) { Set-Content -Path '%ARCHIVO_PID%' -Value $p.ProcessId }"

if exist "%ARCHIVO_PID%" (
    set /p PID_ACTUAL=<"%ARCHIVO_PID%"
    echo  [INFO ] Servicio iniciado con el PID !PID_ACTUAL!.
) else (
    echo  [AVISO] El servicio se lanzo pero no se pudo registrar el PID.
    echo          Revisa la bitacora por si el arranque fallo.
)

echo  [INFO ] Bitacora: %DIR_PROYECTO%\logs\facturacion.log
echo  [INFO ] El API responde en http://localhost:8080
echo  [INFO ] Espera unos segundos y prueba:  curl http://localhost:8080/actuator/health
echo.

endlocal
exit /b 0
