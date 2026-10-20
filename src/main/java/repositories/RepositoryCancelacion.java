package repositories;

import config.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositoryCancelacion {
    private final String url;
    public RepositoryCancelacion() { this(null); }
    public RepositoryCancelacion(String url) { this.url = url; }
    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public void cancelar(int evento, int organizador) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement consulta = connection.prepareStatement(
                        "SELECT estado FROM Evento WHERE id_evento=? AND id_organizador=? FOR UPDATE")) {
                    consulta.setInt(1, evento); consulta.setInt(2, organizador);
                    try (ResultSet result = consulta.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("Selecciona un evento existente de tu cuenta.");
                        String estado = result.getString(1);
                        if ("CANCELADO".equalsIgnoreCase(estado) || "FINALIZADO".equalsIgnoreCase(estado))
                            throw new IllegalArgumentException("No puedes cancelar un evento cancelado o finalizado.");
                    }
                }
                try (PreparedStatement pendiente = connection.prepareStatement(
                        "SELECT id_evento FROM Finalizacion_evento WHERE id_evento=?")) {
                    pendiente.setInt(1, evento);
                    try (ResultSet result = pendiente.executeQuery()) {
                        if (result.next()) throw new IllegalArgumentException("El evento tiene una finalización iniciada. Reintenta el envío de su galería.");
                    }
                }
                ReservasEvento.preparar(connection, evento);
                ReservasEvento.liberar(connection, evento);
                actualizar(connection, "INSERT INTO Notificacion_interna (id_usuario,id_evento,mensaje) "
                        + "SELECT DISTINCT p.id_usuario,?, 'Se canceló un evento asociado a tus solicitudes o contrataciones. Consulta tus avisos.' "
                        + "FROM Proveedor p JOIN Solicitud_Ejecucion s ON s.id_proveedor=p.id_proveedor "
                        + "WHERE s.id_evento=? AND (UPPER(s.estado) NOT IN ('RECHAZADA','CANCELADA','RETIRADA') OR EXISTS "
                        + "(SELECT 1 FROM Contratacion c JOIN Propuesta pr ON pr.id_propuesta=c.id_propuesta "
                        + "WHERE pr.id_solicitud=s.id_solicitud AND c.id_evento=s.id_evento AND UPPER(c.estado) IN ('ACTIVA','CONFIRMADA')))", evento, evento);
                actualizar(connection, "INSERT INTO Notificacion_interna (id_invitado,id_evento,mensaje) "
                        + "SELECT g.id_invitado,?, 'El evento al que estabas invitado fue cancelado. Tu invitación quedó revocada.' "
                        + "FROM Invitado g JOIN Invitacion i ON i.id_invitacion=g.id_invitacion "
                        + "WHERE i.id_evento=? AND (i.estado_respuesta IS NULL OR i.estado_respuesta NOT IN ('BORRADOR','REVOCADA'))", evento, evento);
                actualizar(connection, "UPDATE Contratacion SET estado='CANCELADA' WHERE id_evento=? AND UPPER(estado) IN ('ACTIVA','CONFIRMADA')", evento);
                actualizar(connection, "UPDATE Invitacion SET estado_respuesta='REVOCADA' WHERE id_evento=? "
                        + "AND (estado_respuesta IS NULL OR estado_respuesta NOT IN ('BORRADOR','REVOCADA'))", evento);
                actualizar(connection, "UPDATE Solicitud_Ejecucion SET estado='CANCELADA' WHERE id_evento=? "
                        + "AND UPPER(estado) NOT IN ('RECHAZADA','CANCELADA','RETIRADA')", evento);
                actualizar(connection, "UPDATE Necesidad_Servicio_Evento SET estado='CANCELADA' WHERE id_evento=?", evento);
                actualizar(connection, "UPDATE Evento SET estado='CANCELADO' WHERE id_evento=?", evento);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo completar la cancelación. No se guardaron cambios; vuelve a intentarlo.", e);
        }
    }

    private void actualizar(Connection connection, String sql, int... valores) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i=0; i<valores.length; i++) statement.setInt(i+1,valores[i]);
            statement.executeUpdate();
        }
    }

    public List<String> listarAvisos(int evento, int organizador) {
        List<String> avisos = new ArrayList<>();
        try (Connection connection=conectar(); PreparedStatement statement=connection.prepareStatement(
                "SELECT n.mensaje FROM Notificacion_interna n JOIN Evento e ON e.id_evento=n.id_evento "
                + "WHERE n.id_evento=? AND e.id_organizador=? ORDER BY n.id_notificacion")) {
            statement.setInt(1,evento); statement.setInt(2,organizador);
            try (ResultSet result=statement.executeQuery()) { while(result.next()) avisos.add(result.getString(1)); }
            return avisos;
        } catch (SQLException e) { throw new IllegalStateException("No se pudieron cargar los avisos. Vuelve a intentarlo.",e); }
    }
}
