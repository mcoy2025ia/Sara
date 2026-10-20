-- Restablece la misma regla de main también en bases que recibieron CU28:
-- una notificación debe tener exactamente un destinatario, usuario o invitado.
-- Valida antes de retirar la restricción anterior. Si hay registros inválidos,
-- la apertura falla sin borrarlos ni quitar la protección existente.
ALTER TABLE Notificacion_interna ADD CONSTRAINT IF NOT EXISTS destinatario_notificacion_cu28
CHECK ((id_usuario IS NOT NULL AND id_invitado IS NULL) OR (id_usuario IS NULL AND id_invitado IS NOT NULL));

ALTER TABLE Notificacion_interna DROP CONSTRAINT IF EXISTS destinatario_notificacion;
ALTER TABLE Notificacion_interna RENAME CONSTRAINT destinatario_notificacion_cu28 TO destinatario_notificacion;
