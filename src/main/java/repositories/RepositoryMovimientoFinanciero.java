package repositories;

import config.DatabaseConnection;
import entities.CompromisoFinanciero;
import entities.MovimientoFinanciero;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RepositoryMovimientoFinanciero {

    /**
     * CU06: contrataciones que comprometen el presupuesto de un evento.
     * - Cuentan todas excepto las CANCELADA (una FINALIZADA ya se pagó).
     * - La solicitud de la propuesta debe ser del mismo evento que la contratación,
     *   para que un registro mal relacionado no afecte el presupuesto de otro evento.
     * - El valor es el acordado en la contratación; si no existe, el de la propuesta aceptada.
     *   No se usa el precio de catálogo ni se asume $0: si ninguno existe, valor queda null.
     */
    public List<CompromisoFinanciero> listarCompromisosPorEvento(int idEvento) {
        List<CompromisoFinanciero> compromisos = new ArrayList<>();
        String sql = """
                SELECT c.id_contratacion,
                       COALESCE(s.nombre, pr.descripcion) AS nombre,
                       COALESCE(s.categoria, 'Otros') AS categoria,
                       COALESCE(c.valor_acordado, pr.valor) AS valor,
                       c.estado
                FROM Contratacion c
                JOIN Propuesta pr ON pr.id_propuesta = c.id_propuesta
                JOIN Solicitud_Ejecucion se ON se.id_solicitud = pr.id_solicitud
                                           AND se.id_evento = c.id_evento
                LEFT JOIN Servicio s ON s.id_servicio = se.id_servicio
                WHERE c.id_evento = ?
                  AND UPPER(COALESCE(c.estado, '')) <> 'CANCELADA'
                ORDER BY c.id_contratacion
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal valor = rs.getBigDecimal("valor");
                    compromisos.add(new CompromisoFinanciero(
                            rs.getInt("id_contratacion"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            valor,
                            rs.getString("estado")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron consultar las contrataciones del evento.", e);
        }

        return compromisos;
    }

    /**
     * Consulta todos los movimientos financieros asociados a un evento desde la base de datos H2.
     */
    public List<MovimientoFinanciero> listarMovimientosPorEvento(int idEvento) {
        List<MovimientoFinanciero> lista = new ArrayList<>();
        String sql = "SELECT * FROM Movimiento_financiero WHERE id_evento = ? ORDER BY fecha DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEvento);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMovimiento(rs));
                }
            }
        } catch (SQLException e) {
            // Se propaga el error para que la vista lo muestre en vez de pintar una lista vacía.
            throw new RuntimeException("No se pudieron consultar los movimientos financieros.", e);
        }

        return lista;
    }

    // Busca un movimiento financiero específico por su ID.
    public MovimientoFinanciero buscarMovimiento(int idMovimiento) {
        String sql = "SELECT * FROM Movimiento_financiero WHERE id_movimiento = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idMovimiento);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearMovimiento(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo consultar el movimiento financiero.", e);
        }

        return null;
    }

    /**
     * Método auxiliar para convertir una fila del ResultSet a un objeto MovimientoFinanciero.
     */
    private MovimientoFinanciero mapearMovimiento(ResultSet rs) throws SQLException {
        MovimientoFinanciero mov = new MovimientoFinanciero();
        mov.setIdMovimiento(rs.getInt("id_movimiento"));
        mov.setIdPropuesta(rs.getInt("id_propuesta"));
        mov.setIdEvento(rs.getInt("id_evento"));
        mov.setIdContratacion(rs.getInt("id_contratacion"));
        mov.setValor(rs.getBigDecimal("valor"));
        Timestamp fecha = rs.getTimestamp("fecha");
        mov.setFecha(fecha != null ? fecha.toLocalDateTime() : null);
        mov.setDescripcion(rs.getString("descripcion"));
        mov.setTipo(rs.getString("tipo"));
        mov.setEstado(rs.getString("estado"));
        return mov;
    }
}