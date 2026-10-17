package com.van_uber.famapos;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Migración automática y segura de esquema para PostgreSQL.
 * Agrega columnas requeridas si no existen sin alterar tablas ni perder datos.
 */
@Component
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void runMigrations() {
        log.info("Verificando y aplicando migraciones de base de datos...");
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Agregar columna Categoria a la tabla PRODUCTO si no existe
            stmt.execute("ALTER TABLE PRODUCTO ADD COLUMN IF NOT EXISTS Categoria VARCHAR(100);");
            log.info("Migración aplicada exitosamente: Columna 'Categoria' asegurada en la tabla PRODUCTO.");

        } catch (Exception e) {
            log.error("Error al aplicar la migración de base de datos: {}", e.getMessage(), e);
        }
    }
}
