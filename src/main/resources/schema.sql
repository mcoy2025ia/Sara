CREATE TABLE IF NOT EXISTS Permiso (
    id_permiso INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS Rol (
    id_rol INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS Usuario (
    id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    contraseña VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL,
    acepta_marketing BOOLEAN NOT NULL,
    tipo_usuario VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS Rol_permiso (
    id_rol_permiso INT PRIMARY KEY AUTO_INCREMENT,
    id_rol INT NOT NULL,
    id_permiso INT NOT NULL,
    FOREIGN KEY (id_rol) REFERENCES Rol(id_rol),
    FOREIGN KEY (id_permiso) REFERENCES Permiso(id_permiso)
);

CREATE TABLE IF NOT EXISTS Usuario_rol (
    id_usuario_rol INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    id_rol INT NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    FOREIGN KEY (id_rol) REFERENCES Rol(id_rol)
);

CREATE TABLE IF NOT EXISTS Lugar (
    id_lugar INT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    capacidad INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS Organizador (
    id_organizador INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    empresa VARCHAR(150),
    telefono VARCHAR(30),
    estado VARCHAR(30) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Proveedor (
    id_proveedor INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    empresa VARCHAR(150),
    correo VARCHAR(150),
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    estado VARCHAR(30) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Evento (
    id_evento INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(100),
    presupuesto DECIMAL(12,2) CHECK (presupuesto >= 0),
    invitados_iniciales INT CHECK (invitados_iniciales >= 0),
    descripcion VARCHAR(255),
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    ubicacion VARCHAR(255),
    estado VARCHAR(30) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    FOREIGN KEY (id_organizador) REFERENCES Organizador(id_organizador)
);

CREATE TABLE IF NOT EXISTS Planificacion (
    id_planificacion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    version INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30) NOT NULL,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE IF NOT EXISTS Invitacion (
    id_invitacion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    id_lugar INT NOT NULL,
    fecha_envio TIMESTAMP NOT NULL,
    mensaje VARCHAR(500),
    estado_respuesta VARCHAR(30),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_lugar) REFERENCES Lugar(id_lugar)
);

CREATE TABLE IF NOT EXISTS Invitado (
    id_invitado INT PRIMARY KEY AUTO_INCREMENT,
    id_invitacion INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(30),
    direccion VARCHAR(255),
    estado VARCHAR(30),
    correo VARCHAR(150),
    FOREIGN KEY (id_invitacion) REFERENCES Invitacion(id_invitacion)
);

CREATE TABLE IF NOT EXISTS Acompanante (
    id_acompanante INT PRIMARY KEY AUTO_INCREMENT,
    id_invitado INT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(30),
    tipo VARCHAR(50),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado)
);

CREATE TABLE IF NOT EXISTS Necesidad_Especial (
    id_necesidad INT PRIMARY KEY AUTO_INCREMENT,
    id_invitado INT NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado)
);

CREATE TABLE IF NOT EXISTS Regalo (
    id_regalo INT PRIMARY KEY AUTO_INCREMENT,
    id_invitacion INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    precio_estimado DECIMAL(12,2),
    FOREIGN KEY (id_invitacion) REFERENCES Invitacion(id_invitacion)
);

CREATE TABLE IF NOT EXISTS Reserva_regalo (
    id_reserva INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    id_invitado INT NOT NULL,
    id_regalo INT NOT NULL,
    fecha_reserva TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_invitado) REFERENCES Invitado(id_invitado),
    FOREIGN KEY (id_regalo) REFERENCES Regalo(id_regalo)
);

CREATE TABLE IF NOT EXISTS Servicio (
    id_servicio INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(12,2),
    categoria VARCHAR(100),
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE IF NOT EXISTS Paquete_servicio (
    id_paquete INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio DECIMAL(12,2),
    categoria VARCHAR(100),
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE IF NOT EXISTS Solicitud_Ejecucion (
    id_solicitud INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    id_evento INT NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE IF NOT EXISTS Propuesta (
    id_propuesta INT PRIMARY KEY AUTO_INCREMENT,
    id_solicitud INT NOT NULL,
    valor DECIMAL(12,2),
    descripcion VARCHAR(255),
    estado VARCHAR(30),
    FOREIGN KEY (id_solicitud) REFERENCES Solicitud_Ejecucion(id_solicitud)
);

CREATE TABLE IF NOT EXISTS Contratacion (
    id_contratacion INT PRIMARY KEY AUTO_INCREMENT,
    id_propuesta INT NOT NULL,
    id_evento INT NOT NULL,
    fecha_contratacion TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    condiciones VARCHAR(500),
    valor_acordado DECIMAL(12,2),
    FOREIGN KEY (id_propuesta) REFERENCES Propuesta(id_propuesta),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE IF NOT EXISTS Movimiento_financiero (
    id_movimiento INT PRIMARY KEY AUTO_INCREMENT,
    id_propuesta INT NOT NULL,
    id_evento INT NOT NULL,
    id_contratacion INT NOT NULL,
    valor DECIMAL(12,2),
    fecha TIMESTAMP NOT NULL,
    descripcion VARCHAR(255),
    tipo VARCHAR(50),
    estado VARCHAR(30),
    FOREIGN KEY (id_propuesta) REFERENCES Propuesta(id_propuesta),
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento),
    FOREIGN KEY (id_contratacion) REFERENCES Contratacion(id_contratacion)
);

CREATE TABLE IF NOT EXISTS Resena (
    id_resena INT PRIMARY KEY AUTO_INCREMENT,
    id_contratacion INT NOT NULL,
    calificacion INT,
    comentario VARCHAR(500),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_contratacion) REFERENCES Contratacion(id_contratacion)
);

CREATE TABLE IF NOT EXISTS Conversacion (
    id_conversacion INT PRIMARY KEY AUTO_INCREMENT,
    id_solicitud INT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_solicitud) REFERENCES Solicitud_Ejecucion(id_solicitud)
);

CREATE TABLE IF NOT EXISTS Mensaje (
    id_mensaje INT PRIMARY KEY AUTO_INCREMENT,
    id_conversacion INT NOT NULL,
    id_usuario_remitente INT NOT NULL,
    contenido VARCHAR(1000) NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    FOREIGN KEY (id_conversacion) REFERENCES Conversacion(id_conversacion),
    FOREIGN KEY (id_usuario_remitente) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Respuesta_resena (
    id_respuesta INT PRIMARY KEY AUTO_INCREMENT,
    id_resena INT NOT NULL,
    id_proveedor INT NOT NULL,
    comentario VARCHAR(500),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_resena) REFERENCES Resena(id_resena),
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE IF NOT EXISTS Sancion (
    id_sancion INT PRIMARY KEY AUTO_INCREMENT,
    id_evento INT NOT NULL,
    motivo VARCHAR(255),
    valor DECIMAL(12,2),
    estado VARCHAR(30),
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (id_evento) REFERENCES Evento(id_evento)
);

CREATE TABLE IF NOT EXISTS Segmentacion (
    id_segmento INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    criterios VARCHAR(500),
    descripcion VARCHAR(255),
    estados VARCHAR(30),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Campana (
    id_campana INT PRIMARY KEY AUTO_INCREMENT,
    id_proveedor INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    fecha_inicio TIMESTAMP,
    fecha_fin TIMESTAMP,
    FOREIGN KEY (id_proveedor) REFERENCES Proveedor(id_proveedor)
);

CREATE TABLE IF NOT EXISTS Envio_Campana (
    id_envio INT PRIMARY KEY AUTO_INCREMENT,
    id_campana INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha_solicitud TIMESTAMP NOT NULL,
    estado VARCHAR(30),
    FOREIGN KEY (id_campana) REFERENCES Campana(id_campana),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);

CREATE TABLE IF NOT EXISTS Puntos_fidelidad (
    id_puntos INT PRIMARY KEY AUTO_INCREMENT,
    id_usuario INT NOT NULL,
    cantidad INT NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario)
);
