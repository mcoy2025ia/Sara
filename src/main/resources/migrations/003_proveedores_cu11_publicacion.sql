-- Completa la persistencia de imagenes y preguntas publicas de CU11

ALTER TABLE Pregunta_Servicio
ADD CONSTRAINT IF NOT EXISTS fk_pregunta_organizador
FOREIGN KEY (id_organizador) REFERENCES Organizador(id_organizador);

CREATE INDEX IF NOT EXISTS idx_imagen_servicio
ON Imagen_Servicio(id_servicio);

CREATE INDEX IF NOT EXISTS idx_pregunta_servicio
ON Pregunta_Servicio(id_servicio);
