#!/bin/bash
# ============================================================
# FaMa POS - Respaldo Automático de Base de Datos
# ============================================================

BACKUP_DIR="/opt/famapos/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/famapos_backup_$TIMESTAMP.sql.gz"
DB_PORT="5433" # Ajustar si usa puerto 5432

mkdir -p "$BACKUP_DIR"

echo "Iniciando respaldo de FaMa POS a las $(date)..."
PGPASSWORD="Laud9" pg_dump -h localhost -p "$DB_PORT" -U famapos_admin famapos | gzip > "$BACKUP_FILE"

if [ -f "$BACKUP_FILE" ]; then
    echo "Respaldo exitoso: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"
else
    echo "ERROR: Falló la creación del respaldo" >&2
    exit 1
fi

# Eliminar respaldos de más de 30 días para no saturar disco
find "$BACKUP_DIR" -type f -name "famapos_backup_*.sql.gz" -mtime +30 -delete
