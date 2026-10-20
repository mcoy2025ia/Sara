-- Campos adicionales necesarios para CU11

ALTER TABLE Servicio ADD COLUMN IF NOT EXISTS tipo VARCHAR(100);
ALTER TABLE Servicio ADD COLUMN IF NOT EXISTS cantidad_maxima_personas INT;
ALTER TABLE Servicio ADD COLUMN IF NOT EXISTS ciudad_zona VARCHAR(150);
ALTER TABLE Servicio ADD COLUMN IF NOT EXISTS anticipacion_minima INT DEFAULT 0;

-- La solicitud debe quedar asociada al servicio concreto
ALTER TABLE Solicitud_Ejecucion ADD COLUMN IF NOT EXISTS id_servicio INT;

ALTER TABLE Solicitud_Ejecucion
ADD CONSTRAINT IF NOT EXISTS fk_solicitud_servicio
FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio);

-- Datos que necesitaremos para comprobar horarios contratados
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS fecha_servicio DATE;
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS hora_inicio TIME;
ALTER TABLE Contratacion ADD COLUMN IF NOT EXISTS hora_fin TIME;

-- Disponibilidades publicadas por cada servicio
CREATE TABLE IF NOT EXISTS Disponibilidad_Servicio (
    id_disponibilidad INT PRIMARY KEY AUTO_INCREMENT,
    id_servicio INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(30) DEFAULT 'DISPONIBLE',
    FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_disponibilidad_servicio
ON Disponibilidad_Servicio(id_servicio, fecha, hora_inicio, hora_fin);

-- Imagenes mostradas en la publicacion
CREATE TABLE IF NOT EXISTS Imagen_Servicio (
    id_imagen INT PRIMARY KEY AUTO_INCREMENT,
    id_servicio INT NOT NULL,
    ruta VARCHAR(500) NOT NULL,
    orden_imagen INT DEFAULT 0,
    FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio)
);

-- Preguntas publicas asociadas a un servicio
CREATE TABLE IF NOT EXISTS Pregunta_Servicio (
    id_pregunta INT PRIMARY KEY AUTO_INCREMENT,
    id_servicio INT NOT NULL,
    id_organizador INT NOT NULL,
    pregunta VARCHAR(500) NOT NULL,
    respuesta VARCHAR(500),
    fecha_pregunta TIMESTAMP NOT NULL,
    fecha_respuesta TIMESTAMP,
    estado VARCHAR(30) DEFAULT 'PENDIENTE',
    FOREIGN KEY (id_servicio) REFERENCES Servicio(id_servicio)
);
