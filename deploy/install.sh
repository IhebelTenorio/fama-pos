#!/bin/bash
# ============================================================
# FaMa POS - Script Instalador Automatizado para LMDE 7
# Ejecutar como: sudo bash install.sh
# ============================================================

set -e

if [ "$EUID" -ne 0 ]; then
    echo "❌ Por favor ejecuta este script con sudo: sudo bash install.sh"
    exit 1
fi

REAL_USER="${SUDO_USER:-$USER}"
USER_HOME=$(getent passwd "$REAL_USER" | cut -d: -f6)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="/opt/famapos"

echo "=========================================================="
echo "   Iniciando Instalación de FaMa POS en LMDE 7"
echo "   Usuario de destino: $REAL_USER ($USER_HOME)"
echo "=========================================================="

# 1. Crear directorios en /opt/famapos
echo "[1/6] Creando estructura de directorios en $APP_DIR..."
mkdir -p "$APP_DIR/logs"
mkdir -p "$APP_DIR/backups"
mkdir -p "$APP_DIR/images"

# 2. Copiar archivos
echo "[2/6] Copiando binarios y recursos..."
cp "$SCRIPT_DIR/run.sh" "$APP_DIR/run.sh"
cp "$SCRIPT_DIR/backup.sh" "$APP_DIR/backup.sh"

# Copiar JAR compilado si existe
if [ -f "$SCRIPT_DIR/../target/famapos-0.0.1-SNAPSHOT.jar" ]; then
    cp "$SCRIPT_DIR/../target/famapos-0.0.1-SNAPSHOT.jar" "$APP_DIR/famapos.jar"
elif [ -f "$SCRIPT_DIR/famapos.jar" ]; then
    cp "$SCRIPT_DIR/famapos.jar" "$APP_DIR/famapos.jar"
fi

# Copiar logotipo
if [ -f "$SCRIPT_DIR/../src/main/resources/images/Vanuber_Logo.png" ]; then
    cp "$SCRIPT_DIR/../src/main/resources/images/Vanuber_Logo.png" "$APP_DIR/images/Vanuber_Logo.png"
fi

chmod +x "$APP_DIR/run.sh"
chmod +x "$APP_DIR/backup.sh"

# 3. Permisos de hardware (Impresora térmica y cajón en /dev/usb/lp0)
echo "[3/6] Configurando permisos de hardware para $REAL_USER..."
usermod -aG lp "$REAL_USER" || true
usermod -aG dialout "$REAL_USER" || true

# Regla udev para que /dev/usb/lp0 siempre tenga permisos de lectura/escritura
cat << 'EOF' > /etc/udev/rules.d/99-pos-printer.rules
KERNEL=="lp[0-9]*", SUBSYSTEMS=="usb", MODE="0666", GROUP="lp"
EOF
udevadm control --reload-rules 2>/dev/null || true
udevadm trigger 2>/dev/null || true

# 4. Crear acceso directo en el Escritorio y en el Menú de Cinnamon
echo "[4/6] Creando accesos directos..."
DESKTOP_FILE="$APP_DIR/famapos.desktop"
cat << EOF > "$DESKTOP_FILE"
[Desktop Entry]
Version=1.0
Type=Application
Name=FaMa POS
Comment=Punto de Venta Farmacia Manuel
Exec=/opt/famapos/run.sh
Icon=/opt/famapos/images/Vanuber_Logo.png
Terminal=false
StartupNotify=true
Categories=Office;Finance;
EOF

chmod +x "$DESKTOP_FILE"
cp "$DESKTOP_FILE" "/usr/share/applications/famapos.desktop"

# Copiar al Escritorio del usuario si existe la carpeta Desktop/Escritorio
if [ -d "$USER_HOME/Desktop" ]; then
    cp "$DESKTOP_FILE" "$USER_HOME/Desktop/FaMa POS.desktop"
    chown "$REAL_USER:$REAL_USER" "$USER_HOME/Desktop/FaMa POS.desktop"
    chmod +x "$USER_HOME/Desktop/FaMa POS.desktop"
elif [ -d "$USER_HOME/Escritorio" ]; then
    cp "$DESKTOP_FILE" "$USER_HOME/Escritorio/FaMa POS.desktop"
    chown "$REAL_USER:$REAL_USER" "$USER_HOME/Escritorio/FaMa POS.desktop"
    chmod +x "$USER_HOME/Escritorio/FaMa POS.desktop"
fi

# Asignar propietario a /opt/famapos
chown -R "$REAL_USER:$REAL_USER" "$APP_DIR"

# 5. Configurar respaldo automático diario a las 10:00 PM
echo "[5/6] Programando respaldo diario automático (cron)..."
CRON_JOB="0 22 * * * /opt/famapos/backup.sh >> /opt/famapos/logs/cron_backup.log 2>&1"
(crontab -u "$REAL_USER" -l 2>/dev/null | grep -v "/opt/famapos/backup.sh" ; echo "$CRON_JOB") | crontab -u "$REAL_USER" -

echo "[6/6] Finalizado."
echo "=========================================================="
echo "✅ Instalación completada con éxito."
echo "   - Acceso directo creado en el escritorio."
echo "   - Respaldo diario configurado a las 10:00 PM."
echo "   - Permisos de impresora térmica USB otorgados."
echo "=========================================================="
