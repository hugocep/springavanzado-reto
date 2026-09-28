#!/usr/bin/env bash
# ===================================================================
#  detener.sh
#  Detiene el servicio de facturacion iniciado con iniciar.sh y deja
#  constancia del paro en la bitacora logs/facturacion.log
#
#  Se envia primero SIGTERM para que Spring Boot ejecute su cierre
#  ordenado y escriba sus propios mensajes de fin en la bitacora. Solo
#  si el proceso no responde se recurre a SIGKILL.
#
#  Uso:  ./bin/detener.sh
# ===================================================================
set -u

DIR_BIN="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_PROYECTO="$(dirname "$DIR_BIN")"
NOMBRE_JAR="facturacion-0.0.1-SNAPSHOT.jar"
DIR_LOG="$DIR_PROYECTO/logs"
ARCHIVO_LOG="$DIR_LOG/facturacion.log"
ARCHIVO_PID="$DIR_BIN/facturacion.pid"
ESPERA_MAXIMA=20

echo ""
echo " =================================================="
echo "  Servicio de Facturacion - Paro"
echo " =================================================="

mkdir -p "$DIR_LOG"

# --- 1. Obtener el PID registrado ----------------------------------
PID=""
if [ -f "$ARCHIVO_PID" ]; then
    PID="$(cat "$ARCHIVO_PID")"
fi

# --- 2. Si no hay PID, buscar el proceso por el nombre del JAR -----
if [ -z "$PID" ]; then
    echo " [INFO ] No hay archivo de PID; se busca el proceso por el nombre del JAR."
    PID="$(pgrep -f "$NOMBRE_JAR" | head -n 1 || true)"
fi

if [ -z "$PID" ]; then
    echo " [AVISO] El servicio no parece estar en ejecucion."
    exit 0
fi

if ! kill -0 "$PID" 2>/dev/null; then
    echo " [AVISO] El proceso $PID ya no existe; se limpia el archivo de PID."
    rm -f "$ARCHIVO_PID"
    exit 0
fi

# --- 3. Registrar el paro en la bitacora ---------------------------
{
    echo ""
    echo "==============================================================="
    echo "[FIN] Paro del servicio de facturacion (PID $PID) - $(date '+%Y-%m-%d %H:%M:%S')"
    echo "==============================================================="
} >> "$ARCHIVO_LOG"

# --- 4. Cierre ordenado --------------------------------------------
echo " [INFO ] Deteniendo el proceso $PID..."
kill "$PID" 2>/dev/null

SEGUNDOS=0
while kill -0 "$PID" 2>/dev/null && [ "$SEGUNDOS" -lt "$ESPERA_MAXIMA" ]; do
    sleep 1
    SEGUNDOS=$((SEGUNDOS + 1))
done

# --- 5. Cierre forzado si fue necesario ----------------------------
if kill -0 "$PID" 2>/dev/null; then
    echo " [AVISO] El proceso no respondio en $ESPERA_MAXIMA s; se fuerza el paro."
    kill -9 "$PID" 2>/dev/null
    sleep 1
fi

if kill -0 "$PID" 2>/dev/null; then
    echo " [ERROR] No se pudo detener el proceso $PID."
    exit 1
fi

rm -f "$ARCHIVO_PID"
echo "[FIN] Servicio detenido correctamente - $(date '+%Y-%m-%d %H:%M:%S')" >> "$ARCHIVO_LOG"

echo " [INFO ] Servicio detenido correctamente."
echo " [INFO ] Bitacora: $ARCHIVO_LOG"
echo ""
exit 0
