package repositories;

import config.DatabaseConnection;
import entities.PreguntaServicio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RepositoryPreguntaServicio {

    public void guardarPregunta(
            PreguntaServicio pregunta) {

        String sql = """
                INSERT INTO Pregunta_Servicio
                (id_servicio, id_organizador, pregunta,
                 fecha_pregunta, estado)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(
                    1,
                    pregunta.getIdServicio());

            statement.setInt(
                    2,
                    pregunta.getIdOrganizador());

            statement.setString(
                    3,
                    pregunta.getPregunta());

            statement.setTimestamp(
                    4,
                    Timestamp.valueOf(
                            pregunta.getFechaPregunta()));

            statement.setString(
                    5,
                    pregunta.getEstado());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    pregunta.setIdPregunta(
                            keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo guardar la pregunta.",
                    e);
        }
    }

    public PreguntaServicio buscarPregunta(
            int idPregunta) {

        String sql = """
                SELECT *
                FROM Pregunta_Servicio
                WHERE id_pregunta = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idPregunta);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return construirPregunta(result);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo consultar la pregunta.",
                    e);
        }
    }

    public List<PreguntaServicio> listarPreguntasServicio(
            int idServicio) {

        List<PreguntaServicio> preguntas =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM Pregunta_Servicio
                WHERE id_servicio = ?
                ORDER BY fecha_pregunta, id_pregunta
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    preguntas.add(
                            construirPregunta(result));
                }
            }

            return preguntas;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las preguntas.",
                    e);
        }
    }

    public void responderPregunta(
            int idPregunta,
            String respuesta) {

        String sql = """
                UPDATE Pregunta_Servicio
                SET respuesta = ?,
                    fecha_respuesta = CURRENT_TIMESTAMP,
                    estado = 'RESPONDIDA'
                WHERE id_pregunta = ?
                  AND estado = 'PENDIENTE'
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, respuesta);
            statement.setInt(2, idPregunta);

            int actualizadas =
                    statement.executeUpdate();

            if (actualizadas == 0) {
                throw new IllegalStateException(
                        "La pregunta ya fue respondida o no existe.");
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo responder la pregunta.",
                    e);
        }
    }

    private PreguntaServicio construirPregunta(
            ResultSet result) throws SQLException {

        PreguntaServicio pregunta =
                new PreguntaServicio();

        pregunta.setIdPregunta(
                result.getInt("id_pregunta"));

        pregunta.setIdServicio(
                result.getInt("id_servicio"));

        pregunta.setIdOrganizador(
                result.getInt("id_organizador"));

        pregunta.setPregunta(
                result.getString("pregunta"));

        pregunta.setRespuesta(
                result.getString("respuesta"));

        Timestamp fechaPregunta =
                result.getTimestamp("fecha_pregunta");

        if (fechaPregunta != null) {
            pregunta.setFechaPregunta(
                    fechaPregunta.toLocalDateTime());
        }

        Timestamp fechaRespuesta =
                result.getTimestamp("fecha_respuesta");

        if (fechaRespuesta != null) {
            pregunta.setFechaRespuesta(
                    fechaRespuesta.toLocalDateTime());
        }

        pregunta.setEstado(
                result.getString("estado"));

        return pregunta;
    }
}
