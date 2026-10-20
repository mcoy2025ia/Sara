import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

class BaseDatosPrueba {
    static void preparar(String url) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                    + "fecha_registro, acepta_marketing, tipo_usuario) VALUES "
                    + "(1, 'Organizador de prueba', 'prueba', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')");
            statement.executeUpdate("INSERT INTO Organizador (id_organizador, id_usuario, estado) VALUES (1, 1, 'ACTIVO')");
        }
    }
}
