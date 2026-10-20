package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import config.DatabaseConnection;
import entities.MensajeSolicitud;

public class RepositoryMensajesSolicitud {

    private void verificarParticipante(
            Connection connection,
            int idSolicitud,
            int idUsuario) throws SQLException {

        String sql = """
                SELECT s.id_solicitud
                FROM Solicitud_Ejecucion s
                JOIN Evento e
                  ON e.id_evento = s.id_evento
                JOIN Organizador o
                  ON o.id_organizador = e.id_organizador
                JOIN Proveedor p
                  ON p.id_proveedor = s.id_proveedor
                WHERE s.id_solicitud = ?
                  AND (o.id_usuario = ? OR p.id_usuario = ?)
                """;

        try (PreparedStatement statement
                = connection.prepareStatement(sql)) {

            statement.setInt(1, idSolicitud);
            statement.setInt(2, idUsuario);
            statement.setInt(3, idUsuario);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    throw new IllegalStateException(
                            "No tienes permiso para acceder "
                            + "a esta conversacion.");
                }
            }
        }
    }

    public List<MensajeSolicitud> listarMensajes(
            int idSolicitud,
            int idUsuario) {

        List<MensajeSolicitud> mensajes = new ArrayList<>();

        String sql = """
                SELECT m.id_mensaje,
                       m.id_usuario_remitente,
                       m.contenido,
                       m.adjunto_url,
                       m.fecha_hora
                FROM Mensaje m
                JOIN Conversacion c
                  ON c.id_conversacion = m.id_conversacion
                WHERE c.id_solicitud = ?
                ORDER BY m.fecha_hora, m.id_mensaje
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            verificarParticipante(connection, idSolicitud, idUsuario);

            try (PreparedStatement statement
                    = connection.prepareStatement(sql)) {

                statement.setInt(1, idSolicitud);

                try (ResultSet result = statement.executeQuery()) {

                    while (result.next()) {

                        MensajeSolicitud mensaje = new MensajeSolicitud();

                        mensaje.setIdMensaje(
                                result.getInt("id_mensaje"));

                        mensaje.setIdSolicitud(idSolicitud);

                        mensaje.setIdUsuarioRemitente(
                                result.getInt("id_usuario_remitente"));

                        mensaje.setContenido(
                                result.getString("contenido"));

                        mensaje.setAdjuntoUrl(
                                result.getString("adjunto_url"));

                        Timestamp fecha
                                = result.getTimestamp("fecha_hora");

                        if (fecha != null) {
                            mensaje.setFechaHora(fecha.toLocalDateTime());
                        }

                        mensajes.add(mensaje);
                    }
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudieron consultar los mensajes.", e);
        }

        return mensajes;
    }

    public void enviarMensaje(
            int idSolicitud,
            int idUsuario,
            String contenido,
            String adjuntoUrl) {

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                verificarParticipante(
                        connection,
                        idSolicitud,
                        idUsuario);

                String consultarEstado = """
                        SELECT estado
                        FROM Solicitud_Ejecucion
                        WHERE id_solicitud = ?
                        FOR UPDATE
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(consultarEstado)) {

                    statement.setInt(1, idSolicitud);

                    try (ResultSet result = statement.executeQuery()) {

                        if (!result.next()) {
                            throw new IllegalStateException(
                                    "La solicitud no existe.");
                        }

                        String estado = result.getString("estado");

                        if (!"PENDIENTE".equalsIgnoreCase(estado)
                                && !"EN_NEGOCIACION".equalsIgnoreCase(estado)) {

                            throw new IllegalStateException(
                                    "La negociacion ya esta cerrada.");
                        }
                    }
                }

                int idConversacion = 0;

                String buscarConversacion = """
                        SELECT id_conversacion
                        FROM Conversacion
                        WHERE id_solicitud = ?
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(buscarConversacion)) {

                    statement.setInt(1, idSolicitud);

                    try (ResultSet result = statement.executeQuery()) {

                        if (result.next()) {
                            idConversacion = result.getInt(1);
                        }
                    }
                }

                if (idConversacion == 0) {

                    String crearConversacion = """
                            INSERT INTO Conversacion
                            (id_solicitud, fecha_creacion, estado)
                            VALUES (?, CURRENT_TIMESTAMP, 'ACTIVA')
                            """;

                    try (PreparedStatement statement
                            = connection.prepareStatement(
                                    crearConversacion,
                                    Statement.RETURN_GENERATED_KEYS)) {

                        statement.setInt(1, idSolicitud);
                        statement.executeUpdate();

                        try (ResultSet claves = statement.getGeneratedKeys()) {

                            if (!claves.next()) {
                                throw new IllegalStateException(
                                        "No se pudo crear la conversacion.");
                            }

                            idConversacion = claves.getInt(1);
                        }
                    }
                }

                String insertarMensaje = """
                        INSERT INTO Mensaje
                        (id_conversacion, id_usuario_remitente,
                         contenido, adjunto_url, fecha_hora)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """;

                try (PreparedStatement statement
                        = connection.prepareStatement(insertarMensaje)) {

                    statement.setInt(1, idConversacion);
                    statement.setInt(2, idUsuario);
                    statement.setString(3, contenido);
                    statement.setString(4, adjuntoUrl);
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
                    "No se pudo enviar el mensaje.", e);
        }
    }
}
