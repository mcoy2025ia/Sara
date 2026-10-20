-- CU-028: documentos, correcciones y notificaciones internas.
-- Esta migración amplía la persistencia sin eliminar datos existentes.

ALTER TABLE Proveedor
ADD COLUMN IF NOT EXISTS observacion_correccion VARCHAR(500);

-- Documentos asociados a cada proveedor.
CREATE TABLE IF NOT EXISTS Documento_Proveedor (
    id_documento INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    tipo_documento VARCHAR(100) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo VARCHAR(1000) NOT NULL,
    fecha_vencimiento DATE,
    estado VARCHAR(30) NOT NULL DEFAULT 'PRESENTADO',
    observacion VARCHAR(500),
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_documento_proveedor
        FOREIGN KEY (id_proveedor)
        REFERENCES Proveedor(id_proveedor)
);

CREATE INDEX IF NOT EXISTS idx_documento_proveedor
ON Documento_Proveedor(id_proveedor);

-- Notificaciones internas dirigidas al usuario del proveedor.
CREATE TABLE IF NOT EXISTS Notificacion_Proveedor (
    id_notificacion INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario_destinatario INT NOT NULL,
    id_proveedor INT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje VARCHAR(1000) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leida BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_notif_proveedor_usuario_cu28
        FOREIGN KEY (id_usuario_destinatario)
        REFERENCES Usuario(id_usuario),

    CONSTRAINT fk_notif_proveedor_proveedor_cu28
        FOREIGN KEY (id_proveedor)
        REFERENCES Proveedor(id_proveedor)
);

CREATE INDEX IF NOT EXISTS idx_notificacion_proveedor_usuario_fecha
ON Notificacion_Proveedor(id_usuario_destinatario, fecha_creacion);