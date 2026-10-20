package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import config.DatabaseConnection;
import entities.SolicitudEjecucion;

public class RepositorySolicitudEjecucion {

    public int crearSolicitud(
            int idOrganizador,
            int idEvento,
            int idServicio,
            String descripcionNecesidad) {

        return crearSolicitudParaNecesidad(
                idOrganizador,
                idEvento,
                idServicio,
                0,
                descripcionNecesidad);
    }

    public int crearSolicitudParaNecesidad(
            int idOrganizador,
            int idEvento,
            int idServicio,
            int idNecesidadExistente,
            String descripcionNecesidad) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int idProveedor;
                java.math.BigDecimal costo;
                java.math.BigDecimal presupuesto;

                // Verificamos que el servicio este publicado
                // y que pertenezca a un proveedor aprobado
                String consultarServicio = """
                        SELECT s.id_proveedor, s.precio
                        FROM Servicio s
                        JOIN Proveedor p
                          ON p.id_proveedor = s.id_proveedor
                        WHERE s.id_servicio = ?
                          AND UPPER(s.estado) = 'DISPONIBLE'
                          AND UPPER(p.estado) IN ('APROBADO','VERIFICADO')
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarServicio)) {

                    statement.setInt(1, idServicio);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "El servicio no esta disponible "
                                    + "o el proveedor no esta aprobado.");
                        }

                        idProveedor = result.getInt("id_proveedor");
                        costo = result.getBigDecimal("precio");
                    }
                }

                // El organizador solo puede usar sus propios eventos
                String consultarEvento = """
                        SELECT id_evento, presupuesto
                        FROM Evento
                        WHERE id_evento = ?
                          AND id_organizador = ?
                          AND UPPER(estado) NOT IN
                              ('CANCELADO', 'FINALIZADO')
                        FOR UPDATE
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarEvento)) {

                    statement.setInt(1, idEvento);
                    statement.setInt(2, idOrganizador);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "El evento no pertenece al organizador "
                                    + "o ya esta cerrado.");
                        }
                        presupuesto = result.getBigDecimal("presupuesto");
                    }
                }

                // Los eventos de CU01 tienen presupuesto: aplicar la misma regla del formulario.
                // Las solicitudes históricas sin presupuesto conservan el flujo anterior de CU12.
                if (presupuesto != null) {
                    new RepositorySolicitudServicio().validarCompromiso(connection, idEvento, presupuesto, costo);
                }

                // Cada solicitud nueva comienza asociada a una necesidad
                String insertarNecesidad = """
                        INSERT INTO Necesidad_Servicio_Evento
                        (id_evento, descripcion, estado)
                        VALUES (?, ?, 'ABIERTA')
                        """;

                int idNecesidad;
                if (idNecesidadExistente < 0) {
                    throw new IllegalArgumentException(
                            "El identificador de la necesidad no es valido.");
                }

                if (idNecesidadExistente > 0) {

                    String consultarNecesidad = """
                            SELECT id_necesidad_servicio
                            FROM Necesidad_Servicio_Evento
                            WHERE id_necesidad_servicio = ?
                              AND id_evento = ?
                              AND UPPER(estado) = 'ABIERTA'
                              AND id_propuesta_seleccionada IS NULL
                            FOR UPDATE
                            """;

                    try (PreparedStatement statement
                            = connection.prepareStatement(consultarNecesidad)) {

                        statement.setInt(1, idNecesidadExistente);
                        statement.setInt(2, idEvento);

                        try (ResultSet result = statement.executeQuery()) {

                            if (!result.next()) {
                                throw new IllegalStateException(
                                        "La necesidad no existe, "
                                        + "no pertenece al evento "
                                        + "o ya fue cerrada.");
                            }
                        }
                    }

                    idNecesidad = idNecesidadExistente;

                } else {

                    try (PreparedStatement statement
                            = connection.prepareStatement(
                                    insertarNecesidad,
                                    Statement.RETURN_GENERATED_KEYS)) {

                        statement.setInt(1, idEvento);
                        statement.setString(2, descripcionNecesidad);

                        statement.executeUpdate();

                        try (ResultSet claves = statement.getGeneratedKeys()) {

                            if (!claves.next()) {
                                throw new IllegalStateException(
                                        "No se pudo crear la necesidad del evento.");
                            }

                            idNecesidad = claves.getInt(1);
                        }
                    }
                }

                // Evitamos solicitar dos veces al mismo proveedor
                // para cubrir la misma necesidad
                String consultarDuplicado = """
                        SELECT COUNT(*)
                        FROM Solicitud_Ejecucion
                        WHERE id_necesidad_servicio = ?
                          AND id_proveedor = ?
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarDuplicado)) {

                    statement.setInt(1, idNecesidad);
                    statement.setInt(2, idProveedor);

                    try (ResultSet result = statement.executeQuery()) {

                        result.next();

                        if (result.getInt(1) > 0) {
                            throw new IllegalStateException(
                                    "Ya existe una solicitud para este "
                                    + "proveedor y esta necesidad.");
                        }
                    }
                }

                String insertarSolicitud = """
                        INSERT INTO Solicitud_Ejecucion
                        (id_proveedor, id_evento, id_servicio,
                         id_necesidad_servicio, fecha_solicitud, estado, costo_comprometido)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, 'PENDIENTE', ?)
                        """;

                int idSolicitud;

                try (PreparedStatement statement
                        = connection.prepareStatement(
                                insertarSolicitud,
                                Statement.RETURN_GENERATED_KEYS)) {

                    statement.setInt(1, idProveedor);
                    statement.setInt(2, idEvento);
                    statement.setInt(3, idServicio);
                    statement.setInt(4, idNecesidad);
                    statement.setBigDecimal(5, costo);

                    statement.executeUpdate();

                    try (ResultSet claves = statement.getGeneratedKeys()) {

                        if (!claves.next()) {
                            throw new IllegalStateException(
                                    "No se pudo crear la solicitud.");
                        }

                        idSolicitud = claves.getInt(1);
                    }
                }

                connection.commit();

                return idSolicitud;

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
                    "No se pudo registrar la solicitud.", e);
        }
    }

    public List<String> listarNecesidadesOrganizador(
            int idOrganizador) {

        List<String> necesidades = new ArrayList<>();

        String sql = """
                SELECT n.id_necesidad_servicio,
                       n.id_evento,
                       n.descripcion
                FROM Necesidad_Servicio_Evento n
                JOIN Evento e
                  ON e.id_evento = n.id_evento
                WHERE e.id_organizador = ?
                  AND UPPER(n.estado) = 'ABIERTA'
                  AND n.id_propuesta_seleccionada IS NULL
                ORDER BY n.id_necesidad_servicio
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idOrganizador);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    necesidades.add(
                            result.getInt("id_necesidad_servicio")
                            + " | Evento "
                            + result.getInt("id_evento")
                            + " | "
                            + result.getString("descripcion"));
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las necesidades del organizador.",
                    e);
        }

        return necesidades;
    }

    public List<SolicitudEjecucion> listarPorProveedor(int idProveedor) {

        List<SolicitudEjecucion> solicitudes = new ArrayList<>();

        String sql = """
                SELECT id_solicitud,
                       id_proveedor,
                       id_evento,
                       id_servicio,
                       fecha_solicitud,
                       estado,
                       motivo_rechazo,
                       fecha_respuesta
                FROM Solicitud_Ejecucion
                WHERE id_proveedor = ?
                ORDER BY fecha_solicitud DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idProveedor);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {

                    SolicitudEjecucion solicitud = new SolicitudEjecucion();

                    solicitud.setIdSolicitud(result.getInt("id_solicitud"));
                    solicitud.setIdProveedor(result.getInt("id_proveedor"));
                    solicitud.setIdEvento(result.getInt("id_evento"));
                    solicitud.setIdServicio(result.getInt("id_servicio"));

                    solicitud.setFechaSolicitud(
                            result.getTimestamp("fecha_solicitud").toLocalDateTime());

                    solicitud.setEstado(result.getString("estado"));
                    solicitud.setMotivoRechazo(result.getString("motivo_rechazo"));

                    if (result.getTimestamp("fecha_respuesta") != null) {
                        solicitud.setFechaRespuesta(
                                result.getTimestamp("fecha_respuesta").toLocalDateTime());
                    }

                    solicitudes.add(solicitud);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar las solicitudes del proveedor.", e);
        }

        return solicitudes;
    }

    public boolean rechazarSolicitud(
            int idProveedor,
            int idSolicitud,
            String motivo) {

        String sql = """
                UPDATE Solicitud_Ejecucion
                SET estado = 'RECHAZADA',
                    motivo_rechazo = ?,
                    fecha_respuesta = CURRENT_TIMESTAMP
                WHERE id_solicitud = ?
                  AND id_proveedor = ?
                  AND estado = 'PENDIENTE'
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, motivo);
            statement.setInt(2, idSolicitud);
            statement.setInt(3, idProveedor);

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo rechazar la solicitud.", e);
        }
    }
}
