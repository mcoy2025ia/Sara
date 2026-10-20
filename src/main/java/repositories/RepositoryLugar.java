package repositories;

import config.DatabaseConnection;
import entities.Lugar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Consulta de lugares y su aforo. Solo lectura: la gestión de lugares no es parte del CU-17. */
public class RepositoryLugar {
    private final String url;

    public RepositoryLugar() {
        this(null);
    }

    // Permite utilizar una base aislada en las pruebas.
    public RepositoryLugar(String url) {
        this.url = url;
    }

    private Connection conectar() throws SQLException {
        return url == null ? DatabaseConnection.getConnection() : DatabaseConnection.getConnection(url);
    }

    public List<Lugar> listarLugares() {
        List<Lugar> lugares = new ArrayList<>();
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM Lugar ORDER BY nombre");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) lugares.add(convertir(result));
            return lugares;
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar los lugares. Revisa la conexión con la base de datos.", e);
        }
    }

    public Lugar buscarLugar(int idLugar) {
        try (Connection connection = conectar();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM Lugar WHERE id_lugar = ?")) {
            statement.setInt(1, idLugar);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? convertir(result) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el lugar. Revisa la conexión con la base de datos.", e);
        }
    }

    private Lugar convertir(ResultSet result) throws SQLException {
        return new Lugar(result.getInt("id_lugar"), result.getString("nombre"), result.getString("direccion"),
                result.getInt("capacidad"), result.getString("descripcion"), result.getString("estado"));
    }
}
