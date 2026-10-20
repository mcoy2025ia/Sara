-- Dos cuentas no pueden compartir nombre de usuario.
-- DatabaseConnection comprueba antes que no existan repetidos (sin distinguir
-- mayúsculas) y detiene la apertura con un mensaje claro si los hay; esta
-- migración no borra, renombra ni reasigna datos.
CREATE UNIQUE INDEX IF NOT EXISTS uq_usuario_usuario ON Usuario(usuario);
