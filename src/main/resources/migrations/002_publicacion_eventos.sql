CREATE TABLE IF NOT EXISTS Solicitud_publicacion (
    id_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    motivo VARCHAR(500),
    id_administrador INT,
    fecha_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_revision TIMESTAMP,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_administrador) REFERENCES Usuario(id_usuario)
);

-- Solo datos de la plantilla: nunca se copian invitados, importes ni texto libre.
CREATE TABLE IF NOT EXISTS Plantilla_evento (
    id_plantilla INT PRIMARY KEY AUTO_INCREMENT,
    id_solicitud INT NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(100),
    duracion_minutos BIGINT NOT NULL CHECK (duracion_minutos >= 0),
    FOREIGN KEY (id_solicitud) REFERENCES Solicitud_publicacion(id_solicitud)
);

CREATE TABLE IF NOT EXISTS Notificacion_interna (
    id_notificacion INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);
