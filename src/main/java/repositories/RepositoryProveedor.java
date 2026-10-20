package repositories;

import config.DatabaseConnection;
import entities.DocumentoProveedor;
import entities.NotificacionInterna;
import entities.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la persistencia de proveedores para el CU-028.
 */
public class RepositoryProveedor {

    private final String url;

    public RepositoryProveedor() {
        this(null);
    }

    /**
     * Permite usar una base H2 aislada en las pruebas.
     */
    public RepositoryProveedor(String url) {
        this.url = url;
    }

    public boolean administradorActivo(int usuario) {
        String sql="SELECT 1 FROM Usuario u JOIN Usuario_rol ur ON ur.id_usuario=u.id_usuario "
                + "JOIN Rol r ON r.id_rol=ur.id_rol WHERE u.id_usuario=? AND UPPER(u.estado)='ACTIVO' "
                + "AND UPPER(u.tipo_usuario)='ADMINISTRADOR' AND UPPER(r.nombre)='ADMINISTRADOR' AND UPPER(r.estado)='ACTIVO'";
        try(Connection c=conectar(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1,usuario);
            try(ResultSet r=s.executeQuery()) { return r.next(); }
        } catch(SQLException e) { throw new IllegalStateException("No se pudo validar la sesión administrativa.",e); }
    }

    private Connection conectar() throws SQLException {
        return url == null
                ? DatabaseConnection.getConnection()
                : DatabaseConnection.getConnection(url);
    }

    // ---------------------------------------------------------
    // PROVEEDORES
    // ---------------------------------------------------------

    /**
     * Busca un proveedor por su identificador.
     * Retorna null si no existe.
     */
    public Proveedor buscarProveedor(int idProveedor) {
        String sql = "SELECT id_proveedor, id_usuario, nombre, empresa, "
                + "correo, telefono, direccion, estado, motivo_rechazo "
                + "FROM Proveedor WHERE id_proveedor = ?";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idProveedor);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapearProveedor(result) : null;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el proveedor.", e);
        }
    }

    /**
     * Lista los proveedores que están pendientes de verificación.
     */
    public List<Proveedor> listarProveedoresPendientes() {
        List<Proveedor> proveedores = new ArrayList<>();

        String sql = "SELECT id_proveedor, id_usuario, nombre, empresa, "
                + "correo, telefono, direccion, estado, motivo_rechazo "
                + "FROM Proveedor WHERE " + condicionPendiente("estado")
                + " ORDER BY id_proveedor";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                proveedores.add(mapearProveedor(result));
            }

            return proveedores;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los proveedores pendientes.", e);
        }
    }

    /**
     * Actualiza el estado sin enviar una notificación.
     * Se conserva para mantener la operación original del diagrama.
     */
    public void actualizarEstadoProveedor(int idProveedor, String estado) {
        if (estado == null || estado.isBlank()) {
            throw new IllegalArgumentException(
                    "El estado del proveedor es obligatorio.");
        }

        String sql = "UPDATE Proveedor SET estado = ?, "
                + "motivo_rechazo = NULL, observacion_correccion = NULL "
                + "WHERE id_proveedor = ? AND " + condicionPendiente("estado");

        actualizarEstadoPendiente(idProveedor, estado, null, sql, false);
    }

    /**
     * Aprueba al proveedor y registra la notificación en la misma transacción.
     * Si alguno de los cambios falla, se revierten ambos.
     */
    public void aprobarProveedorConNotificacion(int idProveedor) {
        actualizarEstadoYNotificar(
                idProveedor,
                Proveedor.ESTADO_VERIFICADO,
                null,
                false,
                NotificacionInterna.TIPO_APROBACION,
                "Proveedor verificado",
                "Tu registro como proveedor fue aprobado. Ya puedes consultar "
                        + "el estado de tu cuenta en NextEventux."
        );
    }

    /**
     * Rechaza al proveedor, conserva el motivo y registra una notificación
     * en la misma transacción.
     */
    public void rechazarProveedor(int idProveedor, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                    "Indica el motivo del rechazo.");
        }

        String motivoLimpio = motivo.trim();

        if (motivoLimpio.length() > 500) {
            throw new IllegalArgumentException(
                    "El motivo del rechazo debe tener máximo 500 caracteres.");
        }

        actualizarEstadoYNotificar(
                idProveedor,
                Proveedor.ESTADO_RECHAZADO,
                motivoLimpio,
                true,
                NotificacionInterna.TIPO_RECHAZO,
                "Solicitud de proveedor rechazada",
                "Tu solicitud no fue aprobada. Motivo: " + motivoLimpio
        );
    }

    /**
     * Solicita correcciones sin cambiar el estado pendiente.
     * Guarda la observación y notifica al usuario relacionado.
     */
    public void solicitarCorrecciones(int idProveedor, String observacion) {
        if (observacion == null || observacion.isBlank()) {
            throw new IllegalArgumentException(
                    "Indica qué información debe corregirse.");
        }

        String observacionLimpia = observacion.trim();

        if (observacionLimpia.length() > 500) {
            throw new IllegalArgumentException(
                    "La observación debe tener máximo 500 caracteres.");
        }

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                int idUsuario = obtenerUsuarioPendiente(connection, idProveedor);

                String sql = "UPDATE Proveedor SET observacion_correccion = ? "
                        + "WHERE id_proveedor = ? AND "
                        + condicionPendiente("estado");

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, observacionLimpia);
                    statement.setInt(2, idProveedor);

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "El proveedor ya no está pendiente de verificación.");
                    }
                }

                insertarNotificacion(
                        connection,
                        idUsuario,
                        idProveedor,
                        NotificacionInterna.TIPO_CORRECCION,
                        "Información pendiente de corrección",
                        observacionLimpia
                );

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                revertir(connection, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo registrar la solicitud de corrección.", e);
        }
    }

    private void actualizarEstadoYNotificar(
            int idProveedor,
            String estado,
            String motivo,
            boolean incluirMotivo,
            String tipoNotificacion,
            String titulo,
            String mensaje) {

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                int idUsuario = obtenerUsuarioPendiente(
                        connection, idProveedor);

                String sql;

                if (incluirMotivo) {
                    sql = "UPDATE Proveedor SET estado = ?, motivo_rechazo = ?, "
                            + "observacion_correccion = NULL "
                            + "WHERE id_proveedor = ? AND "
                            + condicionPendiente("estado");
                } else {
                    sql = "UPDATE Proveedor SET estado = ?, motivo_rechazo = NULL, "
                            + "observacion_correccion = NULL "
                            + "WHERE id_proveedor = ? AND "
                            + condicionPendiente("estado");
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, estado);

                    if (incluirMotivo) {
                        statement.setString(2, motivo);
                        statement.setInt(3, idProveedor);
                    } else {
                        statement.setInt(2, idProveedor);
                    }

                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "El proveedor no existe o ya no está pendiente.");
                    }
                }

                insertarNotificacion(
                        connection,
                        idUsuario,
                        idProveedor,
                        tipoNotificacion,
                        titulo,
                        mensaje
                );

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                revertir(connection, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo guardar la decisión del proveedor.", e);
        }
    }

    private int obtenerUsuarioPendiente(
            Connection connection, int idProveedor) throws SQLException {

        String sql = "SELECT id_usuario FROM Proveedor "
                + "WHERE id_proveedor = ? AND " + condicionPendiente("estado");

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idProveedor);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "El proveedor no existe o ya no está pendiente "
                                    + "de verificación.");
                }

                return result.getInt("id_usuario");
            }
        }
    }

    private void actualizarEstadoPendiente(
            int idProveedor,
            String estado,
            String motivo,
            String sql,
            boolean incluyeMotivo) {

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int indice = 1;

                statement.setString(indice++, estado);

                if (incluyeMotivo) {
                    statement.setString(indice++, motivo);
                }

                statement.setInt(indice, idProveedor);

                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException(
                            "El proveedor no existe o ya no está pendiente.");
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {
                revertir(connection, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo actualizar el estado del proveedor.", e);
        }
    }

    // ---------------------------------------------------------
    // DOCUMENTOS DEL PROVEEDOR
    // ---------------------------------------------------------

    /**
     * Lista los documentos asociados a un proveedor.
     */
    public List<DocumentoProveedor> listarDocumentosPorProveedor(
            int idProveedor) {

        List<DocumentoProveedor> documentos = new ArrayList<>();

        String sql = "SELECT id_documento, id_proveedor, tipo_documento, "
                + "nombre_archivo, ruta_archivo, fecha_vencimiento, estado, "
                + "observacion, fecha_carga "
                + "FROM Documento_Proveedor WHERE id_proveedor = ? "
                + "ORDER BY id_documento";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idProveedor);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    documentos.add(mapearDocumento(result));
                }
            }

            return documentos;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los documentos del proveedor.", e);
        }
    }

    /**
     * Registra los metadatos de un documento.
     * Retorna el identificador generado por H2.
     *
     * La ruta se guarda en la base; este método no copia el archivo físico.
     */
    public int guardarDocumento(DocumentoProveedor documento) {
        validarDocumento(documento);

        String sql = "INSERT INTO Documento_Proveedor "
                + "(id_proveedor, tipo_documento, nombre_archivo, ruta_archivo, "
                + "fecha_vencimiento, estado, observacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, documento.getIdProveedor());
            statement.setString(2, documento.getTipoDocumento().trim());
            statement.setString(3, documento.getNombreArchivo().trim());
            statement.setString(4, documento.getRutaArchivo().trim());

            if (documento.getFechaVencimiento() == null) {
                statement.setNull(5, Types.DATE);
            } else {
                statement.setDate(
                        5, java.sql.Date.valueOf(documento.getFechaVencimiento()));
            }

            statement.setString(6, DocumentoProveedor.ESTADO_PRESENTADO);
            statement.setString(7, documento.getObservacion());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

            throw new IllegalStateException(
                    "No se obtuvo el identificador del documento.");

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo guardar el documento del proveedor.", e);
        }
    }

    /**
     * Actualiza un documento corregido y lo devuelve al estado PRESENTADO.
     */
    public void actualizarDocumentoCorregido(
            int idDocumento,
            int idProveedor,
            String nombreArchivo,
            String rutaArchivo,
            LocalDate fechaVencimiento) {

        if (idDocumento <= 0 || idProveedor <= 0) {
            throw new IllegalArgumentException(
                    "Selecciona un documento y un proveedor válidos.");
        }

        if (nombreArchivo == null || nombreArchivo.isBlank()
                || rutaArchivo == null || rutaArchivo.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre y la ruta del documento son obligatorios.");
        }

        String sql = "UPDATE Documento_Proveedor SET "
                + "nombre_archivo = ?, ruta_archivo = ?, fecha_vencimiento = ?, "
                + "estado = ?, observacion = NULL, fecha_carga = CURRENT_TIMESTAMP "
                + "WHERE id_documento = ? AND id_proveedor = ?";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, nombreArchivo.trim());
            statement.setString(2, rutaArchivo.trim());

            if (fechaVencimiento == null) {
                statement.setNull(3, Types.DATE);
            } else {
                statement.setDate(3, java.sql.Date.valueOf(fechaVencimiento));
            }

            statement.setString(4, DocumentoProveedor.ESTADO_PRESENTADO);
            statement.setInt(5, idDocumento);
            statement.setInt(6, idProveedor);

            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "No se encontró el documento asociado al proveedor.");
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo actualizar el documento corregido.", e);
        }
    }

    /**
     * Permite marcar un documento como validado, pendiente de corrección
     * o vencido.
     */
    public void actualizarEstadoDocumento(
            int idDocumento, String estado, String observacion) {

        if (idDocumento <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del documento no es válido.");
        }

        if (!DocumentoProveedor.ESTADO_VALIDADO.equals(estado)
                && !DocumentoProveedor.ESTADO_POR_CORREGIR.equals(estado)
                && !DocumentoProveedor.ESTADO_VENCIDO.equals(estado)
                && !DocumentoProveedor.ESTADO_PRESENTADO.equals(estado)) {
            throw new IllegalArgumentException(
                    "El estado del documento no es válido.");
        }

        String sql = "UPDATE Documento_Proveedor "
                + "SET estado = ?, observacion = ? WHERE id_documento = ?";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, estado);
            statement.setString(2, observacion);
            statement.setInt(3, idDocumento);

            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException(
                        "No se encontró el documento indicado.");
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo actualizar el estado del documento.", e);
        }
    }

    private void validarDocumento(DocumentoProveedor documento) {
        if (documento == null
                || documento.getIdProveedor() <= 0
                || documento.getTipoDocumento() == null
                || documento.getTipoDocumento().isBlank()
                || documento.getNombreArchivo() == null
                || documento.getNombreArchivo().isBlank()
                || documento.getRutaArchivo() == null
                || documento.getRutaArchivo().isBlank()) {

            throw new IllegalArgumentException(
                    "Completa los datos obligatorios del documento.");
        }
    }

    private DocumentoProveedor mapearDocumento(ResultSet result)
            throws SQLException {

        java.sql.Date fechaSQL = result.getDate("fecha_vencimiento");
        Timestamp fechaCargaSQL = result.getTimestamp("fecha_carga");

        return new DocumentoProveedor(
                result.getInt("id_documento"),
                result.getInt("id_proveedor"),
                result.getString("tipo_documento"),
                result.getString("nombre_archivo"),
                result.getString("ruta_archivo"),
                fechaSQL == null ? null : fechaSQL.toLocalDate(),
                result.getString("estado"),
                result.getString("observacion"),
                fechaCargaSQL == null
                        ? null
                        : fechaCargaSQL.toLocalDateTime()
        );
    }

    // ---------------------------------------------------------
    // SERVICIOS OFRECIDOS POR EL PROVEEDOR
    // ---------------------------------------------------------

    /**
     * Devuelve una descripción de los servicios y paquetes del proveedor
     * para que la capa de presentación pueda mostrarlos al administrador.
     */
    public List<String> listarServiciosProveedor(int idProveedor) {
        List<String> servicios = new ArrayList<>();

        String sqlServicios = "SELECT nombre, descripcion, precio, categoria, estado "
                + "FROM Servicio WHERE id_proveedor = ? ORDER BY id_servicio";

        String sqlPaquetes = "SELECT nombre, descripcion, precio, categoria, estado "
                + "FROM Paquete_servicio WHERE id_proveedor = ? ORDER BY id_paquete";

        try (Connection connection = conectar()) {

            consultarDescripcionServicios(
                    connection, sqlServicios, idProveedor, "Servicio", servicios);

            consultarDescripcionServicios(
                    connection, sqlPaquetes, idProveedor, "Paquete", servicios);

            return servicios;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los servicios del proveedor.", e);
        }
    }

    private void consultarDescripcionServicios(
            Connection connection,
            String sql,
            int idProveedor,
            String tipo,
            List<String> resultado) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idProveedor);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String nombre = result.getString("nombre");
                    String descripcion = result.getString("descripcion");
                    String categoria = result.getString("categoria");
                    String estado = result.getString("estado");
                    String precio = result.getString("precio");

                    String detalle = tipo + ": " + nombre;

                    if (categoria != null && !categoria.isBlank()) {
                        detalle += " | Categoría: " + categoria;
                    }

                    if (precio != null) {
                        detalle += " | Precio: " + precio;
                    }

                    if (estado != null && !estado.isBlank()) {
                        detalle += " | Estado: " + estado;
                    }

                    if (descripcion != null && !descripcion.isBlank()) {
                        detalle += " | " + descripcion;
                    }

                    resultado.add(detalle);
                }
            }
        }
    }

    // ---------------------------------------------------------
    // NOTIFICACIONES INTERNAS
    // ---------------------------------------------------------

    /**
     * Lista las notificaciones destinadas a un usuario.
     */
    public List<NotificacionInterna> listarNotificacionesPorUsuario(
            int idUsuario) {

        List<NotificacionInterna> notificaciones = new ArrayList<>();

        String sql = "SELECT id_notificacion, id_usuario_destinatario, "
                + "id_proveedor, tipo, titulo, mensaje, fecha_creacion, leida "
                + "FROM Notificacion_Proveedor "
                + "WHERE id_usuario_destinatario = ? "
                + "ORDER BY fecha_creacion DESC, id_notificacion DESC";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idUsuario);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Timestamp fechaSQL = result.getTimestamp("fecha_creacion");

                    notificaciones.add(new NotificacionInterna(
                            result.getInt("id_notificacion"),
                            result.getInt("id_usuario_destinatario"),
                            result.getInt("id_proveedor"),
                            result.getString("tipo"),
                            result.getString("titulo"),
                            result.getString("mensaje"),
                            fechaSQL == null
                                    ? null
                                    : fechaSQL.toLocalDateTime(),
                            result.getBoolean("leida")
                    ));
                }
            }

            return notificaciones;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las notificaciones.", e);
        }
    }

    private void insertarNotificacion(
            Connection connection,
            int idUsuario,
            int idProveedor,
            String tipo,
            String titulo,
            String mensaje) throws SQLException {

        String sql = "INSERT INTO Notificacion_Proveedor "
                + "(id_usuario_destinatario, id_proveedor, tipo, titulo, mensaje) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idUsuario);
            statement.setInt(2, idProveedor);
            statement.setString(3, tipo);
            statement.setString(4, titulo);
            statement.setString(5, mensaje);
            statement.executeUpdate();
        }
    }

    // ---------------------------------------------------------
    // UTILIDADES
    // ---------------------------------------------------------

    private void revertir(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollback) {
            original.addSuppressed(rollback);
        }
    }

    private String condicionPendiente(String columna) {
        return "UPPER(REPLACE(REPLACE(TRIM(" + columna
                + "), ' ', '_'), '-', '_')) IN "
                + "('PENDIENTE', 'PENDIENTE_VERIFICACION', "
                + "'PENDIENTE_DE_VERIFICACION', 'PENDIENTE_REVISION', "
                + "'PENDIENTE_DE_REVISION', 'PENDIENTE_VERIFICACIÓN', "
                + "'PENDIENTE_DE_VERIFICACIÓN')";
    }

    private Proveedor mapearProveedor(ResultSet result) throws SQLException {
        return new Proveedor(
                result.getInt("id_proveedor"),
                result.getInt("id_usuario"),
                result.getString("nombre"),
                result.getString("empresa"),
                result.getString("correo"),
                result.getString("telefono"),
                result.getString("direccion"),
                result.getString("estado"),
                result.getString("motivo_rechazo")
        );
    }
}