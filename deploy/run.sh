#!/bin/bash
# ============================================================
# FaMa POS - Script de Arranque
# ============================================================

APP_DIR="/opt/famapos"
JAR_NAME="famapos.jar"
LOG_DIR="$APP_DIR/logs"

mkdir -p "$LOG_DIR"

# 1. Asegurar que PostgreSQL esté activo
if ! systemctl is-active --quiet postgresql; then
    echo "Iniciando servicio de PostgreSQL..." >> "$LOG_DIR/startup.log"
    systemctl start postgresql 2>> "$LOG_DIR/startup.log"
fi

# 2. Asegurar permisos para la impresora térmica USB (eQual / POS58)
if [ -e /dev/usb/lp0 ]; then
    chmod 666 /dev/usb/lp0 2>/dev/null
fi

# 3. Lanzar la aplicación JavaFX en segundo plano
cd "$APP_DIR" || exit 1
exec java -Xms256m -Xmx768m -jar "$APP_DIR/$JAR_NAME" >> "$LOG_DIR/famapos.log" 2>&1
