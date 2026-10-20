-- CU20: Segmentar clientes automáticamente.

-- Datos del cliente usados para agrupar (país, ciudad y edad).
ALTER TABLE Usuario ADD COLUMN IF NOT EXISTS pais VARCHAR(80);
ALTER TABLE Usuario ADD COLUMN IF NOT EXISTS ciudad VARCHAR(80);
ALTER TABLE Usuario ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;

-- Cliente que solicitó o contrató el evento.
ALTER TABLE Evento ADD COLUMN IF NOT EXISTS id_cliente INT;
ALTER TABLE Evento ADD CONSTRAINT IF NOT EXISTS fk_evento_cliente
    FOREIGN KEY (id_cliente) REFERENCES Usuario(id_usuario);

-- Tipos de evento que busca cada cliente (mismo formato que Evento.tipo).
CREATE TABLE IF NOT EXISTS Busqueda_evento (
    id_busqueda INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    fecha_busqueda TIMESTAMP NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

-- Clientes que pertenecen a cada segmento.
CREATE TABLE IF NOT EXISTS Segmento_cliente (
    id_segmento_cliente INT PRIMARY KEY AUTO_INCREMENT,
    id_segmento INT NOT NULL,
    id_usuario INT NOT NULL,
    FOREIGN KEY (id_segmento) REFERENCES Segmentacion(id_segmento),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    CONSTRAINT uq_segmento_cliente UNIQUE (id_segmento, id_usuario)
);