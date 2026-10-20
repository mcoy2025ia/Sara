package repositories;

import config.DatabaseConnection;
import entities.Contratacion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos para el caso de uso CU-026.
 * Las solicitudes de organizadores se almacenan separadas
 * de las contrataciones de servicios de proveedores.
 */
public class RepositoryContratacion {

    private final String url;

    public RepositoryContratacion() {
        this(null);
    }

    public RepositoryContratacion(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null
                ? DatabaseConnection.getConnection()
                : DatabaseConnection.getConnection(url);
    }

    /**
     * Registra la solicitud y notifica al organizador
     * dentro de la misma transaccion.
     */
    public void guardarContratacion(Contratacion contratacion) {

        validarDatos(contratacion);

        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);

            try {
                validarCliente(connection, contratacion.getIdCliente());

                String nombreOrganizador = validarOrganizadorDisponible(
                        connection,
                        contratacion.getIdOrganizador());

                String sql =
                        "INSERT INTO Solicitud_Contratacion_Organizador "
                        + "(id_cliente, id_organizador, tipo_evento, ciudad, "
                        + "fecha_aproximada, presupuesto_estimado, "
                        + "criterios_busqueda, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE')";

                int idGenerado;

                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

                    statement.setInt(1, contratacion.getIdCliente());
                    statement.setInt(2, contratacion.getIdOrganizador());
                    statement.setString(3, contratacion.getTipoEvento().trim());
                    statement.setString(4, contratacion.getCiudad().trim());
                    statement.setDate(
                            5, Date.valueOf(contratacion.getFechaAproximada()));
                    statement.setBigDecimal(
                            6, contratacion.getPresupuestoEstimado());

                    String criterios = contratacion.getCriteriosBusqueda();

                    if (criterios == null || criterios.isBlank()) {
                        statement.setNull(7, java.sql.Types.VARCHAR);
                    } else {
                        statement.setString(7, criterios.trim());
                    }

                    statement.executeUpdate();

                    try (ResultSet generated = statement.getGeneratedKeys()) {
                        if (!generated.next()) {
                            throw new SQLException(
                                    "No se genero el identificador de la solicitud.");
                        }

                        idGenerado = generated.getInt(1);
                    }
                }

                String mensaje =
                        "Nueva solicitud de contratacion de organizador. "
                        + "Tipo de evento: "
                        + contratacion.getTipoEvento().trim()
                        + ". Ciudad: "
                        + contratacion.getCiudad().trim()
                        + ". Revisa las solicitudes pendientes.";

                notificar(
                        connection,
                        contratacion.getIdOrganizador(),
                        mensaje);

                contratacion.setIdContratacion(idGenerado);
                contratacion.setEstado("PENDIENTE");

                cargarFechaSolicitud(connection, contratacion);

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
            e.printStackTrace();
            
            throw new IllegalStateException(
                    "No fue posible registrar la solicitud de contratacion. "
                    + "No se guardaron cambios parciales. Puedes intentarlo nuevamente.",
                    e);
        }
    }

    /**
     * Consulta una solicitud por su identificador.
     */
    public Contratacion buscarContratacion(int idContratacion) {

        String sql =
                "SELECT * FROM Solicitud_Contratacion_Organizador "
                + "WHERE id_solicitud = ?";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idContratacion);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar la solicitud de contratacion.", e);
        }
    }

    /**
     * Consulta las solicitudes registradas por un cliente.
     */
    public List<Contratacion> listarContrataciones(int idCliente) {

        if (idCliente <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del cliente no es valido.");
        }

        String sql =
                "SELECT * FROM Solicitud_Contratacion_Organizador "
                + "WHERE id_cliente = ? ORDER BY fecha_solicitud DESC";

        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idCliente);

            try (ResultSet result = statement.executeQuery()) {
                List<Contratacion> lista = new ArrayList<>();

                while (result.next()) {
                    lista.add(convertir(result));
                }

                return lista;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las solicitudes del cliente.", e);
        }
    }

    /**
     * Permite filtrar las solicitudes por cliente, organizador,
     * estado, ciudad o tipo de evento.
     *
     * Claves admitidas: idCliente, idOrganizador, estado,
     * ciudad y tipoEvento.
     */
    public List<Contratacion> listarContrataciones(
            Map<String, Object> filtros) {

        StringBuilder sql = new StringBuilder(
                "SELECT * FROM Solicitud_Contratacion_Organizador "
                + "WHERE 1 = 1");

        List<Object> parametros = new ArrayList<>();

        if (filtros != null) {

            agregarFiltroNumerico(
                    sql, parametros, filtros, "idCliente", "id_cliente");

            agregarFiltroNumerico(
                    sql, parametros, filtros,
                    "idOrganizador", "id_organizador");

            agregarFiltroTexto(
                    sql, parametros, filtros, "estado", "estado");

            agregarFiltroTexto(
                    sql, parametros, filtros, "ciudad", "ciudad");

            agregarFiltroTexto(
                    sql, parametros, filtros,
                    "tipoEvento", "tipo_evento");
        }

        sql.append(" ORDER BY fecha_solicitud DESC");

        try (Connection connection = conectar();
             PreparedStatement statement =
                     connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                statement.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet result = statement.executeQuery()) {
                List<Contratacion> lista = new ArrayList<>();

                while (result.next()) {
                    lista.add(convertir(result));
                }

                return lista;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las solicitudes de contratacion.", e);
        }
    }

    private void validarDatos(Contratacion contratacion) {

        if (contratacion == null) {
            throw new IllegalArgumentException(
                    "La solicitud de contratacion es obligatoria.");
        }

        if (contratacion.getIdCliente() <= 0
                || contratacion.getIdOrganizador() <= 0) {

            throw new IllegalArgumentException(
                    "Selecciona un cliente y un organizador validos.");
        }

        if (contratacion.getTipoEvento() == null
                || contratacion.getTipoEvento().isBlank()
                || contratacion.getCiudad() == null
                || contratacion.getCiudad().isBlank()
                || contratacion.getFechaAproximada() == null
                || contratacion.getPresupuestoEstimado() == null) {

            throw new IllegalArgumentException(
                    "Completa el tipo de evento, ciudad, fecha y presupuesto.");
        }

        if (contratacion.getTipoEvento().trim().length() > 100
                || contratacion.getCiudad().trim().length() > 150
                || (contratacion.getCriteriosBusqueda() != null
                && contratacion.getCriteriosBusqueda().length() > 500)) {

            throw new IllegalArgumentException(
                    "Uno de los datos supera la longitud permitida.");
        }

        if (contratacion.getPresupuestoEstimado().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El presupuesto estimado no puede ser negativo.");
        }
    }

    private void validarCliente(Connection connection, int idCliente)
            throws SQLException {

        String sql =
                "SELECT tipo_usuario, estado FROM Usuario "
                + "WHERE id_usuario = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idCliente);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "El cliente seleccionado no existe.");
                }

                if (!"CLIENTE".equalsIgnoreCase(
                        result.getString("tipo_usuario"))) {

                    throw new IllegalArgumentException(
                            "La solicitud debe estar asociada a un cliente.");
                }

                if (!"ACTIVO".equalsIgnoreCase(result.getString("estado"))) {
                    throw new IllegalArgumentException(
                            "El cliente no tiene una cuenta activa.");
                }
            }
        }
    }

    private String validarOrganizadorDisponible(
            Connection connection, int idOrganizador) throws SQLException {

        String sql =
                "SELECT u.id_usuario, u.nombre, o.empresa "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ? "
                + "AND u.tipo_usuario = 'ORGANIZADOR' "
                + "AND u.estado = 'ACTIVO' "
                + "AND o.estado = 'ACTIVO' "
                + "AND o.estado_verificacion = 'VERIFICADO' "
                + "FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException(
                            "El organizador seleccionado ya no esta activo "
                            + "o no se encuentra verificado. Actualiza la lista.");
                }

                String empresa = result.getString("empresa");

                return empresa == null || empresa.isBlank()
                        ? result.getString("nombre")
                        : empresa;
            }
        }
    }

    
private void notificar(
        Connection connection,
        int idOrganizador,
        String mensaje) throws SQLException {

    // Detectar si la base local conserva la columna antigua.
    boolean tieneColumnaAntigua;

    String consultaColumna =
            "SELECT COUNT(*) "
            + "FROM INFORMATION_SCHEMA.COLUMNS "
            + "WHERE TABLE_SCHEMA = 'PUBLIC' "
            + "AND TABLE_NAME = 'NOTIFICACION_INTERNA' "
            + "AND COLUMN_NAME = 'ID_USUARIO_DESTINATARIO'";

    try (PreparedStatement consulta =
                 connection.prepareStatement(consultaColumna);
         ResultSet resultado = consulta.executeQuery()) {

        resultado.next();
        tieneColumnaAntigua = resultado.getInt(1) > 0;
    }

    String sql;

    if (tieneColumnaAntigua) {

        // Compatibilidad con la base local que conserva el campo antiguo.
        sql =
                "INSERT INTO Notificacion_interna "
                + "(id_usuario, id_usuario_destinatario, mensaje) "
                + "SELECT u.id_usuario, u.id_usuario, ? "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ?";

    } else {

        // Esquema actual de una base de datos limpia.
        sql =
                "INSERT INTO Notificacion_interna "
                + "(id_usuario, mensaje) "
                + "SELECT u.id_usuario, ? "
                + "FROM Organizador o "
                + "JOIN Usuario u ON u.id_usuario = o.id_usuario "
                + "WHERE o.id_organizador = ?";
    }

    try (PreparedStatement statement =
                 connection.prepareStatement(sql)) {

        statement.setString(1, mensaje);
        statement.setInt(2, idOrganizador);

        if (statement.executeUpdate() != 1) {
            throw new SQLException(
                    "No se pudo generar la notificación "
                    + "para el organizador seleccionado.");
        }
    }
}

    private void cargarFechaSolicitud(
            Connection connection, Contratacion contratacion)
            throws SQLException {

        String sql =
                "SELECT fecha_solicitud FROM Solicitud_Contratacion_Organizador "
                + "WHERE id_solicitud = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, contratacion.getIdContratacion());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    Timestamp fecha = result.getTimestamp("fecha_solicitud");

                    if (fecha != null) {
                        contratacion.setFechaSolicitud(fecha.toLocalDateTime());
                    }
                }
            }
        }
    }

    private Contratacion convertir(ResultSet result) throws SQLException {

        Contratacion contratacion = new Contratacion();

        contratacion.setIdContratacion(result.getInt("id_solicitud"));
        contratacion.setIdCliente(result.getInt("id_cliente"));
        contratacion.setIdOrganizador(result.getInt("id_organizador"));
        contratacion.setTipoEvento(result.getString("tipo_evento"));
        contratacion.setCiudad(result.getString("ciudad"));

        Date fecha = result.getDate("fecha_aproximada");
        if (fecha != null) {
            contratacion.setFechaAproximada(fecha.toLocalDate());
        }

        contratacion.setPresupuestoEstimado(
                result.getBigDecimal("presupuesto_estimado"));

        contratacion.setCriteriosBusqueda(
                result.getString("criterios_busqueda"));

        contratacion.setEstado(result.getString("estado"));

        Timestamp fechaSolicitud = result.getTimestamp("fecha_solicitud");
        if (fechaSolicitud != null) {
            contratacion.setFechaSolicitud(fechaSolicitud.toLocalDateTime());
        }

        return contratacion;
    }

    private void agregarFiltroNumerico(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros,
            String clave,
            String columna) {

        Object valor = filtros.get(clave);

        if (valor == null) {
            return;
        }

        if (!(valor instanceof Number)) {
            throw new IllegalArgumentException(
                    "El filtro " + clave + " debe ser numerico.");
        }

        sql.append(" AND ").append(columna).append(" = ?");
        parametros.add(((Number) valor).intValue());
    }

    private void agregarFiltroTexto(
            StringBuilder sql,
            List<Object> parametros,
            Map<String, Object> filtros,
            String clave,
            String columna) {

        Object valor = filtros.get(clave);

        if (valor == null || valor.toString().isBlank()) {
            return;
        }

        sql.append(" AND LOWER(").append(columna).append(") LIKE LOWER(?)");
        parametros.add("%" + valor.toString().trim() + "%");
    }
}
