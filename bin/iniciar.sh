#!/usr/bin/env bash
# ===================================================================
#  iniciar.sh
#  Arranca el servicio de facturacion como aplicacion de Spring Boot
#  a partir del archivo JAR generado con "./mvnw clean package".
#
#  La bitacora de consola del servicio se escribe en logs/facturacion.log
#  y el identificador del proceso queda en bin/facturacion.pid para que
#  detener.sh sepa a quien detener.
#
#  Uso:  ./bin/iniciar.sh
# ===================================================================
set -u

DIR_BIN="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_PROYECTO="$(dirname "$DIR_BIN")"
NOMBRE_JAR="facturacion-0.0.1-SNAPSHOT.jar"
ARCHIVO_JAR="$DIR_PROYECTO/target/$NOMBRE_JAR"
DIR_LOG="$DIR_PROYECTO/logs"
ARCHIVO_LOG="$DIR_LOG/facturacion.log"
ARCHIVO_PID="$DIR_BIN/facturacion.pid"

echo ""
echo " =================================================="
echo "  Servicio de Facturacion - Arranque"
echo " =================================================="

# --- 1. Verificar que Java este disponible -------------------------
if ! command -v java >/dev/null 2>&1; then
    echo " [ERROR] No se encontro Java en el PATH."
    echo "         Instala el JDK 21 y vuelve a intentarlo."
    exit 1
fi

# --- 2. Verificar que exista el JAR --------------------------------
if [ ! -f "$ARCHIVO_JAR" ]; then
    echo " [ERROR] No se encontro el archivo JAR:"
    echo "         $ARCHIVO_JAR"
    echo ""
    echo "         Generalo primero con:  ./mvnw clean package"
    exit 1
fi

# --- 3. Verificar que no haya una instancia corriendo --------------
if [ -f "$ARCHIVO_PID" ]; then
    PID_PREVIO="$(cat "$ARCHIVO_PID")"
    if kill -0 "$PID_PREVIO" 2>/dev/null; then
        echo " [AVISO] El servicio ya esta en ejecucion con el PID $PID_PREVIO."
        echo "         Ejecuta ./bin/detener.sh antes de volver a iniciarlo."
        exit 1
    fi
    echo " [INFO ] Se encontro un PID obsoleto; se descarta."
    rm -f "$ARCHIVO_PID"
fi

# --- 4. Preparar la carpeta de bitacoras ---------------------------
mkdir -p "$DIR_LOG"

{
    echo ""
    echo "==============================================================="
    echo "[INICIO] Arranque del servicio de facturacion - $(date '+%Y-%m-%d %H:%M:%S')"
    echo "==============================================================="
} >> "$ARCHIVO_LOG"

# --- 5. Arrancar el servicio en segundo plano ----------------------
echo " [INFO ] Iniciando el servicio..."
cd "$DIR_PROYECTO"
nohup java -jar "$ARCHIVO_JAR" >> "$ARCHIVO_LOG" 2>&1 &
PID_NUEVO=$!
echo "$PID_NUEVO" > "$ARCHIVO_PID"

sleep 3
if ! kill -0 "$PID_NUEVO" 2>/dev/null; then
    echo " [ERROR] El servicio no pudo arrancar. Revisa la bitacora:"
    echo "         $ARCHIVO_LOG"
    rm -f "$ARCHIVO_PID"
    exit 1
fi

echo " [INFO ] Servicio iniciado con el PID $PID_NUEVO."
echo " [INFO ] Bitacora: $ARCHIVO_LOG"
echo " [INFO ] El API responde en http://localhost:8080"
echo " [INFO ] Espera unos segundos y prueba:  curl http://localhost:8080/actuator/health"
echo ""
exit 0
