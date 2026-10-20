-- =========================================================
-- DATOS DE PRUEBA
-- =========================================================

-- ---------------------------------------------------------
-- PERMISOS
-- ---------------------------------------------------------

INSERT INTO Permiso (id_permiso, nombre, descripcion) VALUES
(1, 'CREAR_EVENTO', 'Permite crear eventos'),
(2, 'GESTIONAR_SERVICIOS', 'Permite gestionar servicios'),
(3, 'GESTIONAR_INVITACIONES', 'Permite gestionar invitaciones'),
(4, 'APROBAR_PROVEEDORES', 'Permite aprobar proveedores');


-- ---------------------------------------------------------
-- ROLES
-- ---------------------------------------------------------

INSERT INTO Rol (id_rol, nombre, descripcion, estado) VALUES
(1, 'ADMINISTRADOR', 'Administrador general del sistema', 'ACTIVO'),
(2, 'ORGANIZADOR', 'Usuario encargado de gestionar eventos', 'ACTIVO'),
(3, 'PROVEEDOR', 'Usuario que ofrece servicios para eventos', 'ACTIVO'),
(4, 'CLIENTE', 'Usuario que solicita y participa en la gestión de eventos', 'ACTIVO');


-- ---------------------------------------------------------
-- USUARIOS
-- ---------------------------------------------------------

INSERT INTO Usuario
(id_usuario, nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario)
VALUES
(1, 'Laura Gómez', 'laura.gomez', 'clave123', 'ACTIVO', TIMESTAMP '2026-10-01 09:00:00', TRUE, 'ORGANIZADOR'),
(2, 'Carlos Rodríguez', 'carlos.rodriguez', 'clave456', 'ACTIVO', TIMESTAMP '2026-10-02 10:30:00', TRUE, 'ORGANIZADOR'),
(3, 'Decoraciones Arcoíris', 'decoraciones.arcoiris', 'clave789', 'ACTIVO', TIMESTAMP '2026-10-03 11:00:00', TRUE, 'PROVEEDOR'),
(4, 'Sabores & Eventos', 'sabores.eventos', 'clave321', 'ACTIVO', TIMESTAMP '2026-10-03 14:00:00', FALSE, 'PROVEEDOR'),
(5, 'Ana Martínez', 'ana.martinez', 'clave654', 'ACTIVO', TIMESTAMP '2026-10-04 08:30:00', TRUE, 'CLIENTE'),
(6, 'Diego Pérez', 'diego.perez', 'clave987', 'ACTIVO', TIMESTAMP '2026-10-05 16:00:00', FALSE, 'CLIENTE'),
(7, 'Administrador NextEventux', 'admin.nexteventux', 'claveadmin', 'ACTIVO', TIMESTAMP '2026-10-18 09:00:00', FALSE, 'ADMINISTRADOR'),
(8, 'Valentina Flores', 'valentina', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'),
(9, 'Santiago Rivera', 'santiago', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'),
(10, 'Juliana Torres', 'juliana', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'),
(11, 'Luna Decoraciones', 'luna', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR'),
(12, 'Sabor & Fiesta Catering', 'sabor', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR'),
(13, 'Flash Studio Fotografía', 'flash', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR'),
(14, 'Camila Reyes', 'camila', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ADMINISTRADOR'),
(15, 'Mateo Castillo', 'mateo', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ADMINISTRADOR'),
(16, 'Shirly Montenegro', 'shirly', '1234', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ADMINISTRADOR');

-- ---------------------------------------------------------
-- ROLES Y PERMISOS
-- ---------------------------------------------------------

INSERT INTO Rol_permiso
(id_rol_permiso, id_rol, id_permiso)
VALUES
(1, 1, 1),
(2, 1, 2),
(3, 1, 3),
(4, 1, 4),
(5, 2, 1),
(6, 2, 3);


-- ---------------------------------------------------------
-- USUARIOS Y ROLES
-- ---------------------------------------------------------

INSERT INTO Usuario_rol
(id_usuario_rol, id_usuario, id_rol)
VALUES
(1, 1, 2),
(2, 2, 2),
(3, 3, 3),
(4, 4, 3),
(5, 5, 4),
(6, 6, 4),
(7, 7, 1),
(8, 8, 2),
(9, 9, 2),
(10, 10, 2),
(11, 11, 3),
(12, 12, 3),
(13, 13, 3),
(14, 14, 1),
(15, 15, 1),
(16, 16, 1);


-- ---------------------------------------------------------
-- LUGARES
-- ---------------------------------------------------------

INSERT INTO Lugar
(id_lugar, nombre, direccion, capacidad, descripcion, estado)
VALUES
(1, 'Salón Primavera', 'Calle 100 #15-20', 150, 'Salón para celebraciones sociales', 'DISPONIBLE'),
(2, 'Jardín Los Pinos', 'Carrera 7 #120-15', 250, 'Espacio al aire libre para eventos', 'DISPONIBLE'),
(3, 'Salón Mirador', 'Calle 80 #10-45', 80, 'Salón para eventos pequeños', 'DISPONIBLE');


-- ---------------------------------------------------------
-- ORGANIZADORES
-- ---------------------------------------------------------

INSERT INTO Organizador
(id_organizador, id_usuario, empresa, telefono, estado,
 ciudad, especialidad, portafolio, tarifa_base, estado_verificacion)
VALUES
(1, 1, 'Eventos Laura', '3001234567', 'ACTIVO',
 'Bogota', 'Bodas y eventos sociales',
 'Organizacion de bodas y celebraciones sociales',
 1500000.00, 'VERIFICADO'),

(2, 2, 'Celebraciones CR', '3017654321', 'ACTIVO',
 'Bogota', 'Celebraciones familiares',
 'Organizacion de reuniones y celebraciones familiares',
 1000000.00, 'VERIFICADO'),

(3, 8, 'Momentos Valentina', '3005550101', 'ACTIVO',
 'Bogota', 'Eventos sociales',
 'Organizacion de eventos sociales y familiares',
 900000.00, 'VERIFICADO'),

(4, 9, 'Eventos Rivera', '3005550102', 'ACTIVO',
 'Medellin', 'Eventos empresariales',
 'Organizacion de encuentros y eventos empresariales',
 1200000.00, 'VERIFICADO'),

(5, 10, 'Sueños de Juliana', '3005550103', 'ACTIVO',
 'Bogota', 'Eventos personalizados',
 'Organizacion de celebraciones personalizadas',
 1100000.00, 'VERIFICADO');

-- ---------------------------------------------------------
-- RESENAS DE ORGANIZADORES PARA CU-026
-- ---------------------------------------------------------

INSERT INTO Resena_Organizador
(id_organizador, id_cliente, calificacion, comentario)
VALUES
(1, 5, 5, 'Buena organizacion y comunicacion durante el evento.'),
(2, 6, 4, 'La celebracion estuvo bien organizada.'),
(3, 5, 5, 'Excelente atencion y organizacion.'),
(4, 6, 4, 'Buen servicio para eventos empresariales.'),
(5, 5, 5, 'Una celebracion muy bien organizada.');

-- ---------------------------------------------------------
-- PROVEEDORES
-- ---------------------------------------------------------

INSERT INTO Proveedor
(id_proveedor, id_usuario, nombre, empresa, correo, telefono, direccion, estado)
VALUES
(1, 3, 'Mariana López', 'Decoraciones Arcoíris', 'contacto@arcoiris.com', '3101234567', 'Carrera 15 #80-20', 'APROBADO'),
(2, 4, 'Andrés Torres', 'Sabores & Eventos', 'contacto@saboreseventos.com', '3117654321', 'Calle 90 #20-30', 'APROBADO'),
(3, 11, 'Luna Decoraciones', 'Luna Decoraciones', 'luna@ejemplo.com', '3005550201', 'Bogotá', 'APROBADO'),
(4, 12, 'Sabor & Fiesta Catering', 'Sabor & Fiesta Catering', 'sabor@ejemplo.com', '3005550202', 'Bogotá', 'APROBADO'),
(5, 13, 'Flash Studio Fotografía', 'Flash Studio Fotografía', 'flash@ejemplo.com', '3005550203', 'Bogotá', 'APROBADO');


-- ---------------------------------------------------------
-- EVENTOS
-- ---------------------------------------------------------

INSERT INTO Evento
(id_evento, id_organizador, nombre, descripcion, fecha_inicio, fecha_fin, ubicacion, estado, fecha_creacion, tipo, presupuesto, invitados_iniciales)
VALUES
(1, 1, 'Boda Laura y Daniel', 'Celebración de matrimonio', TIMESTAMP '2026-11-15 15:00:00', TIMESTAMP '2026-11-15 23:00:00', 'Salón Primavera', 'PLANEADO', TIMESTAMP '2026-10-06 09:00:00', 'Boda', 15000000.00, 120),
(2, 2, 'Baby Shower Valentina', 'Celebración de bienvenida para bebé', TIMESTAMP '2026-12-05 14:00:00', TIMESTAMP '2026-12-05 19:00:00', 'Jardín Los Pinos', 'PLANEADO', TIMESTAMP '2026-10-07 10:00:00', 'Baby shower', 5000000.00, 60);


-- ---------------------------------------------------------
-- PLANIFICACIONES
-- ---------------------------------------------------------

INSERT INTO Planificacion
(id_planificacion, id_evento, version, descripcion, estado)
VALUES
(1, 1, 1, 'Primera propuesta de planificación de la boda', 'BORRADOR'),
(2, 1, 2, 'Segunda propuesta con ajustes de decoración', 'ACTIVA'),
(3, 2, 1, 'Planificación inicial del baby shower', 'ACTIVA');


-- ---------------------------------------------------------
-- INVITACIONES
-- ---------------------------------------------------------

INSERT INTO Invitacion
(id_invitacion, id_evento, id_lugar, fecha_envio, mensaje, estado_respuesta)
VALUES
(1, 1, 1, TIMESTAMP '2026-10-20 10:00:00', 'Te invitamos a celebrar con nosotros este día especial.', 'ENVIADA'),
(2, 1, 1, TIMESTAMP '2026-10-20 10:15:00', 'Esperamos contar contigo en nuestra boda.', 'ENVIADA'),
(3, 2, 2, TIMESTAMP '2026-11-01 09:00:00', 'Acompáñanos en el baby shower de Valentina.', 'ENVIADA');


-- ---------------------------------------------------------
-- INVITADOS
-- ---------------------------------------------------------

INSERT INTO Invitado
(id_invitado, id_invitacion, nombre, telefono, direccion, estado, correo)
VALUES
(1, 1, 'Sofía Cortés', '3005551111', 'Calle 50 #10-20', 'CONFIRMADO', 'sofia@email.com'),
(2, 2, 'Juan García', '3005552222', 'Carrera 20 #30-40', 'PENDIENTE', 'juan@email.com'),
(3, 3, 'Camila Torres', '3005553333', 'Calle 70 #15-25', 'CONFIRMADO', 'camila@email.com'),
(4, 3, 'Natalia Ruiz', '3005554444', 'Carrera 12 #45-60', 'PENDIENTE', 'natalia@email.com'),
(5, 1, 'Emilia Navarro', '3005550301', 'Bogotá', 'CONFIRMADO', 'emilia@ejemplo.com'),
(6, 2, 'Nicolás Vega', '3005550302', 'Bogotá', 'PENDIENTE', 'nicolas@ejemplo.com'),
(7, 3, 'Renata Morales', '3005550303', 'Bogotá', 'CONFIRMADO', 'renata@ejemplo.com');


-- ---------------------------------------------------------
-- ACOMPAÑANTES
-- ---------------------------------------------------------

INSERT INTO Acompanante
(id_acompanante, id_invitado, nombre, telefono, tipo)
VALUES
(1, 1, 'Daniel Cortés', '3006661111', 'PAREJA'),
(2, 3, 'Mateo Torres', '3006662222', 'PAREJA'),
(3, 4, 'Laura Ruiz', '3006663333', 'FAMILIAR');


-- ---------------------------------------------------------
-- NECESIDADES ESPECIALES
-- ---------------------------------------------------------

INSERT INTO Necesidad_Especial
(id_necesidad, id_invitado, descripcion, estado)
VALUES
(1, 3, 'Espacio accesible para silla de ruedas', 'REGISTRADA'),
(2, 4, 'Preferencia por menú sin frutos secos', 'REGISTRADA');


-- ---------------------------------------------------------
-- REGALOS
-- ---------------------------------------------------------

INSERT INTO Regalo
(id_regalo, id_invitacion, nombre, descripcion, estado, precio_estimado)
VALUES
(1, 3, 'Pañitos para bebé', 'Paquete de pañitos húmedos', 'DISPONIBLE', 25000.00),
(2, 3, 'Kit de biberones', 'Kit de biberones para recién nacido', 'DISPONIBLE', 80000.00),
(3, 3, 'Cobija para bebé', 'Cobija suave para recién nacido', 'DISPONIBLE', 60000.00),
(4, 3, 'Ropa de bebé', 'Conjunto de ropa para recién nacido', 'DISPONIBLE', 50000.00);


-- ---------------------------------------------------------
-- RESERVAS DE REGALOS
-- ---------------------------------------------------------

INSERT INTO Reserva_regalo
(id_reserva, id_evento, id_invitado, id_regalo, fecha_reserva, estado)
VALUES
(1, 2, 3, 1, TIMESTAMP '2026-11-10 12:00:00', 'RESERVADO'),
(2, 2, 4, 1, TIMESTAMP '2026-11-11 15:30:00', 'RESERVADO'),
(3, 2, 3, 2, TIMESTAMP '2026-11-12 09:00:00', 'RESERVADO');


-- ---------------------------------------------------------
-- SERVICIOS
-- ---------------------------------------------------------

INSERT INTO Servicio
(id_servicio, id_proveedor, nombre, descripcion, precio, categoria, estado)
VALUES
(1, 1, 'Decoración floral', 'Decoración floral para eventos sociales', 1200000.00, 'DECORACION', 'DISPONIBLE'),
(2, 1, 'Decoración temática', 'Decoración personalizada según temática', 900000.00, 'DECORACION', 'DISPONIBLE'),
(3, 2, 'Catering básico', 'Servicio de alimentación para eventos', 1800000.00, 'ALIMENTACION', 'DISPONIBLE'),
(4, 2, 'Mesa de postres', 'Postres variados para celebraciones', 650000.00, 'ALIMENTACION', 'DISPONIBLE');


-- ---------------------------------------------------------
-- PAQUETES DE SERVICIO
-- ---------------------------------------------------------

INSERT INTO Paquete_servicio
(id_paquete, id_proveedor, nombre, descripcion, precio, categoria, estado)
VALUES
(1, 1, 'Paquete Boda Clásica', 'Decoración floral y decoración de mesas', 1800000.00, 'DECORACION', 'DISPONIBLE'),
(2, 2, 'Paquete Celebración', 'Catering y mesa de postres', 2200000.00, 'ALIMENTACION', 'DISPONIBLE');


-- ---------------------------------------------------------
-- SOLICITUDES DE EJECUCIÓN
-- ---------------------------------------------------------

INSERT INTO Solicitud_Ejecucion
(id_solicitud, id_proveedor, id_evento, fecha_solicitud, estado, id_servicio)
VALUES
(1, 1, 1, TIMESTAMP '2026-10-08 10:00:00', 'RESPONDIDA', 1),
(2, 2, 1, TIMESTAMP '2026-10-08 11:30:00', 'RESPONDIDA', 3),
(3, 1, 2, TIMESTAMP '2026-10-09 09:00:00', 'PENDIENTE', 2);

-- CU-12: cada solicitud corresponde a una necesidad del evento; sin ella el
-- organizador no puede seleccionar las propuestas de los datos de ejemplo.
INSERT INTO Necesidad_Servicio_Evento
(id_necesidad_servicio, id_evento, descripcion, estado)
VALUES
(1, 1, 'Decoración floral del evento', 'ABIERTA'),
(2, 1, 'Banquete para los invitados', 'ABIERTA'),
(3, 2, 'Decoración temática del evento', 'ABIERTA');

UPDATE Solicitud_Ejecucion SET id_necesidad_servicio = id_solicitud
WHERE id_solicitud IN (1, 2, 3);

ALTER TABLE Necesidad_Servicio_Evento ALTER COLUMN id_necesidad_servicio RESTART WITH 4;


-- ---------------------------------------------------------
-- PROPUESTAS
-- ---------------------------------------------------------

INSERT INTO Propuesta
(id_propuesta, id_solicitud, valor, descripcion, estado)
VALUES
(1, 1, 1800000.00, 'Decoración completa para la boda', 'ACEPTADA'),
(2, 2, 2200000.00, 'Servicio de catering y postres', 'ACEPTADA'),
(3, 3, 900000.00, 'Decoración temática para baby shower', 'PENDIENTE');


-- ---------------------------------------------------------
-- CONTRATACIONES
-- ---------------------------------------------------------

INSERT INTO Contratacion
(id_contratacion, id_propuesta, id_evento, fecha_contratacion, estado, condiciones, valor_acordado)
VALUES
(1, 1, 1, TIMESTAMP '2026-10-10 14:00:00', 'ACTIVA', 'Montaje terminado tres horas antes del evento', 1800000.00),
(2, 2, 1, TIMESTAMP '2026-10-10 15:00:00', 'ACTIVA', 'Servicio de alimentación para 120 personas', 2200000.00);


-- ---------------------------------------------------------
-- MOVIMIENTOS FINANCIEROS
-- ---------------------------------------------------------

INSERT INTO Movimiento_financiero
(id_movimiento, id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado)
VALUES
(1, 1, 1, 1, 900000.00, TIMESTAMP '2026-10-11 09:00:00', 'Anticipo de decoración', 'EGRESO', 'REGISTRADO'),
(2, 1, 1, 1, 900000.00, TIMESTAMP '2026-11-15 23:30:00', 'Pago final de decoración', 'EGRESO', 'PENDIENTE'),
(3, 2, 1, 2, 1100000.00, TIMESTAMP '2026-10-12 10:00:00', 'Anticipo de catering', 'EGRESO', 'REGISTRADO'),
(4, 2, 1, 2, 1100000.00, TIMESTAMP '2026-11-15 23:30:00', 'Pago final de catering', 'EGRESO', 'PENDIENTE');


-- ---------------------------------------------------------
-- RESEÑAS
-- ---------------------------------------------------------

INSERT INTO Resena
(id_resena, id_contratacion, calificacion, comentario, fecha)
VALUES
(1, 1, 5, 'La decoración quedó exactamente como la esperábamos.', TIMESTAMP '2026-11-16 10:00:00'),
(2, 2, 4, 'El servicio de catering fue muy bueno.', TIMESTAMP '2026-11-16 11:00:00');


-- ---------------------------------------------------------
-- CONVERSACIONES
-- ---------------------------------------------------------

INSERT INTO Conversacion
(id_conversacion, id_solicitud, fecha_creacion, estado)
VALUES
(1, 1, TIMESTAMP '2026-10-08 10:30:00', 'ACTIVA'),
(2, 2, TIMESTAMP '2026-10-08 12:00:00', 'ACTIVA');


-- ---------------------------------------------------------
-- MENSAJES
-- ---------------------------------------------------------

INSERT INTO Mensaje
(id_mensaje, id_conversacion, id_usuario_remitente, contenido, fecha_hora)
VALUES
(1, 1, 1, 'Hola, quisiera confirmar si pueden incluir flores blancas.', TIMESTAMP '2026-10-08 10:35:00'),
(2, 1, 3, 'Sí, podemos incluir flores blancas sin problema.', TIMESTAMP '2026-10-08 10:40:00'),
(3, 2, 1, '¿El servicio de catering incluye bebidas?', TIMESTAMP '2026-10-08 12:10:00'),
(4, 2, 4, 'Sí, incluye bebidas no alcohólicas.', TIMESTAMP '2026-10-08 12:15:00');


-- ---------------------------------------------------------
-- RESPUESTAS A RESEÑAS
-- ---------------------------------------------------------

INSERT INTO Respuesta_resena
(id_respuesta, id_resena, id_proveedor, comentario, fecha)
VALUES
(1, 1, 1, 'Muchas gracias por confiar en nuestro trabajo.', TIMESTAMP '2026-11-16 12:00:00'),
(2, 2, 2, 'Nos alegra saber que disfrutaron el servicio.', TIMESTAMP '2026-11-16 13:00:00');


-- ---------------------------------------------------------
-- SANCIONES
-- ---------------------------------------------------------

INSERT INTO Sancion
(id_sancion, id_evento, motivo, valor, estado, fecha)
VALUES
(1, 1, 'Incumplimiento de una condición del evento', 100000.00, 'PENDIENTE', TIMESTAMP '2026-11-16 14:00:00'),
(2, 2, 'Cancelación tardía de un servicio', 75000.00, 'PAGADA', TIMESTAMP '2026-11-16 15:00:00');


-- ---------------------------------------------------------
-- SEGMENTACIONES
-- ---------------------------------------------------------

INSERT INTO Segmentacion
(id_segmento, id_usuario, nombre, criterios, descripcion, estados)
VALUES
(1, 1, 'Clientes frecuentes', 'Más de 3 eventos registrados', 'Clientes con alta frecuencia de uso', 'ACTIVA'),
(2, 1, 'Eventos familiares', 'Tipo de evento familiar', 'Clientes interesados en eventos familiares', 'ACTIVA'),
(3, 2, 'Clientes nuevos', 'Primer evento registrado', 'Clientes que utilizan el servicio por primera vez', 'ACTIVA');


-- ---------------------------------------------------------
-- CAMPAÑAS
-- ---------------------------------------------------------

INSERT INTO Campana
(id_campana, id_proveedor, nombre, descripcion, fecha_inicio, fecha_fin)
VALUES
(1, 1, 'Decoración de fin de año', 'Promoción de servicios de decoración', TIMESTAMP '2026-11-01 00:00:00', TIMESTAMP '2026-12-31 23:59:59'),
(2, 2, 'Menús para celebraciones', 'Promoción de paquetes de alimentación', TIMESTAMP '2026-10-15 00:00:00', TIMESTAMP '2026-12-15 23:59:59');


-- ---------------------------------------------------------
-- ENVÍOS DE CAMPAÑA
-- ---------------------------------------------------------

INSERT INTO Envio_Campana
(id_envio, id_campana, id_usuario, fecha_solicitud, estado)
VALUES
(1, 1, 5, TIMESTAMP '2026-11-02 09:00:00', 'ENVIADO'),
(2, 1, 6, TIMESTAMP '2026-11-02 09:05:00', 'ENVIADO'),
(3, 2, 5, TIMESTAMP '2026-10-16 10:00:00', 'ENVIADO'),
(4, 2, 6, TIMESTAMP '2026-10-16 10:05:00', 'PENDIENTE');


-- ---------------------------------------------------------
-- PUNTOS DE FIDELIDAD
-- ---------------------------------------------------------

INSERT INTO Puntos_fidelidad
(id_puntos, id_usuario, cantidad, fecha_actualizacion)
VALUES
(1, 5, 350, TIMESTAMP '2026-10-10 18:00:00'),
(2, 6, 120, TIMESTAMP '2026-10-10 18:00:00');

-- Continuar los identificadores automáticos después de los datos de ejemplo.
ALTER TABLE Permiso ALTER COLUMN id_permiso RESTART WITH 5;
ALTER TABLE Rol ALTER COLUMN id_rol RESTART WITH 5;
ALTER TABLE Usuario ALTER COLUMN id_usuario RESTART WITH 17;
ALTER TABLE Rol_permiso ALTER COLUMN id_rol_permiso RESTART WITH 7;
ALTER TABLE Usuario_rol ALTER COLUMN id_usuario_rol RESTART WITH 17;
ALTER TABLE Lugar ALTER COLUMN id_lugar RESTART WITH 4;
ALTER TABLE Organizador ALTER COLUMN id_organizador RESTART WITH 6;
ALTER TABLE Proveedor ALTER COLUMN id_proveedor RESTART WITH 6;
ALTER TABLE Evento ALTER COLUMN id_evento RESTART WITH 3;
ALTER TABLE Planificacion ALTER COLUMN id_planificacion RESTART WITH 4;
ALTER TABLE Invitacion ALTER COLUMN id_invitacion RESTART WITH 4;
ALTER TABLE Invitado ALTER COLUMN id_invitado RESTART WITH 8;
ALTER TABLE Acompanante ALTER COLUMN id_acompanante RESTART WITH 4;
ALTER TABLE Necesidad_Especial ALTER COLUMN id_necesidad RESTART WITH 3;
ALTER TABLE Regalo ALTER COLUMN id_regalo RESTART WITH 5;
ALTER TABLE Reserva_regalo ALTER COLUMN id_reserva RESTART WITH 4;
ALTER TABLE Servicio ALTER COLUMN id_servicio RESTART WITH 5;
ALTER TABLE Paquete_servicio ALTER COLUMN id_paquete RESTART WITH 3;
ALTER TABLE Solicitud_Ejecucion ALTER COLUMN id_solicitud RESTART WITH 4;
ALTER TABLE Propuesta ALTER COLUMN id_propuesta RESTART WITH 4;
ALTER TABLE Contratacion ALTER COLUMN id_contratacion RESTART WITH 3;
ALTER TABLE Movimiento_financiero ALTER COLUMN id_movimiento RESTART WITH 5;
ALTER TABLE Resena ALTER COLUMN id_resena RESTART WITH 3;
ALTER TABLE Conversacion ALTER COLUMN id_conversacion RESTART WITH 3;
ALTER TABLE Mensaje ALTER COLUMN id_mensaje RESTART WITH 5;
ALTER TABLE Respuesta_resena ALTER COLUMN id_respuesta RESTART WITH 3;
ALTER TABLE Sancion ALTER COLUMN id_sancion RESTART WITH 3;
ALTER TABLE Segmentacion ALTER COLUMN id_segmento RESTART WITH 4;
ALTER TABLE Campana ALTER COLUMN id_campana RESTART WITH 3;
ALTER TABLE Envio_Campana ALTER COLUMN id_envio RESTART WITH 5;
ALTER TABLE Puntos_fidelidad ALTER COLUMN id_puntos RESTART WITH 3;

-- =========================================================
-- MARKETING: datos de prueba para CU21 (segmentar clientes)
-- =========================================================

-- La segmentación la ejecuta el administrador de ejemplo ya registrado
-- arriba (admin.nexteventux, id 7); no se crea un segundo usuario con el mismo nombre.

-- Ubicación y edad de los clientes que ya existían.
UPDATE Usuario SET pais = 'Colombia', ciudad = 'Bogotá', fecha_nacimiento = DATE '1998-05-12' WHERE usuario = 'ana.martinez';
UPDATE Usuario SET pais = 'Colombia', ciudad = 'Medellín', fecha_nacimiento = DATE '1999-02-08' WHERE usuario = 'diego.perez';

-- Cliente que contrató cada evento de ejemplo.
UPDATE Evento SET id_cliente = (SELECT id_usuario FROM Usuario WHERE usuario = 'ana.martinez') WHERE id_evento = 1;
UPDATE Evento SET id_cliente = (SELECT id_usuario FROM Usuario WHERE usuario = 'diego.perez') WHERE id_evento = 2;

-- Clientes nuevos. Mínimo por segmento: 5 clientes.
-- Bogotá, 18-25 años: 6 clientes (forman segmento).
INSERT INTO Usuario
(nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario, pais, ciudad, fecha_nacimiento)
SELECT 'Cliente Bogotá Joven ' || X, 'cliente.bogota.joven' || X, 'clave111', 'ACTIVO',
       TIMESTAMP '2026-10-06 09:00:00', (X <> 3), 'CLIENTE', 'Colombia', 'Bogotá', DATE '2003-03-15'
FROM SYSTEM_RANGE(1, 6);

-- Bogotá, 26-35 años: 5 clientes nuevos + Ana = 6 (forman segmento).
INSERT INTO Usuario
(nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario, pais, ciudad, fecha_nacimiento)
SELECT 'Cliente Bogotá Adulto ' || X, 'cliente.bogota.adulto' || X, 'clave222', 'ACTIVO',
       TIMESTAMP '2026-10-06 10:00:00', TRUE, 'CLIENTE', 'Colombia', 'Bogotá', DATE '1996-06-20'
FROM SYSTEM_RANGE(1, 5);

-- Medellín, 26-35 años: 4 clientes nuevos + Diego = 5 (justo en el mínimo).
INSERT INTO Usuario
(nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario, pais, ciudad, fecha_nacimiento)
SELECT 'Cliente Medellín ' || X, 'cliente.medellin' || X, 'clave333', 'ACTIVO',
       TIMESTAMP '2026-10-06 11:00:00', TRUE, 'CLIENTE', 'Colombia', 'Medellín', DATE '1997-09-30'
FROM SYSTEM_RANGE(1, 4);

-- Cali, 36-50 años: solo 3 clientes (NO forman segmento).
INSERT INTO Usuario
(nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario, pais, ciudad, fecha_nacimiento)
SELECT 'Cliente Cali ' || X, 'cliente.cali' || X, 'clave444', 'ACTIVO',
       TIMESTAMP '2026-10-06 12:00:00', TRUE, 'CLIENTE', 'Colombia', 'Cali', DATE '1985-01-10'
FROM SYSTEM_RANGE(1, 3);

-- Clientes sin ciudad ni fecha de nacimiento (se excluyen de la segmentación).
INSERT INTO Usuario
(nombre, usuario, contraseña, estado, fecha_registro, acepta_marketing, tipo_usuario)
SELECT 'Cliente Sin Datos ' || X, 'cliente.sindatos' || X, 'clave555', 'ACTIVO',
       TIMESTAMP '2026-10-06 13:00:00', TRUE, 'CLIENTE'
FROM SYSTEM_RANGE(1, 2);

-- Búsquedas de tipos de evento.
INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda)
SELECT u.id_usuario, t.tipo, TIMESTAMP '2026-10-10 10:00:00'
FROM Usuario u
JOIN (VALUES ('Social'), ('Social'), ('Cultural')) AS t(tipo) ON TRUE
WHERE u.usuario LIKE 'cliente.bogota.joven%';

INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda)
SELECT u.id_usuario, t.tipo, TIMESTAMP '2026-10-11 10:00:00'
FROM Usuario u
JOIN (VALUES ('Corporativo'), ('Corporativo'), ('Social')) AS t(tipo) ON TRUE
WHERE u.usuario LIKE 'cliente.bogota.adulto%' OR u.usuario = 'ana.martinez';

INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda)
SELECT u.id_usuario, t.tipo, TIMESTAMP '2026-10-12 10:00:00'
FROM Usuario u
JOIN (VALUES ('Cultural'), ('Cultural'), ('Otro')) AS t(tipo) ON TRUE
WHERE u.usuario LIKE 'cliente.medellin%' OR u.usuario = 'diego.perez';

INSERT INTO Busqueda_evento (id_usuario, tipo, fecha_busqueda)
SELECT id_usuario, 'Social', TIMESTAMP '2026-10-13 10:00:00'
FROM Usuario
WHERE usuario LIKE 'cliente.cali%' OR usuario LIKE 'cliente.sindatos%';