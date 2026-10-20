package repositories;

import config.DatabaseConnection;
import entities.Invitacion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla Invitacion (CU-17). */
public class RepositoryInvitacion {
    private static final String COLUMNAS =
            "id_invitacion, id_evento, id_lugar, fecha_envio, mensaje, estado_respuesta";

    private final String url;

    public RepositoryInvitacion() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryInvitacion(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public void guardarInvitacion(Invitacion invitacion) {
        String sql = "INSERT INTO Invitacion (id_evento, id_lugar, fecha_envio, mensaje, estado_respuesta) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, invitacion.getIdEvento());
            statement.setInt(2, invitacion.getIdLugar());
            statement.setTimestamp(3, Timestamp.valueOf(invitacion.getFechaEnvio()));
            statement.setString(4, invitacion.getMensaje());
            statement.setString(5, invitacion.getEstadoRespuesta());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se generó el identificador de la invitación.");
                invitacion.setIdInvitacion(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar la invitación. Revisa la conexión con la base de datos.", e);
        }
    }

    public Invitacion buscarInvitacion(int idInvitacion) {
        String sql = "SELECT " + COLUMNAS + " FROM Invitacion WHERE id_invitacion = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idInvitacion);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la invitación. Revisa la conexión con la base de datos.", e);
        }
    }

    public void actualizarInvitacion(Invitacion invitacion) {
        String sql = "UPDATE Invitacion SET id_evento = ?, id_lugar = ?, fecha_envio = ?, mensaje = ?, "
                + "estado_respuesta = ? WHERE id_invitacion = ?";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, invitacion.getIdEvento());
            statement.setInt(2, invitacion.getIdLugar());
            statement.setTimestamp(3, Timestamp.valueOf(invitacion.getFechaEnvio()));
            statement.setString(4, invitacion.getMensaje());
            statement.setString(5, invitacion.getEstadoRespuesta());
            statement.setInt(6, invitacion.getIdInvitacion());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo actualizar la invitación. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Borrado físico. El flujo normal del CU-17 revoca la invitación en lugar de eliminarla. */
    public void eliminarInvitacion(int idInvitacion) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM Invitacion WHERE id_invitacion = ?")) {
            statement.setInt(1, idInvitacion);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo eliminar la invitación. Verifica que no tenga invitados o regalos asociados.", e);
        }
    }

    public List<Invitacion> listarInvitaciones(int idEvento) {
        String sql = "SELECT " + COLUMNAS + " FROM Invitacion WHERE id_evento = ? ORDER BY id_invitacion";
        List<Invitacion> invitaciones = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, idEvento);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) invitaciones.add(convertir(result));
            }
            return invitaciones;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar las invitaciones. Revisa la conexión con la base de datos.", e);
        }
    }

    private Invitacion convertir(ResultSet result) throws SQLException {
        return new Invitacion(result.getInt("id_invitacion"), result.getInt("id_evento"), result.getInt("id_lugar"),
                result.getTimestamp("fecha_envio").toLocalDateTime(), result.getString("mensaje"),
                result.getString("estado_respuesta"));
    }
}
