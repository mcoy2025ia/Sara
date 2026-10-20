package repositories;

import config.DatabaseConnection;
import entities.Regalo;
import entities.ReservaRegalo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acceso a la tabla Regalo (CU-20). La reserva activa de cada regalo se toma de Reserva_regalo. */
public class RepositoryRegalo {
    /**
     * Una reserva activa por regalo. Si hay varias (datos antiguos), se prefiere la del
     * invitado indicado y después la más antigua.
     */
    private static final String SELECT = "SELECT r.id_regalo, r.id_invitacion, r.nombre, r.descripcion, r.estado, "
            + "r.precio_estimado, rr.id_invitado AS id_invitado_reserva, v.nombre AS reservado_por "
            + "FROM Regalo r "
            + "LEFT JOIN Reserva_regalo rr ON rr.id_reserva = (SELECT x.id_reserva FROM Reserva_regalo x "
            + "WHERE x.id_regalo = r.id_regalo AND UPPER(x.estado) = '" + ReservaRegalo.ESTADO_RESERVADO + "' "
            + "ORDER BY CASE WHEN x.id_invitado = ? THEN 0 ELSE 1 END, x.id_reserva FETCH FIRST 1 ROWS ONLY) "
            + "LEFT JOIN Invitado v ON v.id_invitado = rr.id_invitado ";

    private final String url;

    public RepositoryRegalo() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryRegalo(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public void guardarRegalo(Regalo regalo) {
        String sql = "INSERT INTO Regalo (id_invitacion, nombre, descripcion, estado, precio_estimado) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, regalo.getIdInvitacion());
            statement.setString(2, regalo.getNombre());
            statement.setString(3, regalo.getDescripcion());
            statement.setString(4, regalo.getEstado());
            statement.setBigDecimal(5, regalo.getPrecioEstimado());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No se generó el identificador del regalo.");
                regalo.setIdRegalo(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo guardar el regalo. Revisa la conexión con la base de datos.", e);
        }
    }

    public Regalo buscarRegalo(int idRegalo) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(SELECT + "WHERE r.id_regalo = ?")) {
            statement.setInt(1, 0);
            statement.setInt(2, idRegalo);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el regalo. Revisa la conexión con la base de datos.", e);
        }
    }

    public List<Regalo> listarRegalos(int idInvitacion) {
        return listarRegalos(idInvitacion, 0);
    }

    /** Regalos de la mesa de una invitación; si el invitado tiene reserva en un regalo, se muestra la suya. */
    public List<Regalo> listarRegalos(int idInvitacion, int idInvitado) {
        List<Regalo> regalos = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT + "WHERE r.id_invitacion = ? ORDER BY r.id_regalo")) {
            statement.setInt(1, idInvitado);
            statement.setInt(2, idInvitacion);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) regalos.add(convertir(result));
            }
            return regalos;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar la mesa de regalos. Revisa la conexión con la base de datos.", e);
        }
    }

    /** Borra el regalo junto con sus reservas canceladas (historial), en una sola transacción. */
    public void eliminarRegalo(int idRegalo) {
        try (Connection connection = conectar()) {
            connection.setAutoCommit(false);
            try (PreparedStatement reservas = connection.prepareStatement(
                         "DELETE FROM Reserva_regalo WHERE id_regalo = ? AND UPPER(estado) <> '"
                                 + ReservaRegalo.ESTADO_RESERVADO + "'");
                 PreparedStatement regalo = connection.prepareStatement("DELETE FROM Regalo WHERE id_regalo = ?")) {
                reservas.setInt(1, idRegalo);
                reservas.executeUpdate();
                regalo.setInt(1, idRegalo);
                regalo.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo quitar el regalo. Verifica que no tenga reservas activas.", e);
        }
    }

    private Regalo convertir(ResultSet result) throws SQLException {
        Regalo regalo = new Regalo(result.getInt("id_regalo"), result.getInt("id_invitacion"), result.getString("nombre"),
                result.getString("descripcion"), result.getString("estado"), result.getBigDecimal("precio_estimado"));
        int idInvitado = result.getInt("id_invitado_reserva");
        if (!result.wasNull()) {
            regalo.setIdInvitadoReserva(idInvitado);
            regalo.setReservadoPor(result.getString("reservado_por"));
            regalo.setEstado(Regalo.ESTADO_RESERVADO);
        } else {
            regalo.setEstado(Regalo.ESTADO_DISPONIBLE);
        }
        return regalo;
    }
}
