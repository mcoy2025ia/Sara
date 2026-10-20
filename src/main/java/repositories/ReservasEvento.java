package repositories;

import java.sql.*;

/** Reservas compartidas por cancelación y finalización; no modifica invitaciones. */
final class ReservasEvento {
    private ReservasEvento() {}
    static void preparar(Connection connection, int evento) throws SQLException {
                // CU11 identifica el horario contratado por servicio, fecha y horas exactas.
                actualizar(connection, "UPDATE Contratacion c SET id_disponibilidad=(SELECT d.id_disponibilidad "
                        + "FROM Disponibilidad_Servicio d JOIN Propuesta p ON p.id_propuesta=c.id_propuesta "
                        + "JOIN Solicitud_Ejecucion s ON s.id_solicitud=p.id_solicitud "
                        + "WHERE d.id_servicio=s.id_servicio AND d.fecha=c.fecha_servicio "
                        + "AND d.hora_inicio=c.hora_inicio AND d.hora_fin=c.hora_fin) "
                        + "WHERE c.id_evento=? AND c.id_disponibilidad IS NULL "
                        + "AND UPPER(c.estado) IN ('ACTIVA','CONFIRMADA')", evento);
                comprobarReservas(connection, evento);
                // Bloquear también los recursos evita liberaciones incoherentes entre cancelaciones simultáneas.
                actualizar(connection, "UPDATE Disponibilidad_Servicio SET estado=estado WHERE id_disponibilidad IN "
                        + "(SELECT id_disponibilidad FROM Contratacion WHERE id_evento=? AND UPPER(estado) IN ('ACTIVA','CONFIRMADA'))", evento);
                actualizar(connection, "UPDATE Lugar SET estado=estado WHERE id_lugar IN (SELECT id_lugar FROM Evento WHERE id_evento=?)", evento);
    }
    static void liberar(Connection connection, int evento) throws SQLException {
                // Libera solo las reservas de este evento que no tengan otro compromiso activo.
                actualizar(connection, "UPDATE Disponibilidad_Servicio d SET estado='DISPONIBLE' WHERE "
                        + "d.id_disponibilidad IN (SELECT id_disponibilidad FROM Contratacion WHERE id_evento=? AND UPPER(estado) IN ('ACTIVA','CONFIRMADA')) "
                        + "AND NOT EXISTS (SELECT 1 FROM Contratacion c "
                        + "JOIN Propuesta p ON p.id_propuesta=c.id_propuesta "
                        + "JOIN Solicitud_Ejecucion s ON s.id_solicitud=p.id_solicitud "
                        + "WHERE UPPER(c.estado) IN ('ACTIVA','CONFIRMADA') AND c.id_evento<>? "
                        + "AND (c.id_disponibilidad=d.id_disponibilidad OR (c.id_disponibilidad IS NULL "
                        + "AND s.id_servicio=d.id_servicio AND c.fecha_servicio=d.fecha "
                        + "AND c.hora_inicio=d.hora_inicio AND c.hora_fin=d.hora_fin)))", evento, evento);
                actualizar(connection, "UPDATE Lugar l SET estado='DISPONIBLE' WHERE l.id_lugar IN "
                        + "(SELECT id_lugar FROM Evento WHERE id_evento=?) AND NOT EXISTS "
                        + "(SELECT 1 FROM Evento e WHERE e.id_lugar=l.id_lugar AND e.id_evento<>? "
                        + "AND NOT EXISTS (SELECT 1 FROM Finalizacion_evento f WHERE f.id_evento=e.id_evento) AND UPPER(e.estado) NOT IN ('CANCELADO','FINALIZADO'))", evento, evento);
    }
    private static void comprobarReservas(Connection connection, int evento) throws SQLException {
        String sql = "SELECT c.id_contratacion FROM Contratacion c JOIN Propuesta p ON p.id_propuesta=c.id_propuesta "
                + "JOIN Solicitud_Ejecucion s ON s.id_solicitud=p.id_solicitud "
                + "LEFT JOIN Disponibilidad_Servicio d ON d.id_disponibilidad=c.id_disponibilidad "
                + "LEFT JOIN Servicio v ON v.id_servicio=d.id_servicio "
                + "WHERE c.id_evento=? AND UPPER(c.estado) IN ('ACTIVA','CONFIRMADA') "
                + "AND (d.id_disponibilidad IS NULL OR v.id_proveedor<>s.id_proveedor OR s.id_evento<>c.id_evento "
                + "OR (s.id_servicio IS NOT NULL AND s.id_servicio<>d.id_servicio) "
                + "OR (c.fecha_servicio IS NOT NULL AND c.fecha_servicio<>d.fecha) "
                + "OR (c.hora_inicio IS NOT NULL AND c.hora_inicio<>d.hora_inicio) "
                + "OR (c.hora_fin IS NOT NULL AND c.hora_fin<>d.hora_fin))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, evento);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) throw new IllegalArgumentException(
                        "Hay una contratación sin una reserva válida. Revisa su disponibilidad antes de gestionar el evento.");
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT i.id_lugar FROM Invitacion i JOIN Lugar l ON l.id_lugar=i.id_lugar "
                + "JOIN Evento e ON e.id_evento=i.id_evento WHERE e.id_evento=? AND (e.id_lugar IS NULL OR e.id_lugar<>i.id_lugar) AND l.estado='RESERVADO'")) {
            statement.setInt(1, evento);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) throw new IllegalArgumentException("Vincula la reserva del lugar al evento antes de gestionar el evento.");
            }
        }
    }

    private static void actualizar(Connection connection, String sql, int... valores) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i=0; i<valores.length; i++) statement.setInt(i+1,valores[i]);
            statement.executeUpdate();
        }
    }

}
