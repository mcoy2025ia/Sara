package repositories;

import config.DatabaseConnection;
import entities.ImagenServicio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RepositoryImagenServicio {

    public void guardarImagen(ImagenServicio imagen) {

        String sql = """
                INSERT INTO Imagen_Servicio
                (id_servicio, ruta, orden_imagen)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, imagen.getIdServicio());
            statement.setString(2, imagen.getRuta());
            statement.setInt(3, imagen.getOrdenImagen());

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    imagen.setIdImagen(keys.getInt(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo guardar la imagen del servicio.",
                    e);
        }
    }

    public ImagenServicio buscarImagen(int idImagen) {

        String sql = """
                SELECT id_imagen, id_servicio, ruta, orden_imagen
                FROM Imagen_Servicio
                WHERE id_imagen = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idImagen);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return construirImagen(result);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo consultar la imagen.",
                    e);
        }
    }

    public List<ImagenServicio> listarImagenesServicio(
            int idServicio) {

        List<ImagenServicio> imagenes =
                new ArrayList<>();

        String sql = """
                SELECT id_imagen, id_servicio, ruta, orden_imagen
                FROM Imagen_Servicio
                WHERE id_servicio = ?
                ORDER BY orden_imagen, id_imagen
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idServicio);

            try (ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    imagenes.add(
                            construirImagen(result));
                }
            }

            return imagenes;

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudieron consultar las imagenes.",
                    e);
        }
    }

    public void eliminarImagen(int idImagen) {

        String sql = """
                DELETE FROM Imagen_Servicio
                WHERE id_imagen = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, idImagen);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                    "No se pudo eliminar la imagen.",
                    e);
        }
    }

    private ImagenServicio construirImagen(
            ResultSet result) throws SQLException {

        ImagenServicio imagen =
                new ImagenServicio();

        imagen.setIdImagen(
                result.getInt("id_imagen"));

        imagen.setIdServicio(
                result.getInt("id_servicio"));

        imagen.setRuta(
                result.getString("ruta"));

        imagen.setOrdenImagen(
                result.getInt("orden_imagen"));

        return imagen;
    }
}
