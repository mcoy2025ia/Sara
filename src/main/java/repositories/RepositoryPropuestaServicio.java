package repositories;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import config.DatabaseConnection;
import entities.PropuestaServicio;

public class RepositoryPropuestaServicio {

    public List<PropuestaServicio> listarHistorial(
            int idProveedor,
            int idSolicitud) {

        List<PropuestaServicio> propuestas = new ArrayList<>();

        String sql = """
                SELECT p.id_propuesta,
                       p.id_solicitud,
                       p.numero_version,
                       p.valor,
                       p.descripcion,
                       p.condiciones,
                                              CASE
                           WHEN n.id_propuesta_seleccionada = p.id_propuesta
                           THEN 'SELECCIONADA'
                           ELSE p.estado
                       END AS estado,
                       p.vigente,
                       p.fecha_envio
                FROM Propuesta p
                JOIN Solicitud_Ejecucion s
                  ON s.id_solicitud = p.id_solicitud
                LEFT JOIN Necesidad_Servicio_Evento n
                  ON n.id_necesidad_servicio = s.id_necesidad_servicio
                WHERE s.id_solicitud = ?
                  AND s.id_proveedor = ?
                ORDER BY p.numero_version DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idSolicitud);
            statement.setInt(2, idProveedor);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    PropuestaServicio propuesta = new PropuestaServicio();

                    propuesta.setIdPropuesta(
                            result.getInt("id_propuesta"));

                    propuesta.setIdSolicitud(
                            result.getInt("id_solicitud"));

                    propuesta.setNumeroVersion(
                            result.getInt("numero_version"));

                    propuesta.setValor(
                            result.getBigDecimal("valor"));

                    propuesta.setDescripcion(
                            result.getString("descripcion"));

                    propuesta.setCondiciones(
                            result.getString("condiciones"));

                    propuesta.setEstado(
                            result.getString("estado"));

                    propuesta.setVigente(
                            result.getBoolean("vigente"));

                    Timestamp fecha
                            = result.getTimestamp("fecha_envio");

                    if (fecha != null) {
                        propuesta.setFechaEnvio(fecha.toLocalDateTime());
                    }

                    propuestas.add(propuesta);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo consultar el historial de propuestas.",
                    e);
        }

        return propuestas;
    }

    public List<PropuestaServicio> listarPropuestasOrganizador(
            int idOrganizador) {

        List<PropuestaServicio> propuestas = new ArrayList<>();

        String sql = """
                SELECT p.id_propuesta,
                       p.id_solicitud,
                       p.numero_version,
                       p.valor,
                       p.descripcion,
                       p.condiciones,
                       CASE
                           WHEN n.id_propuesta_seleccionada = p.id_propuesta
                           THEN 'SELECCIONADA'
                           ELSE p.estado
                       END AS estado,
                       p.vigente,
                       p.fecha_envio
                FROM Propuesta p
                JOIN Solicitud_Ejecucion s
                  ON s.id_solicitud = p.id_solicitud
                JOIN Evento e
                  ON e.id_evento = s.id_evento
                LEFT JOIN Necesidad_Servicio_Evento n
                  ON n.id_necesidad_servicio = s.id_necesidad_servicio
                WHERE e.id_organizador = ?
                  AND p.vigente = TRUE
                  AND s.estado = 'EN_NEGOCIACION'
                ORDER BY p.fecha_envio DESC, p.id_propuesta DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    PropuestaServicio propuesta = new PropuestaServicio();

                    propuesta.setIdPropuesta(
                            result.getInt("id_propuesta"));

                    propuesta.setIdSolicitud(
                            result.getInt("id_solicitud"));

                    propuesta.setNumeroVersion(
                            result.getInt("numero_version"));

                    propuesta.setValor(
                            result.getBigDecimal("valor"));

                    propuesta.setDescripcion(
                            result.getString("descripcion"));

                    propuesta.setCondiciones(
                            result.getString("condiciones"));

                    propuesta.setEstado(
                            result.getString("estado"));

                    propuesta.setVigente(
                            result.getBoolean("vigente"));

                    Timestamp fecha
                            = result.getTimestamp("fecha_envio");

                    if (fecha != null) {
                        propuesta.setFechaEnvio(
                                fecha.toLocalDateTime());
                    }

                    propuestas.add(propuesta);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las propuestas del organizador.",
                    e);
        }

        return propuestas;
    }

    public void rechazarPropuesta(
            int idOrganizador,
            int idPropuesta) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int idSolicitud;

                String consultar = """
                        SELECT s.id_solicitud
                        FROM Propuesta p
                        JOIN Solicitud_Ejecucion s
                          ON s.id_solicitud = p.id_solicitud
                        JOIN Evento e
                          ON e.id_evento = s.id_evento
                        LEFT JOIN Necesidad_Servicio_Evento n
                          ON n.id_necesidad_servicio = s.id_necesidad_servicio
                        WHERE p.id_propuesta = ?
                          AND e.id_organizador = ?
                          AND p.vigente = TRUE
                          AND UPPER(s.estado) = 'EN_NEGOCIACION'
                          AND (n.id_propuesta_seleccionada IS NULL
                               OR n.id_propuesta_seleccionada <> p.id_propuesta)
                        FOR UPDATE
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultar)) {

                    statement.setInt(1, idPropuesta);
                    statement.setInt(2, idOrganizador);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "La propuesta no se puede rechazar "
                                    + "o ya fue seleccionada.");
                        }

                        idSolicitud = result.getInt(1);
                    }
                }

                String rechazar = """
                        UPDATE Propuesta
                        SET estado = 'RECHAZADA',
                            vigente = FALSE
                        WHERE id_propuesta = ?
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(rechazar)) {

                    statement.setInt(1, idPropuesta);
                    statement.executeUpdate();
                }

                String cerrarSolicitud = """
                        UPDATE Solicitud_Ejecucion
                        SET estado = 'PROPUESTA_RECHAZADA'
                        WHERE id_solicitud = ?
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(cerrarSolicitud)) {

                    statement.setInt(1, idSolicitud);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {

                try {
                    connection.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo rechazar la propuesta.", e);
        }
    }

    public void seleccionarPropuesta(
            int idOrganizador,
            int idPropuesta) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int idNecesidad;

                // Verificamos que sea una propuesta vigente
                // de un evento del organizador
                String consultar = """
                        SELECT s.id_necesidad_servicio
                        FROM Propuesta p
                        JOIN Solicitud_Ejecucion s
                          ON s.id_solicitud = p.id_solicitud
                        JOIN Evento e
                          ON e.id_evento = s.id_evento
                        WHERE p.id_propuesta = ?
                          AND e.id_organizador = ?
                          AND p.vigente = TRUE
                          AND UPPER(s.estado) = 'EN_NEGOCIACION'
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultar)) {

                    statement.setInt(1, idPropuesta);
                    statement.setInt(2, idOrganizador);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "La propuesta no existe, no pertenece "
                                    + "al organizador o no esta vigente.");
                        }

                        idNecesidad = result.getInt(1);

                        if (result.wasNull()) {
                            throw new IllegalStateException(
                                    "La propuesta no tiene una necesidad asociada.");
                        }
                    }
                }

                // Bloqueamos la necesidad mientras se cambia la seleccion
                String bloquearNecesidad = """
                        SELECT id_necesidad_servicio
                        FROM Necesidad_Servicio_Evento
                        WHERE id_necesidad_servicio = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(bloquearNecesidad)) {

                    statement.setInt(1, idNecesidad);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "La necesidad ya no existe.");
                        }
                    }
                }

                // No se puede cambiar una seleccion ya contratada
                String consultarContratacion = """
                        SELECT COUNT(*)
                        FROM Contratacion c
                        JOIN Propuesta p
                          ON p.id_propuesta = c.id_propuesta
                        JOIN Solicitud_Ejecucion s
                          ON s.id_solicitud = p.id_solicitud
                        WHERE s.id_necesidad_servicio = ?
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarContratacion)) {

                    statement.setInt(1, idNecesidad);

                    try (ResultSet result = statement.executeQuery()) {

                        result.next();

                        if (result.getInt(1) > 0) {
                            throw new IllegalStateException(
                                    "Esta necesidad ya tiene una contratacion.");
                        }
                    }
                }

                // Guardamos una sola propuesta seleccionada por necesidad
                String actualizarNecesidad = """
                        UPDATE Necesidad_Servicio_Evento
                        SET id_propuesta_seleccionada = ?,
                            estado = 'SELECCIONADA'
                        WHERE id_necesidad_servicio = ?
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(actualizarNecesidad)) {

                    statement.setInt(1, idPropuesta);
                    statement.setInt(2, idNecesidad);
                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {

                try {
                    connection.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo seleccionar la propuesta.", e);
        }
    }

    public int guardarNuevaVersion(
            int idProveedor,
            int idSolicitud,
            BigDecimal valor,
            String descripcion,
            String condiciones) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                // Bloqueamos la solicitud mientras se prepara la nueva version
                // para que dos operaciones no calculen el mismo numero
                String consultarSolicitud = """
                    SELECT estado
                    FROM Solicitud_Ejecucion
                    WHERE id_solicitud = ?
                      AND id_proveedor = ?
                    FOR UPDATE
                    """;

                String estado;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarSolicitud)) {

                    statement.setInt(1, idSolicitud);
                    statement.setInt(2, idProveedor);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "La solicitud no pertenece al proveedor.");
                        }

                        estado = result.getString("estado");
                    }
                }

                if (!"PENDIENTE".equalsIgnoreCase(estado)
                        && !"EN_NEGOCIACION".equalsIgnoreCase(estado)) {

                    throw new IllegalStateException(
                            "La solicitud no permite enviar propuestas.");
                }

                // Si ya existe una propuesta seleccionada para esta necesidad,
                // no permitimos reemplazarla mediante una version nueva
                String consultarSeleccion = """
                        SELECT COUNT(*)
                        FROM Necesidad_Servicio_Evento n
                        JOIN Solicitud_Ejecucion s
                          ON s.id_necesidad_servicio = n.id_necesidad_servicio
                        WHERE s.id_solicitud = ?
                          AND n.id_propuesta_seleccionada IS NOT NULL
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarSeleccion)) {

                    statement.setInt(1, idSolicitud);

                    try (ResultSet result = statement.executeQuery()) {

                        result.next();

                        if (result.getInt(1) > 0) {
                            throw new IllegalStateException(
                                    "La necesidad ya tiene una propuesta "
                                    + "seleccionada. No puedes crear otra version.");
                        }
                    }
                }
                // Calculamos la siguiente version sin borrar las anteriores
                int siguienteVersion = 1;

                String consultarVersion = """
                    SELECT COALESCE(MAX(numero_version), 0) + 1
                    FROM Propuesta
                    WHERE id_solicitud = ?
                    """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarVersion)) {

                    statement.setInt(1, idSolicitud);

                    try (ResultSet result = statement.executeQuery()) {

                        if (result.next()) {
                            siguienteVersion = result.getInt(1);
                        }
                    }
                }

                // La version anterior deja de ser la actual
                String desactivarAnterior = """
                    UPDATE Propuesta
                    SET vigente = FALSE
                    WHERE id_solicitud = ?
                      AND vigente = TRUE
                    """;

                try (PreparedStatement statement
                        = connection.prepareStatement(desactivarAnterior)) {

                    statement.setInt(1, idSolicitud);
                    statement.executeUpdate();
                }

                String insertarPropuesta = """
                    INSERT INTO Propuesta
                    (id_solicitud, valor, descripcion, estado,
                     numero_version, condiciones, fecha_envio, vigente)
                    VALUES (?, ?, ?, 'ENVIADA', ?, ?, CURRENT_TIMESTAMP, TRUE)
                    """;

                int idPropuesta;

                try (PreparedStatement statement
                        = connection.prepareStatement(
                                insertarPropuesta,
                                Statement.RETURN_GENERATED_KEYS)) {

                    statement.setInt(1, idSolicitud);
                    statement.setBigDecimal(2, valor);
                    statement.setString(3, descripcion);
                    statement.setInt(4, siguienteVersion);
                    statement.setString(5, condiciones);

                    statement.executeUpdate();

                    try (ResultSet claves = statement.getGeneratedKeys()) {

                        if (!claves.next()) {
                            throw new IllegalStateException(
                                    "No se pudo obtener el ID de la propuesta.");
                        }

                        idPropuesta = claves.getInt(1);
                    }
                }

                String actualizarSolicitud = """
                    UPDATE Solicitud_Ejecucion
                    SET estado = 'EN_NEGOCIACION',
                        fecha_respuesta = COALESCE(
                            fecha_respuesta, CURRENT_TIMESTAMP)
                    WHERE id_solicitud = ?
                      AND id_proveedor = ?
                    """;

                try (PreparedStatement statement
                        = connection.prepareStatement(actualizarSolicitud)) {

                    statement.setInt(1, idSolicitud);
                    statement.setInt(2, idProveedor);
                    statement.executeUpdate();
                }

                // Todos los cambios se confirman juntos
                connection.commit();

                return idPropuesta;

            } catch (SQLException | RuntimeException e) {

                try {
                    connection.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                throw e;
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo guardar la propuesta.", e);
        }
    }
}
