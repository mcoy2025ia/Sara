-- Un usuario solo puede tener un perfil de organizador.
-- DatabaseConnection comprueba antes que no existan repetidos y detiene la apertura
-- con un mensaje claro si los hay; esta migración no borra ni reasigna datos.
CREATE UNIQUE INDEX IF NOT EXISTS uq_organizador_usuario ON Organizador(id_usuario);
