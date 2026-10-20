
-- CU-026: Perfil y contratacion de organizadores.

-- Datos necesarios para buscar y comparar organizadores.
ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS ciudad VARCHAR(150);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS especialidad VARCHAR(150);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS portafolio VARCHAR(500);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS tarifa_base DECIMAL(12,2);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS estado_verificacion
    VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE';


-- Solicitudes de contratacion de organizadores.
-- Se mantiene separada de Contratacion, que ya gestiona servicios.
CREATE TABLE IF NOT EXISTS Solicitud_Contratacion_Organizador (
    id_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    id_cliente INT NOT NULL,
    id_organizador INT NOT NULL,
    tipo_evento VARCHAR(100) NOT NULL,
    ciudad VARCHAR(150) NOT NULL,
    fecha_aproximada DATE NOT NULL,
    presupuesto_estimado DECIMAL(12,2) NOT NULL,
    criterios_busqueda VARCHAR(500),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_cu26_presupuesto
        CHECK (presupuesto_estimado >= 0),

    CONSTRAINT fk_cu26_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES Usuario(id_usuario),

    CONSTRAINT fk_cu26_organizador
        FOREIGN KEY (id_organizador)
        REFERENCES Organizador(id_organizador)
);

CREATE INDEX IF NOT EXISTS idx_cu26_cliente
    ON Solicitud_Contratacion_Organizador(id_cliente);

CREATE INDEX IF NOT EXISTS idx_cu26_organizador_estado
    ON Solicitud_Contratacion_Organizador(id_organizador, estado);


-- Resenas dirigidas especificamente a organizadores.
-- No se reutilizan las resenas de contrataciones de proveedores.
CREATE TABLE IF NOT EXISTS Resena_Organizador (
    id_resena_organizador INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    id_cliente INT NOT NULL,
    calificacion INT NOT NULL
        CHECK (calificacion BETWEEN 1 AND 5),
    comentario VARCHAR(500),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cu26_resena_organizador
        FOREIGN KEY (id_organizador)
        REFERENCES Organizador(id_organizador),

    CONSTRAINT fk_cu26_resena_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES Usuario(id_usuario)
);

CREATE INDEX IF NOT EXISTS idx_cu26_resenas
    ON Resena_Organizador(id_organizador);


-- CU-026: permitir notificaciones dirigidas a organizadores.
-- Una notificacion de organizador no necesita un proveedor asociado.

ALTER TABLE Notificacion_interna
    ADD COLUMN IF NOT EXISTS id_usuario_destinatario INT
    REFERENCES Usuario(id_usuario);

ALTER TABLE Notificacion_interna
    ADD COLUMN IF NOT EXISTS id_proveedor INT
    REFERENCES Proveedor(id_proveedor);

ALTER TABLE Notificacion_interna
    ALTER COLUMN id_proveedor DROP NOT NULL;

ALTER TABLE Notificacion_interna
    ALTER COLUMN id_usuario_destinatario DROP NOT NULL;
