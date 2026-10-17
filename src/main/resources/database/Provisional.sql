-- Así va quedando 13/08/2026 - Actualizado con Categoría

-- 1. Crear tabla PRODUCTO
CREATE TABLE PRODUCTO (
    CodigoBarras VARCHAR(50) NOT NULL,
    Nombre VARCHAR(150) NOT NULL,
    Categoria VARCHAR(100),
    UnidadesDisponibles INT NOT NULL,
    UmbralUnidades INT NOT NULL,
    FechaCaducidadProxima DATE,
    Costo DECIMAL(10, 2) NOT NULL,
    PrecioPublico DECIMAL(10, 2) NOT NULL,
    Laboratorio VARCHAR(150),
    Activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (CodigoBarras)
);

-- 2. Crear tabla TICKET
CREATE TABLE TICKET (
    IDTicket SERIAL NOT NULL, -- Yo tome en cuenta que usamos PostgreSQL, pero si no usamos ese, se cambia el "SERIAL" por "AUTO_INCREMENT"
    Hora TIME NOT NULL,
    Fecha DATE NOT NULL,
    Total DECIMAL(10, 2) NOT NULL,
    MontoRecibido DECIMAL(10, 2) NOT NULL,
    Cambio DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (IDTicket)
);

-- 3. Crear tabla COMPUESTOGRAMAJE
CREATE TABLE COMPUESTOGRAMAJE (
    CodigoBarras VARCHAR(50) NOT NULL,
    CompuestoGramaje VARCHAR(100) NOT NULL,
    PRIMARY KEY (CodigoBarras, CompuestoGramaje)
);

-- 4. Crear tabla COMPRAR
CREATE TABLE COMPRAR (
    CodigoBarras VARCHAR(50) NOT NULL,
    IDTicket INT NOT NULL,
    Cantidad INT NOT NULL,
    PrecioVenta DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (CodigoBarras, IDTicket)
);


-- Aqui son puros ALTER TABLE, para agregar las ccaracterísticas necesarias de cada tabla
-- 1. Relación de COMPUESTOGRAMAJE con PRODUCTO
ALTER TABLE COMPUESTOGRAMAJE
ADD CONSTRAINT FK_Compuesto_Producto
FOREIGN KEY (CodigoBarras) REFERENCES PRODUCTO(CodigoBarras)
ON DELETE CASCADE 
ON UPDATE CASCADE;

-- 2. Relación de COMPRAR con PRODUCTO
ALTER TABLE COMPRAR
ADD CONSTRAINT FK_Comprar_Producto
FOREIGN KEY (CodigoBarras) REFERENCES PRODUCTO(CodigoBarras)
ON DELETE RESTRICT 
ON UPDATE CASCADE;

-- 3. Relación de COMPRAR con TICKET
ALTER TABLE COMPRAR
ADD CONSTRAINT FK_Comprar_Ticket
FOREIGN KEY (IDTicket) REFERENCES TICKET(IDTicket)
ON DELETE CASCADE 
ON UPDATE CASCADE;
-- 5. Crear tabla OPERADOR (Para el usuario único)
CREATE TABLE OPERADOR (
    Id SERIAL PRIMARY KEY,
    Username VARCHAR(50) NOT NULL,
    Password VARCHAR(255) NOT NULL
);
-- INSERT INTO OPERADOR (Username, Password) VALUES ('admin', '1234');
