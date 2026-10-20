-- Datos necesarios para responder solicitudes en CU12

-- Se conserva el motivo cuando el proveedor rechaza una solicitud
ALTER TABLE Solicitud_Ejecucion
ADD COLUMN IF NOT EXISTS motivo_rechazo VARCHAR(500);

-- Fecha en la que el proveedor responde
ALTER TABLE Solicitud_Ejecucion
ADD COLUMN IF NOT EXISTS fecha_respuesta TIMESTAMP;


-- ---------------------------------------------------------
-- NECESIDADES DE SERVICIO DE CADA EVENTO
-- ---------------------------------------------------------

-- Varias solicitudes pueden corresponder a una misma necesidad
-- del evento, aunque se envien a proveedores diferentes

CREATE TABLE IF NOT EXISTS Necesidad_Servicio_Evento (
    id_necesidad_servicio INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    estado VARCHAR(30) DEFAULT 'ABIERTA',
    id_propuesta_seleccionada INT,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_propuesta_seleccionada)
        REFERENCES Propuesta(id_propuesta)
);


-- Relacionamos cada solicitud con la necesidad que busca cubrir
-- Se permite NULL para conservar las solicitudes antiguas

ALTER TABLE Solicitud_Ejecucion
ADD COLUMN IF NOT EXISTS id_necesidad_servicio INT;

ALTER TABLE Solicitud_Ejecucion
ADD CONSTRAINT IF NOT EXISTS fk_solicitud_necesidad_servicio
FOREIGN KEY (id_necesidad_servicio)
REFERENCES Necesidad_Servicio_Evento(id_necesidad_servicio);


-- ---------------------------------------------------------
-- VERSIONES DE PROPUESTAS
-- ---------------------------------------------------------

-- Cada ajuste debe generar una nueva version
-- Las propuestas anteriores se conservan

ALTER TABLE Propuesta
ADD COLUMN IF NOT EXISTS numero_version INT DEFAULT 1;

ALTER TABLE Propuesta
ADD COLUMN IF NOT EXISTS condiciones VARCHAR(1000);

ALTER TABLE Propuesta
ADD COLUMN IF NOT EXISTS fecha_envio TIMESTAMP;

ALTER TABLE Propuesta
ADD COLUMN IF NOT EXISTS vigente BOOLEAN DEFAULT TRUE;

-- Las propuestas anteriores a CU12 reciben inicialmente version 1.
-- Si una solicitud ya tenía varias, asignamos versiones según su ID
-- antes de crear la restricción única.
UPDATE Propuesta p
SET numero_version = (
    SELECT COUNT(*)
    FROM Propuesta anterior
    WHERE anterior.id_solicitud = p.id_solicitud
      AND anterior.id_propuesta <= p.id_propuesta
)
WHERE p.id_solicitud IN (
    SELECT id_solicitud
    FROM Propuesta
    GROUP BY id_solicitud
    HAVING COUNT(*) > COUNT(DISTINCT numero_version)
);

-- Una solicitud no puede tener dos propuestas con igual version

CREATE UNIQUE INDEX IF NOT EXISTS uq_propuesta_version
ON Propuesta(id_solicitud, numero_version);


-- ---------------------------------------------------------
-- APOYO PARA CONVERSACIONES
-- ---------------------------------------------------------

-- Permite asociar un enlace de apoyo a un mensaje
-- sin mezclar los mensajes con las versiones de propuesta

ALTER TABLE Mensaje
ADD COLUMN IF NOT EXISTS adjunto_url VARCHAR(1000);
-- Una solicitud tiene una sola conversacion privada
CREATE UNIQUE INDEX IF NOT EXISTS uq_conversacion_solicitud
ON Conversacion(id_solicitud);