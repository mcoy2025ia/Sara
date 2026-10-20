import config.DatabaseConnection;
import entities.Evento;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import org.h2.tools.RunScript;

/** Verifica el contrato de datos de CU01; no sustituye al futuro repositorio JDBC. */
public class PruebaEventoSchema {
    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:cu01", "sa", "")) {
            DatabaseConnection.inicializarEsquema(connection);
            try (InputStreamReader reader = new InputStreamReader(
                    PruebaEventoSchema.class.getResourceAsStream("/seed.sql"), StandardCharsets.UTF_8)) {
                RunScript.execute(connection, reader);
            }
            Evento evento = new Evento(1, "Festival", "Corporativo", "Descripción",
                    LocalDateTime.of(2026, 10, 20, 9, 0), LocalDateTime.of(2026, 10, 20, 18, 0),
                    "Bogotá", new BigDecimal("15000000.25"), 100);
            String sql = "INSERT INTO Evento (id_organizador, nombre, tipo, descripcion, fecha_inicio, "
                    + "fecha_fin, ubicacion, presupuesto, invitados_iniciales, estado, fecha_creacion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, evento.getIdOrganizador());
                statement.setString(2, evento.getNombre());
                statement.setString(3, evento.getTipo());
                statement.setString(4, evento.getDescripcion());
                statement.setTimestamp(5, Timestamp.valueOf(evento.getFechaInicio()));
                statement.setTimestamp(6, Timestamp.valueOf(evento.getFechaFin()));
                statement.setString(7, evento.getUbicacion());
                statement.setBigDecimal(8, evento.getPresupuesto());
                statement.setInt(9, evento.getInvitadosIniciales());
                statement.setString(10, evento.getEstado());
                statement.setTimestamp(11, Timestamp.valueOf(evento.getFechaCreacion()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new AssertionError("No se generó el identificador.");
                    evento.setIdEvento(keys.getInt(1));
                }
            }
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM Evento WHERE id_evento = ?")) {
                statement.setInt(1, evento.getIdEvento());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()
                            || result.getInt("id_organizador") != evento.getIdOrganizador()
                            || !evento.getNombre().equals(result.getString("nombre"))
                            || !evento.getTipo().equals(result.getString("tipo"))
                            || !evento.getDescripcion().equals(result.getString("descripcion"))
                            || !evento.getFechaInicio().equals(result.getTimestamp("fecha_inicio").toLocalDateTime())
                            || !evento.getFechaFin().equals(result.getTimestamp("fecha_fin").toLocalDateTime())
                            || !evento.getUbicacion().equals(result.getString("ubicacion"))
                            || evento.getPresupuesto().compareTo(result.getBigDecimal("presupuesto")) != 0
                            || evento.getInvitadosIniciales() != result.getInt("invitados_iniciales")
                            || !evento.getEstado().equals(result.getString("estado"))
                            || result.getTimestamp("fecha_creacion") == null) {
                        throw new AssertionError("El esquema no conservó los datos de CU01.");
                    }
                }
            }
            System.out.println("Entidad Evento compatible con H2; todos sus datos fueron recuperados.");
        }
    }
}
