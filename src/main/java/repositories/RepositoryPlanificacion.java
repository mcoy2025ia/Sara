package repositories;

import config.DatabaseConnection;
import entities.Planificacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RepositoryPlanificacion {

    /**
     * Busca la planificación vigente de un evento: la versión ACTIVA más reciente.
     * Si el evento aún no tiene una versión ACTIVA, devuelve la más reciente (por ejemplo
     * un BORRADOR); la pantalla muestra su estado para que el organizador lo distinga.
     */
    public Planificacion buscarPlanificacionPorEvento(int idEvento) {
        String sql = "SELECT * FROM Planificacion WHERE id_evento = ? "
                + "ORDER BY CASE WHEN UPPER(estado) = 'ACTIVA' THEN 0 ELSE 1 END, version DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearPlanificacion(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo acceder a la planificación del evento.", e);
        }
        return null;
    }

    /**
     * Busca una planificación específica por su ID.
     */
    public Planificacion buscarPlanificacion(int idPlanificacion) {
        String sql = "SELECT * FROM Planificacion WHERE id_planificacion = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idPlanificacion);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearPlanificacion(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo acceder a la planificación del evento.", e);
        }
        return null;
    }

    /**
     * Lista todas las versiones de planificación asociadas a un evento.
     */
    public List<Planificacion> listarVersionesPlanificacion(int idEvento) {
        List<Planificacion> versiones = new ArrayList<>();
        String sql = "SELECT * FROM Planificacion WHERE id_evento = ? ORDER BY version ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    versiones.add(mapearPlanificacion(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo acceder a la planificación del evento.", e);
        }
        return versiones;
    }

    /**
     * Guarda o inserta una nueva planificación en la base de datos.
     */
    public void guardarPlanificacion(Planificacion planificacion) {
        String sql = "INSERT INTO Planificacion (id_evento, version, descripcion, estado) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, planificacion.getIdEvento());
            pstmt.setInt(2, planificacion.getVersion());
            pstmt.setString(3, planificacion.getDescripcion());
            pstmt.setString(4, planificacion.getEstado());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo acceder a la planificación del evento.", e);
        }
    }

    public void guardarVersionPlanificacion(Planificacion planificacion) {
        guardarPlanificacion(planificacion);
    }

    /**
     * Método auxiliar para convertir una fila del ResultSet a un objeto Planificacion.
     */
    private Planificacion mapearPlanificacion(ResultSet rs) throws SQLException {
        Planificacion plan = new Planificacion();
        plan.setIdPlanificacion(rs.getInt("id_planificacion"));
        plan.setIdEvento(rs.getInt("id_evento"));
        plan.setVersion(rs.getInt("version"));
        plan.setDescripcion(rs.getString("descripcion"));
        plan.setEstado(rs.getString("estado"));
        return plan;
    }
}