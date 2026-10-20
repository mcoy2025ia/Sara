package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import config.DatabaseConnection;
import entities.DisponibilidadServicio;

// Se encarga de guardar y consultar disponibilidades en H2
public class RepositoryDisponibilidadServicio {

    public void guardarDisponibilidad(
            DisponibilidadServicio disponibilidad) {

        String[] horas = separarHorario(disponibilidad.getHorario());

        String sql = """
                INSERT INTO Disponibilidad_Servicio
                (id_servicio, fecha, hora_inicio, hora_fin, estado)
                VALUES (?, ?, ?, ?, 'DISPONIBLE')
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(
                sql,
                java.sql.Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, disponibilidad.getIdServicio());
            statement.setDate(
                    2,
                    new java.sql.Date(
                            disponibilidad.getFecha().getTime()));
            statement.setTime(
                    3,
                    java.sql.Time.valueOf(
                            LocalTime.parse(horas[0])));
            statement.setTime(
                    4,
                    java.sql.Time.valueOf(
                            LocalTime.parse(horas[1])));

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    disponibilidad.setIdDisponibilidad(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo guardar la disponibilidad.",
                    e);
        }
    }

    public DisponibilidadServicio buscarDisponibilidad(
            int idServicio,
            Date fecha,
            String horario) {

        String[] horas = separarHorario(horario);

        String sql = """
                SELECT *
                FROM Disponibilidad_Servicio
                WHERE id_servicio = ?
                  AND fecha = ?
                  AND hora_inicio = ?
                  AND hora_fin = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);
            statement.setDate(
                    2,
                    new java.sql.Date(fecha.getTime()));
            statement.setTime(
                    3,
                    java.sql.Time.valueOf(LocalTime.parse(horas[0])));
            statement.setTime(
                    4,
                    java.sql.Time.valueOf(LocalTime.parse(horas[1])));

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return construirDisponibilidad(result);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo consultar la disponibilidad.",
                    e);
        }
    }

    public List<DisponibilidadServicio> listarDisponibilidadesServicio(
            int idServicio) {

        List<DisponibilidadServicio> resultado = new ArrayList<>();

        String sql = """
                SELECT *
                FROM Disponibilidad_Servicio
                WHERE id_servicio = ?
                ORDER BY fecha, hora_inicio
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    resultado.add(
                            construirDisponibilidad(result));
                }
            }

            return resultado;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las disponibilidades.",
                    e);
        }
    }

    public int contarDisponibilidades(int idServicio) {

        String sql = """
                SELECT COUNT(*)
                FROM Disponibilidad_Servicio
                WHERE id_servicio = ?
                  AND estado = 'DISPONIBLE'
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron contar las disponibilidades.",
                    e);
        }
    }

    public boolean existeCruceDisponibilidad(
            int idServicio,
            Date fecha,
            String horario) {

        String[] horas = separarHorario(horario);

        String sql = """
                SELECT COUNT(*)
                FROM Disponibilidad_Servicio
                WHERE id_servicio = ?
                  AND fecha = ?
                  AND estado = 'DISPONIBLE'
                  AND ? < hora_fin
                  AND ? > hora_inicio
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);
            statement.setDate(
                    2,
                    new java.sql.Date(fecha.getTime()));
            statement.setTime(
                    3,
                    java.sql.Time.valueOf(LocalTime.parse(horas[0])));
            statement.setTime(
                    4,
                    java.sql.Time.valueOf(LocalTime.parse(horas[1])));

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo validar el cruce de disponibilidad.",
                    e);
        }
    }

    public boolean existeCruceContratacion(
            int idServicio,
            Date fecha,
            String horario) {

        String[] horas = separarHorario(horario);

        String sql = """
                SELECT COUNT(*)
                FROM Contratacion c
                JOIN Propuesta p
                  ON p.id_propuesta = c.id_propuesta
                JOIN Solicitud_Ejecucion s
                  ON s.id_solicitud = p.id_solicitud
                WHERE s.id_servicio = ?
                  AND UPPER(c.estado) IN ('ACTIVA', 'CONFIRMADA')
                  AND c.fecha_servicio = ?
                  AND ? < c.hora_fin
                  AND ? > c.hora_inicio
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);
            statement.setDate(
                    2,
                    new java.sql.Date(fecha.getTime()));
            statement.setTime(
                    3,
                    java.sql.Time.valueOf(LocalTime.parse(horas[0])));
            statement.setTime(
                    4,
                    java.sql.Time.valueOf(LocalTime.parse(horas[1])));

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo validar el horario contratado.",
                    e);
        }
    }

    public void eliminarDisponibilidad(
            int idServicio,
            int idDisponibilidad) {

        String sql = """
                DELETE FROM Disponibilidad_Servicio
                WHERE id_disponibilidad = ?
                  AND id_servicio = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idDisponibilidad);
            statement.setInt(2, idServicio);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo eliminar la disponibilidad.",
                    e);
        }
    }

    private DisponibilidadServicio construirDisponibilidad(
            ResultSet result) throws SQLException {

        DisponibilidadServicio disponibilidad
                = new DisponibilidadServicio();

        disponibilidad.setIdDisponibilidad(
                result.getInt("id_disponibilidad"));
        disponibilidad.setIdServicio(
                result.getInt("id_servicio"));
        disponibilidad.setFecha(
                new Date(result.getDate("fecha").getTime()));

        String horario
                = result.getTime("hora_inicio")
                        .toLocalTime()
                        .toString()
                + " - "
                + result.getTime("hora_fin")
                        .toLocalTime()
                        .toString();

        disponibilidad.setHorario(horario);

        return disponibilidad;
    }

    private String[] separarHorario(String horario) {

        if (horario == null) {
            throw new IllegalArgumentException(
                    "El horario es obligatorio.");
        }

        String[] partes = horario.trim().split("\\s*-\\s*");

        if (partes.length != 2) {
            throw new IllegalArgumentException(
                    "El horario debe tener el formato HH:mm - HH:mm.");
        }

        LocalTime inicio = LocalTime.parse(partes[0]);
        LocalTime fin = LocalTime.parse(partes[1]);

        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException(
                    "La hora final debe ser posterior a la inicial.");
        }

        return new String[]{
            inicio.toString(),
            fin.toString()
        };
    }
}
