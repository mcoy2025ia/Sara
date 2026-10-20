
-- =========================================================
-- CU-030: Verificar, categorizar y aprobar organizador
-- =========================================================

-- Datos profesionales y de identificacion utilizados
-- durante la revision del organizador.
ALTER TABLE Usuario
    ADD COLUMN IF NOT EXISTS documento_identidad VARCHAR(50);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS experiencia_anos INT;

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS especialidad VARCHAR(150);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS portafolio VARCHAR(500);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS tarifa_base DECIMAL(12,2);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS presupuesto_maximo DECIMAL(12,2);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS estado_verificacion
    VARCHAR(30) DEFAULT 'PENDIENTE' NOT NULL;

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(20);

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS puntaje_evaluacion INT;

ALTER TABLE Organizador
    ADD COLUMN IF NOT EXISTS version_datos INT DEFAULT 0 NOT NULL;


-- ---------------------------------------------------------
-- DOCUMENTOS DEL ORGANIZADOR
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS Documento_Organizador (
    id_documento INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    tipo_documento VARCHAR(80) NOT NULL,
    nombre_archivo VARCHAR(255),
    ruta_archivo VARCHAR(500),
    numero_documento VARCHAR(50),
    fecha_vencimiento DATE,
    estado VARCHAR(30) NOT NULL DEFAULT 'PRESENTADO',
    observacion VARCHAR(500),
    fecha_carga TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cu30_documento_organizador
        FOREIGN KEY (id_organizador)
        REFERENCES Organizador(id_organizador),

    CONSTRAINT chk_cu30_estado_documento
        CHECK (
            estado IN (
                'PRESENTADO',
                'VALIDADO',
                'POR_CORREGIR',
                'VENCIDO'
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_cu30_documentos_organizador
    ON Documento_Organizador(id_organizador, estado);


-- ---------------------------------------------------------
-- CONFIGURACION DE EVALUACION
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS Configuracion_Evaluacion_Organizador (
    id_configuracion INT PRIMARY KEY,
    peso_documentos INT NOT NULL,
    peso_portafolio INT NOT NULL,
    peso_experiencia INT NOT NULL,
    peso_perfil INT NOT NULL,
    puntaje_minimo_senior INT NOT NULL,
    presupuesto_maximo_sin_revision DECIMAL(12,2) NOT NULL,

    CONSTRAINT chk_cu30_pesos
        CHECK (
            peso_documentos >= 0
            AND peso_portafolio >= 0
            AND peso_experiencia >= 0
            AND peso_perfil >= 0
            AND peso_documentos + peso_portafolio
                + peso_experiencia + peso_perfil = 100
        ),

    CONSTRAINT chk_cu30_umbral_senior
        CHECK (puntaje_minimo_senior BETWEEN 0 AND 100),

    CONSTRAINT chk_cu30_presupuesto
        CHECK (presupuesto_maximo_sin_revision >= 0)
);

-- Valores iniciales de demostracion.
-- Son configurables y deben confirmarse con el equipo/profesor.
INSERT INTO Configuracion_Evaluacion_Organizador (
    id_configuracion,
    peso_documentos,
    peso_portafolio,
    peso_experiencia,
    peso_perfil,
    puntaje_minimo_senior,
    presupuesto_maximo_sin_revision
)
SELECT 1, 40, 20, 20, 20, 70, 5000000
WHERE NOT EXISTS (
    SELECT 1
    FROM Configuracion_Evaluacion_Organizador
    WHERE id_configuracion = 1
);


-- ---------------------------------------------------------
-- EVALUACIONES Y DECISIONES
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS Evaluacion_Organizador (
    id_evaluacion INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    id_administrador INT NOT NULL,
    puntaje INT NOT NULL,
    categoria_sugerida VARCHAR(20) NOT NULL,
    categoria_final VARCHAR(20),
    justificacion_cambio VARCHAR(500),
    decision VARCHAR(30) NOT NULL,
    motivo_rechazo VARCHAR(500),
    requiere_revision_presupuesto BOOLEAN NOT NULL DEFAULT FALSE,
    presupuesto_validado BOOLEAN NOT NULL DEFAULT FALSE,
    version_datos_revisada INT NOT NULL,
    fecha_evaluacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cu30_evaluacion_organizador
        FOREIGN KEY (id_organizador)
        REFERENCES Organizador(id_organizador),

    CONSTRAINT fk_cu30_evaluacion_administrador
        FOREIGN KEY (id_administrador)
        REFERENCES Usuario(id_usuario),

    CONSTRAINT chk_cu30_puntaje
        CHECK (puntaje BETWEEN 0 AND 100),

    CONSTRAINT chk_cu30_categoria_sugerida
        CHECK (categoria_sugerida IN ('JUNIOR', 'SENIOR')),

    CONSTRAINT chk_cu30_categoria_final
        CHECK (
            categoria_final IS NULL
            OR categoria_final IN ('JUNIOR', 'SENIOR')
        ),

    CONSTRAINT chk_cu30_decision
        CHECK (
            decision IN (
                'APROBADO',
                'RECHAZADO',
                'CORRECCION_SOLICITADA',
                'PENDIENTE_PRESUPUESTO'
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_cu30_evaluacion_organizador
    ON Evaluacion_Organizador(id_organizador, fecha_evaluacion);


-- ---------------------------------------------------------
-- CANAL INTERNO DE COMUNICACION
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS Mensaje_Organizador (
    id_mensaje INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    id_remitente INT NOT NULL,
    id_destinatario INT NOT NULL,
    contenido VARCHAR(1000) NOT NULL,
    tipo_mensaje VARCHAR(40) NOT NULL DEFAULT 'MENSAJE',
    estado VARCHAR(30) NOT NULL DEFAULT 'ENVIADO',
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cu30_mensaje_organizador
        FOREIGN KEY (id_organizador)
        REFERENCES Organizador(id_organizador),

    CONSTRAINT fk_cu30_mensaje_remitente
        FOREIGN KEY (id_remitente)
        REFERENCES Usuario(id_usuario),

    CONSTRAINT fk_cu30_mensaje_destinatario
        FOREIGN KEY (id_destinatario)
        REFERENCES Usuario(id_usuario),

    CONSTRAINT chk_cu30_tipo_mensaje
        CHECK (
            tipo_mensaje IN (
                'MENSAJE',
                'SOLICITUD_CORRECCION',
                'RESPUESTA'
            )
        ),

    CONSTRAINT chk_cu30_estado_mensaje
        CHECK (estado IN ('ENVIADO', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_cu30_mensajes_conversacion
    ON Mensaje_Organizador(id_organizador, fecha_hora);


-- ---------------------------------------------------------
-- BITACORA DE AUDITORIA
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS Bitacora_Organizador (
    id_bitacora INT PRIMARY KEY AUTO_INCREMENT,
    id_organizador INT NOT NULL,
    id_administrador INT NOT NULL,
    accion VARCHAR(80) NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30),
    categoria_anterior VARCHAR(20),
    categoria_nueva VARCHAR(20),
    puntaje INT,
    justificacion VARCHAR(500),
    detalles VARCHAR(1000),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cu30_bitacora_organizador
        FOREIGN KEY (id_organizador)
        REFERENCES Organizador(id_organizador),

    CONSTRAINT fk_cu30_bitacora_administrador
        FOREIGN KEY (id_administrador)
        REFERENCES Usuario(id_usuario)
);

CREATE INDEX IF NOT EXISTS idx_cu30_bitacora_organizador
    ON Bitacora_Organizador(id_organizador, fecha);

-- Compatibilidad con esquemas locales anteriores de notificaciones.
ALTER TABLE Notificacion_interna
    ADD COLUMN IF NOT EXISTS id_usuario_destinatario INT
    REFERENCES Usuario(id_usuario);

ALTER TABLE Notificacion_interna
    ADD COLUMN IF NOT EXISTS id_proveedor INT
    REFERENCES Proveedor(id_proveedor);

-- Las notificaciones dirigidas a organizadores no corresponden
-- necesariamente a un proveedor.
ALTER TABLE Notificacion_interna
    ALTER COLUMN id_proveedor DROP NOT NULL;
