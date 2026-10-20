CREATE TABLE IF NOT EXISTS Finalizacion_evento (
    id_evento INT PRIMARY KEY REFERENCES Evento(id_evento),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviada BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS Fotografia_evento (
    id_fotografia INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL REFERENCES Finalizacion_evento(id_evento),
    nombre VARCHAR(150) NOT NULL,
    contenido BLOB NOT NULL
);
-- Destinatarios confirmados al iniciar el cierre; se conserva la selección al reintentar.
CREATE TABLE IF NOT EXISTS Entrega_galeria (
    id_evento INT NOT NULL REFERENCES Finalizacion_evento(id_evento),
    id_invitado INT NOT NULL REFERENCES Invitado(id_invitado),
    PRIMARY KEY (id_evento, id_invitado)
);
