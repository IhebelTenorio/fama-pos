# 🚀 Guía de Despliegue y Puesta a Punto — FaMa POS en LMDE 7

Esta guía resume los pasos para dejar la computadora de la farmacia lista en **menos de 15 minutos**.

---

## Paso 1: Instalar dependencias base en LMDE 7

Abre una terminal en la computadora y ejecuta:

```bash
# Actualizar repositorios
sudo apt update && sudo apt upgrade -y

# Instalar Java 21, PostgreSQL y herramientas
sudo apt install -y openjdk-21-jre postgresql postgresql-contrib curl git openssh-server
```

---

## Paso 2: Configurar la Base de Datos

Ejecuta el script incluido para crear el usuario, la base de datos y cargar el esquema:

```bash
cd /ruta/hacia/famapos/deploy
chmod +x *.sh
sudo bash setup_database.sh
```

*(Nota: Si en la máquina de destino PostgreSQL corre en el puerto estándar `5432`, asegúrate de que coincida con `application.yaml` o edita la variable `DB_PORT="5432"` en `setup_database.sh` y `application.yaml`).*

---

## Paso 3: Compilar y Ejecutar el Instalador

1. Si aún no has generado el `.jar`:
   ```bash
   mvn clean package
   ```
2. Ejecuta el instalador automático:
   ```bash
   sudo bash install.sh
   ```

Este script automáticamente:
- Copia la aplicación a `/opt/famapos/`.
- Configura las reglas `udev` para que la impresora térmica `/dev/usb/lp0` funcione sin pedir permisos.
- Crea el acceso directo con ícono en el **Escritorio** y en el **Menú de Cinnamon**.
- Programa un respaldo diario en segundo plano a las **10:00 PM**.

---

## Paso 4: Configurar Conexión Remota (Para administrar desde otra ciudad)

### A. Red Privada con Tailscale (Acceso SSH sin abrir puertos)
```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
```
- Inicia sesión con tu cuenta.
- A partir de este momento, podrás conectarte desde tu casa a la farmacia con:
  ```bash
  ssh cajero@IP_TAILSCALE
  ```

### B. Soporte Visual con RustDesk (Opcional pero muy recomendado)
- Descarga el `.deb` de [RustDesk](https://rustdesk.com).
- Instálalo con `sudo dpkg -i rustdesk-*.deb`.
- Ve a **Ajustes > Seguridad > Acceso desatendido** y define una contraseña fija.
- Con esto puedes ver la pantalla del usuario en tiempo real cuando te pida apoyo.

---

## Paso 5: Modo Punto de Venta Automático (Opcional)

Para que el cajero no tenga que hacer nada al llegar en la mañana:
1. En el menú de Linux Mint, ve a **"Ventana de inicio de sesión" (Login Window)** y activa **"Inicio de sesión automático"** para el usuario.
2. Abre **"Aplicaciones al inicio" (Startup Applications)**, haz clic en **+ > Aplicación personalizada**, y selecciona `FaMa POS` (`/opt/famapos/run.sh`).
3. Al encender la PC, el sistema iniciará sesión solo y abrirá FaMa POS directamente.
