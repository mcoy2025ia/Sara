-- Misma estructura de disponibilidad definida por CU11; no cambia sus registros.
CREATE TABLE IF NOT EXISTS Disponibilidad_Servicio (
    id_disponibilidad INT PRIMARY KEY AUTO_INCREMENT,
    id_servicio INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(30) DEFAULT 'DISPONIBLE',
    FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio)
);
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS id_disponibilidad INT REFERENCES Disponibilidad_Servicio(id_disponibilidad);
ALTER TABLE Evento ADD COLUMN IF NOT EXISTS id_lugar INT REFERENCES Lugar(id_lugar);

-- Invitado no tiene cuenta Usuario en el modelo actual.
ALTER TABLE Notificacion_interna ALTER COLUMN id_usuario DROP NOT NULL;
ALTER TABLE Notificacion_interna ADD COLUMN IF NOT EXISTS id_invitado INT REFERENCES Invitado(id_invitado);
ALTER TABLE Notificacion_interna ADD COLUMN IF NOT EXISTS id_evento INT REFERENCES Evento(id_evento);
ALTER TABLE Notificacion_interna ADD CONSTRAINT IF NOT EXISTS destinatario_notificacion
CHECK ((id_usuario IS NOT NULL AND id_invitado IS NULL) OR (id_usuario IS NULL AND id_invitado IS NOT NULL));
