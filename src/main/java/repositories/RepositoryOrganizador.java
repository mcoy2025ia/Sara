
package repositories;

import config.DatabaseConnection;
import entities.BitacoraOrganizador;
import entities.DocumentoOrganizador;
import entities.EvaluacionOrganizador;
import entities.MensajeOrganizador;
import entities.Organizador;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio de organizadores para CU-030.
 *
 * Centraliza las consultas de organizadores, documentos,
 * duplicidades de identidad, evaluaciones, mensajes internos,
 * notificaciones y bitacora de auditoria.
 */
public class RepositoryOrganizador {

    private final String url;

    private static final String CONSULTA_BASE =
            "SELECT o.id_organizador, u.nombre, o.empresa, o.ciudad, "
            + "o.especialidad, o.portafolio, o.tarifa_base, "
            + "COALESCE(AVG(r.calificacion), 0) AS calificacion_promedio, "
            + "COUNT(r.id_resena_organizador) AS total_resenas "
            + "FROM Organizador o "
            + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
            + "LEFT JOIN Resena_Organizador r "
            + "ON r.id_organizador = o.id_organizador ";

    public RepositoryOrganizador() {
        this(null);
    }

    /**
     * Constructor alternativo para pruebas con una base aislada.
     */
    public RepositoryOrganizador(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null
                ? DatabaseConnection.getConnection()
                : DatabaseConnection.getConnection(url);
    }

    /**
     * Busca un organizador por su identificador.
     */
    public Organizador buscarOrganizador(int idOrganizador) {

        String sql = CONSULTA_BASE
                + "WHERE o.id_organizador = ? "
                + "GROUP BY o.id_organizador, u.nombre, o.empresa, "
                + "o.ciudad, o.especialidad, o.portafolio, o.tarifa_base";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el organizador.", e);
        }
    }

    /**
     * Devuelve los organizadores activos pendientes de verificacion.
     */
    public List<Organizador> listarOrganizadoresPendientes() {

        String sql =
                "SELECT o.id_organizador, u.nombre "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE UPPER(o.estado_verificacion) = 'PENDIENTE' "
                + "AND UPPER(o.estado) = 'ACTIVO' "
                + "AND UPPER(u.estado) = 'ACTIVO' "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR' "
                + "ORDER BY u.nombre";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            List<Organizador> organizadores = new ArrayList<>();

            while (result.next()) {
                organizadores.add(
                        new Organizador(
                                result.getInt("id_organizador"),
                                result.getString("nombre")));
            }

            return organizadores;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los organizadores pendientes.",
                    e);
        }
    }

    /**
     * Actualiza el nombre y la informacion de perfil de un organizador.
     */
    public void actualizarOrganizador(
            int idOrganizador, Organizador organizador) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del organizador no es valido.");
        }

        if (organizador == null
                || organizador.nombre() == null
                || organizador.nombre().isBlank()) {

            throw new IllegalArgumentException(
                    "Los datos y el nombre del organizador son obligatorios.");
        }

        try (Connection connection = conectar()) {

            connection.setAutoCommit(false);

            try {
                String actualizarUsuario =
                        "UPDATE Usuario SET nombre = ? "
                        + "WHERE id_usuario = ("
                        + "SELECT id_usuario FROM Organizador "
                        + "WHERE id_organizador = ?)";

                try (PreparedStatement statement =
                             connection.prepareStatement(actualizarUsuario)) {

                    statement.setString(1, organizador.nombre().trim());
                    statement.setInt(2, idOrganizador);

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "El organizador seleccionado no existe.");
                    }
                }

                String actualizarPerfil =
                        "UPDATE Organizador SET empresa = ?, ciudad = ?, "
                        + "especialidad = ?, portafolio = ?, tarifa_base = ? "
                        + "WHERE id_organizador = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(actualizarPerfil)) {

                    statement.setString(1, organizador.empresa());
                    statement.setString(2, organizador.ciudad());
                    statement.setString(3, organizador.especialidad());
                    statement.setString(4, organizador.portafolio());
                    statement.setBigDecimal(5, organizador.tarifaBase());
                    statement.setInt(6, idOrganizador);

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "No se pudo actualizar el perfil del organizador.");
                    }
                }

                // CU-030 controla ediciones concurrentes con version_datos.
                incrementarVersionDatos(connection, idOrganizador);

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    e.addSuppressed(rollback);
                }

                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo actualizar el organizador.", e);
        }
    }

    /**
     * Recupera los datos profesionales y de verificacion
     * necesarios para que el administrador revise un perfil.
     */
    public Map<String, Object> obtenerDetalleOrganizador(
            int idOrganizador) {

        String sql =
                "SELECT o.id_organizador, o.id_usuario, "
                + "u.nombre, u.documento_identidad, "
                + "o.empresa, o.telefono, o.estado, "
                + "o.experiencia_anos, o.especialidad, o.portafolio, "
                + "o.tarifa_base, o.presupuesto_maximo, "
                + "o.estado_verificacion, o.categoria, "
                + "o.puntaje_evaluacion, o.version_datos "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ?";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                Map<String, Object> datos = new HashMap<>();

                datos.put("idOrganizador",
                        result.getInt("id_organizador"));
                datos.put("idUsuario",
                        result.getInt("id_usuario"));
                datos.put("nombre",
                        result.getString("nombre"));
                datos.put("documentoIdentidad",
                        result.getString("documento_identidad"));
                datos.put("empresa",
                        result.getString("empresa"));
                datos.put("telefono",
                        result.getString("telefono"));
                datos.put("estado",
                        result.getString("estado"));
                datos.put("experienciaAnios",
                        result.getObject("experiencia_anos"));
                datos.put("especialidad",
                        result.getString("especialidad"));
                datos.put("portafolio",
                        result.getString("portafolio"));
                datos.put("tarifaBase",
                        result.getBigDecimal("tarifa_base"));
                datos.put("presupuestoMaximo",
                        result.getBigDecimal("presupuesto_maximo"));
                datos.put("estadoVerificacion",
                        result.getString("estado_verificacion"));
                datos.put("categoria",
                        result.getString("categoria"));
                datos.put("puntajeEvaluacion",
                        result.getObject("puntaje_evaluacion"));
                datos.put("versionDatos",
                        result.getInt("version_datos"));

                return datos;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo recuperar el detalle del organizador.", e);
        }
    }

    /**
     * Actualiza la informacion profesional corregida por un organizador.
     *
     * Claves admitidas:
     * nombre, documentoIdentidad, empresa, telefono,
     * experienciaAnios, especialidad, portafolio,
     * tarifaBase y presupuestoMaximo.
     */
    public void actualizarDatosProfesionales(
            int idOrganizador, Map<String, Object> datos) {

        if (idOrganizador <= 0 || datos == null || datos.isEmpty()) {
            throw new IllegalArgumentException(
                    "Los datos corregidos son obligatorios.");
        }

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                String sqlUsuario =
                        "UPDATE Usuario SET nombre = ?, "
                        + "documento_identidad = ? "
                        + "WHERE id_usuario = ("
                        + "SELECT id_usuario FROM Organizador "
                        + "WHERE id_organizador = ?)";

                try (PreparedStatement statement =
                             connection.prepareStatement(sqlUsuario)) {

                    statement.setString(
                            1, texto(datos.get("nombre")));
                    statement.setString(
                            2, texto(datos.get("documentoIdentidad")));
                    statement.setInt(3, idOrganizador);

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "No se encontro el organizador.");
                    }
                }

                String sqlOrganizador =
                        "UPDATE Organizador SET empresa = ?, telefono = ?, "
                        + "experiencia_anos = ?, especialidad = ?, "
                        + "portafolio = ?, tarifa_base = ?, "
                        + "presupuesto_maximo = ?, "
                        + "version_datos = version_datos + 1 "
                        + "WHERE id_organizador = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(sqlOrganizador)) {

                    statement.setString(1, texto(datos.get("empresa")));
                    statement.setString(2, texto(datos.get("telefono")));

                    Object experiencia = datos.get("experienciaAnios");
                    if (experiencia == null) {
                        statement.setNull(3, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(
                                3, ((Number) experiencia).intValue());
                    }

                    statement.setString(
                            4, texto(datos.get("especialidad")));
                    statement.setString(
                            5, texto(datos.get("portafolio")));

                    asignarDecimal(
                            statement, 6, datos.get("tarifaBase"));
                    asignarDecimal(
                            statement, 7, datos.get("presupuestoMaximo"));

                    statement.setInt(8, idOrganizador);

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "No se pudo actualizar el perfil.");
                    }
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron guardar los datos corregidos.", e);
        }
    }

    // ---------------------------------------------------------
    // DOCUMENTOS
    // ---------------------------------------------------------

    /**
     * Consulta los documentos presentados por un organizador.
     */
    public List<DocumentoOrganizador> listarDocumentosPorOrganizador(
            int idOrganizador) {

        String sql =
                "SELECT id_documento, id_organizador, tipo_documento, "
                + "nombre_archivo, ruta_archivo, numero_documento, "
                + "fecha_vencimiento, estado, observacion, fecha_carga "
                + "FROM Documento_Organizador "
                + "WHERE id_organizador = ? "
                + "ORDER BY fecha_carga DESC";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                List<DocumentoOrganizador> documentos = new ArrayList<>();

                while (result.next()) {
                    DocumentoOrganizador documento =
                            new DocumentoOrganizador();

                    documento.setIdDocumento(
                            result.getInt("id_documento"));
                    documento.setIdOrganizador(
                            result.getInt("id_organizador"));
                    documento.setTipoDocumento(
                            result.getString("tipo_documento"));
                    documento.setNombreArchivo(
                            result.getString("nombre_archivo"));
                    documento.setRutaArchivo(
                            result.getString("ruta_archivo"));
                    documento.setNumeroDocumento(
                            result.getString("numero_documento"));

                    Date vencimiento =
                            result.getDate("fecha_vencimiento");

                    if (vencimiento != null) {
                        documento.setFechaVencimiento(
                                vencimiento.toLocalDate());
                    }

                    documento.setEstado(
                            result.getString("estado"));
                    documento.setObservacion(
                            result.getString("observacion"));

                    Timestamp carga =
                            result.getTimestamp("fecha_carga");

                    if (carga != null) {
                        documento.setFechaCarga(
                                carga.toLocalDateTime());
                    }

                    documentos.add(documento);
                }

                return documentos;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los documentos.", e);
        }
    }

    /**
     * Guarda un documento nuevo y marca el perfil como modificado.
     */
    public void guardarDocumento(DocumentoOrganizador documento) {

        if (documento == null
                || documento.getIdOrganizador() <= 0
                || documento.getTipoDocumento() == null
                || documento.getTipoDocumento().isBlank()) {

            throw new IllegalArgumentException(
                    "Los datos del documento no son validos.");
        }

        String sql =
                "INSERT INTO Documento_Organizador "
                + "(id_organizador, tipo_documento, nombre_archivo, "
                + "ruta_archivo, numero_documento, fecha_vencimiento, "
                + "estado, observacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                int idGenerado;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     sql, Statement.RETURN_GENERATED_KEYS)) {

                    statement.setInt(
                            1, documento.getIdOrganizador());
                    statement.setString(
                            2, documento.getTipoDocumento().trim());
                    statement.setString(
                            3, documento.getNombreArchivo());
                    statement.setString(
                            4, documento.getRutaArchivo());
                    statement.setString(
                            5, documento.getNumeroDocumento());

                    if (documento.getFechaVencimiento() == null) {
                        statement.setNull(
                                6, java.sql.Types.DATE);
                    } else {
                        statement.setDate(
                                6, Date.valueOf(
                                        documento.getFechaVencimiento()));
                    }

                    statement.setString(
                            7, documento.getEstado() == null
                                    ? "PRESENTADO"
                                    : documento.getEstado());
                    statement.setString(
                            8, documento.getObservacion());

                    statement.executeUpdate();

                    try (ResultSet generated =
                                 statement.getGeneratedKeys()) {
                        if (!generated.next()) {
                            throw new SQLException(
                                    "No se genero el identificador del documento.");
                        }

                        idGenerado = generated.getInt(1);
                    }
                }

                incrementarVersionDatos(
                        connection, documento.getIdOrganizador());

                connection.commit();

                documento.setIdDocumento(idGenerado);
                documento.setFechaCarga(LocalDateTime.now());

            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo guardar el documento.", e);
        }
    }

    /**
     * Actualiza el estado de un documento revisado.
     */
    public void actualizarEstadoDocumento(
            int idDocumento, String estado, String observacion)
            throws SQLException {

        if (!List.of(
                "PRESENTADO",
                "VALIDADO",
                "POR_CORREGIR",
                "VENCIDO").contains(estado)) {

            throw new IllegalArgumentException(
                    "El estado del documento no es valido.");
        }

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                int idOrganizador;

                try (PreparedStatement consulta =
                             connection.prepareStatement(
                                     "SELECT id_organizador "
                                     + "FROM Documento_Organizador "
                                     + "WHERE id_documento = ? FOR UPDATE")) {

                    consulta.setInt(1, idDocumento);

                    try (ResultSet result = consulta.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException(
                                    "El documento no existe.");
                        }

                        idOrganizador =
                                result.getInt("id_organizador");
                    }
                }

                try (PreparedStatement actualizar =
                             connection.prepareStatement(
                                     "UPDATE Documento_Organizador "
                                     + "SET estado = ?, observacion = ? "
                                     + "WHERE id_documento = ?")) {

                    actualizar.setString(1, estado);
                    actualizar.setString(2, observacion);
                    actualizar.setInt(3, idDocumento);
                    actualizar.executeUpdate();
                }

                incrementarVersionDatos(
                        connection, idOrganizador);

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        }
    }

    /**
     * Comprueba si el documento de identidad aparece en una cuenta
     * de Cliente o Proveedor distinta a la del organizador.
     */
    public boolean verificarIdentidadDuplicada(int idOrganizador) {

        String sql =
                "SELECT COUNT(*) "
                + "FROM Usuario actual "
                + "JOIN Organizador o "
                + "ON o.id_usuario = actual.id_usuario "
                + "JOIN Usuario otro "
                + "ON otro.documento_identidad = actual.documento_identidad "
                + "WHERE o.id_organizador = ? "
                + "AND actual.documento_identidad IS NOT NULL "
                + "AND TRIM(actual.documento_identidad) <> '' "
                + "AND otro.id_usuario <> actual.id_usuario "
                + "AND UPPER(otro.tipo_usuario) IN ('CLIENTE', 'PROVEEDOR')";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo comprobar la duplicidad de identidad.", e);
        }
    }

    // ---------------------------------------------------------
    // CONFIGURACION DE EVALUACION
    // ---------------------------------------------------------

    /**
     * Recupera los pesos y umbrales configurados para evaluar.
     */
    public Map<String, Object> obtenerConfiguracionEvaluacion() {

        String sql =
                "SELECT peso_documentos, peso_portafolio, "
                + "peso_experiencia, peso_perfil, "
                + "puntaje_minimo_senior, "
                + "presupuesto_maximo_sin_revision "
                + "FROM Configuracion_Evaluacion_Organizador "
                + "WHERE id_configuracion = 1";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            if (!result.next()) {
                throw new IllegalStateException(
                        "No hay configuracion de evaluacion.");
            }

            Map<String, Object> configuracion = new HashMap<>();

            configuracion.put("pesoDocumentos",
                    result.getInt("peso_documentos"));
            configuracion.put("pesoPortafolio",
                    result.getInt("peso_portafolio"));
            configuracion.put("pesoExperiencia",
                    result.getInt("peso_experiencia"));
            configuracion.put("pesoPerfil",
                    result.getInt("peso_perfil"));
            configuracion.put("puntajeMinimoSenior",
                    result.getInt("puntaje_minimo_senior"));
            configuracion.put("presupuestoMaximoSinRevision",
                    result.getBigDecimal(
                            "presupuesto_maximo_sin_revision"));

            return configuracion;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar la configuracion de evaluacion.",
                    e);
        }
    }

    // ---------------------------------------------------------
    // EVALUACION Y DECISION ADMINISTRATIVA
    // ---------------------------------------------------------

    /**
     * Guarda una evaluacion, actualiza el estado del organizador,
     * registra la auditoria y genera la notificacion en una
     * misma transaccion.
     *
     * Si ocurre un error, todos los cambios se revierten.
     */
    public void guardarEvaluacion(
            EvaluacionOrganizador evaluacion) {

        validarEvaluacion(evaluacion);

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                validarAdministrador(
                        connection,
                        evaluacion.getIdAdministrador());

                Map<String, Object> actual =
                        obtenerEstadoParaEvaluar(
                                connection,
                                evaluacion.getIdOrganizador());

                int versionActual =
                        ((Number) actual.get("versionDatos")).intValue();

                if (versionActual
                        != evaluacion.getVersionDatosRevisada()) {
                    throw new IllegalStateException(
                            "La informacion del organizador cambio "
                            + "durante la revision. Actualiza el perfil "
                            + "antes de continuar.");
                }

                String estadoAnterior =
                        (String) actual.get("estadoVerificacion");

                String categoriaAnterior =
                        (String) actual.get("categoria");

                if (!"PENDIENTE".equalsIgnoreCase(estadoAnterior)) {
                    throw new IllegalStateException(
                            "El organizador ya no esta pendiente "
                            + "de verificacion.");
                }

                if (evaluacion.tieneCambioManualDeCategoria()
                        && (evaluacion.getJustificacionCambio() == null
                        || evaluacion.getJustificacionCambio().isBlank())) {
                    throw new IllegalArgumentException(
                            "Debes justificar el cambio manual de categoria.");
                }

                if (evaluacion.isRequiereRevisionPresupuesto()
                        && !evaluacion.isPresupuestoValidado()
                        && EvaluacionOrganizador.DECISION_APROBADO
                                .equals(evaluacion.getDecision())) {
                    throw new IllegalStateException(
                            "El presupuesto requiere una validacion adicional.");
                }

                String estadoNuevo = estadoAnterior;

                if (EvaluacionOrganizador.DECISION_APROBADO
                        .equals(evaluacion.getDecision())) {

                    actualizarEstadoFinal(
                            connection,
                            evaluacion,
                            "VERIFICADO",
                            "ACTIVO");

                    estadoNuevo = "VERIFICADO";

                } else if (EvaluacionOrganizador.DECISION_RECHAZADO
                        .equals(evaluacion.getDecision())) {

                    actualizarEstadoFinal(
                            connection,
                            evaluacion,
                            "RECHAZADO",
                            "RECHAZADO");

                    estadoNuevo = "RECHAZADO";
                }

                insertarEvaluacion(connection, evaluacion);

                insertarBitacora(
                        connection,
                        new BitacoraOrganizador(
                                evaluacion.getIdOrganizador(),
                                evaluacion.getIdAdministrador(),
                                evaluacion.getDecision(),
                                estadoAnterior,
                                estadoNuevo,
                                categoriaAnterior,
                                evaluacion.getCategoriaFinal(),
                                evaluacion.getPuntaje(),
                                evaluacion.getJustificacionCambio(),
                                "CU-030: evaluacion y decision administrativa"));

                insertarNotificacion(
                        connection,
                        obtenerIdUsuarioOrganizador(
                                connection,
                                evaluacion.getIdOrganizador()),
                        construirMensajeDecision(evaluacion));

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No fue posible guardar la evaluacion del organizador.",
                    e);
        }
    }

    private void validarEvaluacion(
            EvaluacionOrganizador evaluacion) {

        if (evaluacion == null
                || evaluacion.getIdOrganizador() <= 0
                || evaluacion.getIdAdministrador() <= 0) {
            throw new IllegalArgumentException(
                    "Los datos de evaluacion no son validos.");
        }

        if (evaluacion.getPuntaje() < 0
                || evaluacion.getPuntaje() > 100) {
            throw new IllegalArgumentException(
                    "El puntaje debe estar entre 0 y 100.");
        }

        if (!EvaluacionOrganizador.esCategoriaValida(
                evaluacion.getCategoriaSugerida())
                || !EvaluacionOrganizador.esCategoriaValida(
                        evaluacion.getCategoriaFinal())) {
            throw new IllegalArgumentException(
                    "La categoria debe ser JUNIOR o SENIOR.");
        }

        String decision = evaluacion.getDecision();

        if (!List.of(
                EvaluacionOrganizador.DECISION_APROBADO,
                EvaluacionOrganizador.DECISION_RECHAZADO,
                EvaluacionOrganizador.DECISION_CORRECCION_SOLICITADA,
                EvaluacionOrganizador.DECISION_PENDIENTE_PRESUPUESTO)
                .contains(decision)) {
            throw new IllegalArgumentException(
                    "La decision administrativa no es valida.");
        }

        if (EvaluacionOrganizador.DECISION_RECHAZADO.equals(decision)
                && (evaluacion.getMotivoRechazo() == null
                || evaluacion.getMotivoRechazo().isBlank())) {
            throw new IllegalArgumentException(
                    "Debes indicar el motivo de rechazo.");
        }

        if (evaluacion.getVersionDatosRevisada() < 0) {
            throw new IllegalArgumentException(
                    "La version de datos revisada no es valida.");
        }
    }

    private void validarAdministrador(
            Connection connection, int idAdministrador)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM Usuario "
                + "WHERE id_usuario = ? "
                + "AND UPPER(tipo_usuario) = 'ADMINISTRADOR' "
                + "AND UPPER(estado) = 'ACTIVO'";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idAdministrador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalArgumentException(
                            "El usuario no tiene permisos de administrador.");
                }
            }
        }
    }

    private Map<String, Object> obtenerEstadoParaEvaluar(
            Connection connection, int idOrganizador)
            throws SQLException {

        String sql =
                "SELECT estado_verificacion, categoria, "
                + "version_datos "
                + "FROM Organizador "
                + "WHERE id_organizador = ? FOR UPDATE";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "El organizador no existe.");
                }

                Map<String, Object> actual = new HashMap<>();

                actual.put(
                        "estadoVerificacion",
                        result.getString("estado_verificacion"));
                actual.put(
                        "categoria",
                        result.getString("categoria"));
                actual.put(
                        "versionDatos",
                        result.getInt("version_datos"));

                return actual;
            }
        }
    }

    private void actualizarEstadoFinal(
            Connection connection,
            EvaluacionOrganizador evaluacion,
            String estadoVerificacion,
            String estadoOrganizador) throws SQLException {

        String sql =
                "UPDATE Organizador SET "
                + "estado_verificacion = ?, "
                + "estado = ?, "
                + "categoria = ?, "
                + "puntaje_evaluacion = ?, "
                + "version_datos = version_datos + 1 "
                + "WHERE id_organizador = ? "
                + "AND version_datos = ? "
                + "AND UPPER(estado_verificacion) = 'PENDIENTE'";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, estadoVerificacion);
            statement.setString(2, estadoOrganizador);
            statement.setString(3, evaluacion.getCategoriaFinal());
            statement.setInt(4, evaluacion.getPuntaje());
            statement.setInt(5, evaluacion.getIdOrganizador());
            statement.setInt(
                    6, evaluacion.getVersionDatosRevisada());

            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException(
                        "El organizador cambio durante la revision. "
                        + "Consulta la informacion actualizada.");
            }
        }
    }

    private void insertarEvaluacion(
            Connection connection,
            EvaluacionOrganizador evaluacion) throws SQLException {

        String sql =
                "INSERT INTO Evaluacion_Organizador "
                + "(id_organizador, id_administrador, puntaje, "
                + "categoria_sugerida, categoria_final, "
                + "justificacion_cambio, decision, motivo_rechazo, "
                + "requiere_revision_presupuesto, presupuesto_validado, "
                + "version_datos_revisada) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, evaluacion.getIdOrganizador());
            statement.setInt(2, evaluacion.getIdAdministrador());
            statement.setInt(3, evaluacion.getPuntaje());
            statement.setString(
                    4, evaluacion.getCategoriaSugerida());
            statement.setString(
                    5, evaluacion.getCategoriaFinal());
            statement.setString(
                    6, evaluacion.getJustificacionCambio());
            statement.setString(
                    7, evaluacion.getDecision());
            statement.setString(
                    8, evaluacion.getMotivoRechazo());
            statement.setBoolean(
                    9, evaluacion.isRequiereRevisionPresupuesto());
            statement.setBoolean(
                    10, evaluacion.isPresupuestoValidado());
            statement.setInt(
                    11, evaluacion.getVersionDatosRevisada());

            statement.executeUpdate();

            try (ResultSet generated = statement.getGeneratedKeys()) {
                if (generated.next()) {
                    evaluacion.setIdEvaluacion(generated.getInt(1));
                }
            }
        }
    }

    private void insertarBitacora(
            Connection connection,
            BitacoraOrganizador bitacora) throws SQLException {

        if (!bitacora.esValida()) {
            throw new IllegalArgumentException(
                    "La entrada de auditoria no es valida.");
        }

        String sql =
                "INSERT INTO Bitacora_Organizador "
                + "(id_organizador, id_administrador, accion, "
                + "estado_anterior, estado_nuevo, categoria_anterior, "
                + "categoria_nueva, puntaje, justificacion, detalles) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, bitacora.getIdOrganizador());
            statement.setInt(2, bitacora.getIdAdministrador());
            statement.setString(3, bitacora.getAccion());
            statement.setString(4, bitacora.getEstadoAnterior());
            statement.setString(5, bitacora.getEstadoNuevo());
            statement.setString(6, bitacora.getCategoriaAnterior());
            statement.setString(7, bitacora.getCategoriaNueva());

            if (bitacora.getPuntaje() == null) {
                statement.setNull(8, java.sql.Types.INTEGER);
            } else {
                statement.setInt(8, bitacora.getPuntaje());
            }

            statement.setString(9, bitacora.getJustificacion());
            statement.setString(10, bitacora.getDetalles());

            statement.executeUpdate();
        }
    }

    private String construirMensajeDecision(
            EvaluacionOrganizador evaluacion) {

        String mensaje;

        switch (evaluacion.getDecision()) {
            case EvaluacionOrganizador.DECISION_APROBADO:
                mensaje = "Tu perfil fue verificado y aprobado. "
                        + "Categoria asignada: "
                        + evaluacion.getCategoriaFinal() + ".";
                break;

            case EvaluacionOrganizador.DECISION_RECHAZADO:
                mensaje = "Tu perfil fue rechazado. Motivo: "
                        + evaluacion.getMotivoRechazo();
                break;

            case EvaluacionOrganizador.DECISION_CORRECCION_SOLICITADA:
                mensaje = "El administrador solicita informacion "
                        + "adicional o correcciones. Consulta el canal interno.";
                break;

            default:
                mensaje = "Tu perfil requiere una validacion adicional "
                        + "de presupuesto antes de continuar.";
                break;
        }

        return mensaje.length() > 500
                ? mensaje.substring(0, 500)
                : mensaje;
    }

    // ---------------------------------------------------------
    // MENSAJES INTERNOS
    // ---------------------------------------------------------

    /**
     * Guarda el mensaje y notifica al destinatario en una transaccion.
     * Si la notificacion falla, no se conserva el mensaje como enviado.
     */
    public void guardarMensaje(MensajeOrganizador mensaje) {

        if (mensaje == null || !mensaje.esValido()) {
            throw new IllegalArgumentException(
                    "Los datos del mensaje no son validos.");
        }

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                int idMensaje;

                String sql =
                        "INSERT INTO Mensaje_Organizador "
                        + "(id_organizador, id_remitente, id_destinatario, "
                        + "contenido, tipo_mensaje, estado) "
                        + "VALUES (?, ?, ?, ?, ?, 'ENVIADO')";

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     sql, Statement.RETURN_GENERATED_KEYS)) {

                    statement.setInt(
                            1, mensaje.getIdOrganizador());
                    statement.setInt(
                            2, mensaje.getIdRemitente());
                    statement.setInt(
                            3, mensaje.getIdDestinatario());
                    statement.setString(
                            4, mensaje.getContenido().trim());
                    statement.setString(
                            5, mensaje.getTipoMensaje());

                    statement.executeUpdate();

                    try (ResultSet generated =
                                 statement.getGeneratedKeys()) {
                        if (!generated.next()) {
                            throw new SQLException(
                                    "No se genero el identificador del mensaje.");
                        }

                        idMensaje = generated.getInt(1);
                    }
                }

                insertarNotificacion(
                        connection,
                        mensaje.getIdDestinatario(),
                        mensaje.getContenido());

                connection.commit();

                mensaje.setIdMensaje(idMensaje);
                mensaje.setEstado(MensajeOrganizador.ESTADO_ENVIADO);
                mensaje.setFechaHora(LocalDateTime.now());

            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No fue posible enviar el mensaje. Puedes intentarlo nuevamente.",
                    e);
        }
    }

    /**
     * Consulta el historial de mensajes de un organizador.
     */
    public List<MensajeOrganizador> listarMensajes(
            int idOrganizador) {

        String sql =
                "SELECT id_mensaje, id_organizador, id_remitente, "
                + "id_destinatario, contenido, tipo_mensaje, estado, "
                + "fecha_hora "
                + "FROM Mensaje_Organizador "
                + "WHERE id_organizador = ? "
                + "ORDER BY fecha_hora ASC";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                List<MensajeOrganizador> mensajes = new ArrayList<>();

                while (result.next()) {
                    MensajeOrganizador mensaje =
                            new MensajeOrganizador();

                    mensaje.setIdMensaje(
                            result.getInt("id_mensaje"));
                    mensaje.setIdOrganizador(
                            result.getInt("id_organizador"));
                    mensaje.setIdRemitente(
                            result.getInt("id_remitente"));
                    mensaje.setIdDestinatario(
                            result.getInt("id_destinatario"));
                    mensaje.setContenido(
                            result.getString("contenido"));
                    mensaje.setTipoMensaje(
                            result.getString("tipo_mensaje"));
                    mensaje.setEstado(
                            result.getString("estado"));

                    Timestamp fecha =
                            result.getTimestamp("fecha_hora");

                    if (fecha != null) {
                        mensaje.setFechaHora(
                                fecha.toLocalDateTime());
                    }

                    mensajes.add(mensaje);
                }

                return mensajes;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el historial de mensajes.", e);
        }
    }

    // ---------------------------------------------------------
    // NOTIFICACIONES Y CONTROL DE VERSION
    // ---------------------------------------------------------

    private int obtenerIdUsuarioOrganizador(
            Connection connection, int idOrganizador)
            throws SQLException {

        String sql =
                "SELECT id_usuario FROM Organizador "
                + "WHERE id_organizador = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "No se encontro el usuario del organizador.");
                }

                return result.getInt("id_usuario");
            }
        }
    }

    /**
     * Usa ambas columnas de destinatario para mantener compatibilidad
     * con esquemas que conservan id_usuario_destinatario.
     */
    private void insertarNotificacion(
            Connection connection,
            int idDestinatario,
            String mensaje) throws SQLException {

        String sql =
                "INSERT INTO Notificacion_interna "
                + "(id_usuario, id_usuario_destinatario, id_proveedor, mensaje) "
                + "VALUES (?, ?, NULL, ?)";

        String texto = mensaje == null ? "" : mensaje.trim();

        if (texto.length() > 500) {
            texto = texto.substring(0, 500);
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idDestinatario);
            statement.setInt(2, idDestinatario);
            statement.setString(3, texto);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "No se pudo registrar la notificacion interna.");
            }
        }
    }

    private void incrementarVersionDatos(
            Connection connection, int idOrganizador)
            throws SQLException {

        String sql =
                "UPDATE Organizador "
                + "SET version_datos = version_datos + 1 "
                + "WHERE id_organizador = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "No se pudo actualizar la version del perfil.");
            }
        }
    }

    private void rollback(
            Connection connection, Exception original) {

        try {
            connection.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }

    private String texto(Object valor) {
        return valor == null
                ? null
                : valor.toString().trim();
    }

    private void asignarDecimal(
            PreparedStatement statement,
            int indice,
            Object valor) throws SQLException {

        if (valor == null) {
            statement.setNull(indice, java.sql.Types.DECIMAL);
            return;
        }

        BigDecimal decimal = valor instanceof BigDecimal
                ? (BigDecimal) valor
                : new BigDecimal(valor.toString());

        if (decimal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Los valores monetarios no pueden ser negativos.");
        }

        statement.setBigDecimal(indice, decimal);
    }

    /**
     * Comprueba que el organizador este activo y verificado.
     */
    public boolean organizadorActivoVerificado(int idOrganizador) {

        String sql =
                "SELECT COUNT(*) "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ? "
                + "AND UPPER(o.estado) = 'ACTIVO' "
                + "AND UPPER(o.estado_verificacion) = 'VERIFICADO' "
                + "AND UPPER(u.estado) = 'ACTIVO' "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR'";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo verificar el estado del organizador.", e);
        }
    }

    /**
     * Busca organizadores activos y verificados que coincidan
     * con los criterios ingresados por el cliente.
     *
     * Filtros admitidos:
     * ciudad, tipoEvento, presupuestoEstimado y fechaAproximada.
     */
    public List<Organizador> consultarOrganizadoresDisponibles(
            Map<String, Object> filtros) {

        StringBuilder sql = new StringBuilder(CONSULTA_BASE);

        sql.append(
                "WHERE UPPER(u.estado) = 'ACTIVO' "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR' "
                + "AND UPPER(o.estado) = 'ACTIVO' "
                + "AND UPPER(o.estado_verificacion) = 'VERIFICADO' ");

        List<Object> parametros = new ArrayList<>();

        if (filtros != null) {
            agregarFiltroCiudad(sql, parametros, filtros);
            agregarFiltroTipoEvento(sql, parametros, filtros);
            agregarFiltroPresupuesto(sql, parametros, filtros);
            agregarFiltroFecha(sql, parametros, filtros);
        }

        sql.append(
                "GROUP BY o.id_organizador, u.nombre, o.empresa, "
                + "o.ciudad, o.especialidad, o.portafolio, o.tarifa_base "
                + "ORDER BY calificacion_promedio DESC, "
                + "total_resenas DESC, u.nombre ASC");

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            asignarParametros(statement, parametros);

            try (ResultSet result = statement.executeQuery()) {
                List<Organizador> organizadores = new ArrayList<>();

                while (result.next()) {
                    organizadores.add(convertir(result));
                }

                return organizadores;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los organizadores disponibles.",
                    e);
        }
    }

    /**
     * Comprueba el estado y la disponibilidad del organizador
     * para la fecha del evento.
     */
    public boolean verificarDisponibilidadOrganizador(
            int idOrganizador, java.time.LocalDate fecha) {

        if (idOrganizador <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del organizador no es valido.");
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha aproximada del evento es obligatoria.");
        }

        String sql =
                "SELECT COUNT(*) "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ? "
                + "AND UPPER(u.estado) = 'ACTIVO' "
                + "AND UPPER(u.tipo_usuario) = 'ORGANIZADOR' "
                + "AND UPPER(o.estado) = 'ACTIVO' "
                + "AND UPPER(o.estado_verificacion) = 'VERIFICADO' "
                + "AND NOT EXISTS ("
                + "SELECT 1 FROM Evento e "
                + "WHERE e.id_organizador = o.id_organizador "
                + "AND CAST(e.fecha_inicio AS DATE) <= ? "
                + "AND CAST(e.fecha_fin AS DATE) >= ? "
                + "AND UPPER(e.estado) <> 'CANCELADO')";

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);
            statement.setDate(2, Date.valueOf(fecha));
            statement.setDate(3, Date.valueOf(fecha));

            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo verificar la disponibilidad del organizador.",
                    e);
        }
    }

    private void agregarFiltroCiudad(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros) {

        Object ciudad = filtros.get("ciudad");

        if (ciudad == null || ciudad.toString().isBlank()) {
            return;
        }

        sql.append("AND LOWER(o.ciudad) = LOWER(?) ");
        parametros.add(ciudad.toString().trim());
    }

    private void agregarFiltroTipoEvento(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros) {

        Object tipoEvento = filtros.get("tipoEvento");

        if (tipoEvento == null || tipoEvento.toString().isBlank()) {
            return;
        }

        sql.append("AND LOWER(o.especialidad) LIKE LOWER(?) ");
        parametros.add("%" + tipoEvento.toString().trim() + "%");
    }

    private void agregarFiltroPresupuesto(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros) {

        Object presupuesto = filtros.get("presupuestoEstimado");

        if (presupuesto == null) {
            return;
        }

        if (!(presupuesto instanceof Number)) {
            throw new IllegalArgumentException(
                    "El presupuesto debe ser un valor numerico.");
        }

        BigDecimal valor = new BigDecimal(presupuesto.toString());

        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El presupuesto no puede ser negativo.");
        }

        sql.append("AND o.tarifa_base <= ? ");
        parametros.add(valor);
    }

    private void agregarFiltroFecha(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros) {

        Object fecha = filtros.get("fechaAproximada");

        if (fecha == null) {
            return;
        }

        if (!(fecha instanceof java.time.LocalDate)) {
            throw new IllegalArgumentException(
                    "La fecha aproximada debe ser LocalDate.");
        }

        sql.append(
                "AND NOT EXISTS ("
                + "SELECT 1 FROM Evento e "
                + "WHERE e.id_organizador = o.id_organizador "
                + "AND CAST(e.fecha_inicio AS DATE) <= ? "
                + "AND CAST(e.fecha_fin AS DATE) >= ? "
                + "AND UPPER(e.estado) <> 'CANCELADO') ");

        Date fechaSQL = Date.valueOf((java.time.LocalDate) fecha);

        parametros.add(fechaSQL);
        parametros.add(fechaSQL);
    }

    private void asignarParametros(
            PreparedStatement statement,
            List<Object> parametros) throws SQLException {

        for (int i = 0; i < parametros.size(); i++) {
            statement.setObject(i + 1, parametros.get(i));
        }
    }

    private Organizador convertir(ResultSet result) throws SQLException {

        BigDecimal tarifa = result.getBigDecimal("tarifa_base");
        BigDecimal promedio = result.getBigDecimal("calificacion_promedio");

        return new Organizador(
                result.getInt("id_organizador"),
                result.getString("nombre"),
                result.getString("empresa"),
                result.getString("ciudad"),
                result.getString("especialidad"),
                result.getString("portafolio"),
                tarifa == null ? BigDecimal.ZERO : tarifa,
                promedio == null ? BigDecimal.ZERO : promedio,
                result.getInt("total_resenas")
        );
    }
}
