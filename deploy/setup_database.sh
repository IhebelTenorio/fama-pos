#!/bin/bash
# ============================================================
# FaMa POS - Inicializador de Base de Datos PostgreSQL
# ============================================================

set -e

DB_NAME="famapos"
DB_USER="famapos_admin"
DB_PASS="Laud9"
DB_PORT="5433" # Cambiar a 5432 si en la máquina destino corre en el puerto estándar

echo "Configurando PostgreSQL para FaMa POS..."

# 1. Crear usuario si no existe
sudo -u postgres psql -p "$DB_PORT" -tc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1 || \
sudo -u postgres psql -p "$DB_PORT" -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASS';"

# 2. Crear base de datos si no existe
sudo -u postgres psql -p "$DB_PORT" -tc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1 || \
sudo -u postgres psql -p "$DB_PORT" -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"

# 3. Otorgar privilegios
sudo -u postgres psql -p "$DB_PORT" -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"

# 4. Cargar esquema inicial desde Provisional.sql si existe
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="$SCRIPT_DIR/../src/main/resources/database/Provisional.sql"

if [ -f "$SQL_FILE" ]; then
    echo "Cargando esquema desde $SQL_FILE..."
    PGPASSWORD="$DB_PASS" psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SQL_FILE" || true
fi

# 5. Insertar operador por defecto si no existe
PGPASSWORD="$DB_PASS" psql -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
INSERT INTO OPERADOR (Username, Password) 
SELECT 'admin', '1234'
WHERE NOT EXISTS (SELECT 1 FROM OPERADOR WHERE Username = 'admin');
" || true

echo "✅ Base de datos configurada con éxito."
