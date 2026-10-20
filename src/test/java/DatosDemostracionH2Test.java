
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;
import services.ServiceAutenticacion;

public class DatosDemostracionH2Test {

    private static final String TEST_URL
            = "jdbc:h2:mem:datos_demostracion;DB_CLOSE_DELAY=-1";

    @Test
    void usuariosDeDemostracionDebenPoderIngresar() throws Exception {

        System.setProperty("nexteventux.db.url", TEST_URL);

        try {
            try (Connection connection
                    = DatabaseConnection.getConnection(TEST_URL)) {

                assertTrue(
                        connection.getMetaData()
                                .getURL()
                                .startsWith("jdbc:h2:mem:datos_demostracion"));

                CargarDatosEjemplo.cargar(connection);
            }

            ServiceAutenticacion service
                    = new ServiceAutenticacion(
                            new RepositoryAutenticacion());

            comprobarUsuario(service, "valentina", "ORGANIZADOR");
            comprobarUsuario(service, "santiago", "ORGANIZADOR");
            comprobarUsuario(service, "juliana", "ORGANIZADOR");

            comprobarUsuario(service, "luna", "PROVEEDOR");
            comprobarUsuario(service, "sabor", "PROVEEDOR");
            comprobarUsuario(service, "flash", "PROVEEDOR");

            comprobarUsuario(service, "camila", "ADMINISTRADOR");
            comprobarUsuario(service, "mateo", "ADMINISTRADOR");
            comprobarUsuario(service, "shirly", "ADMINISTRADOR");

            try (Connection connection
                    = DatabaseConnection.getConnection(TEST_URL); Statement statement
                    = connection.createStatement(); ResultSet result
                    = statement.executeQuery("""
                                 SELECT COUNT(*)
                                 FROM Invitado
                                 WHERE id_invitado IN (5, 6, 7)
                                 """)) {

                assertTrue(result.next());
                assertEquals(3, result.getInt(1));
            }

            try (Connection connection
                    = DatabaseConnection.getConnection(TEST_URL)) {

                // seed.sql también agrega clientes de marketing (CU-21) con IDs automáticos;
                // lo importante es que el siguiente ID no choque con los existentes.
                comprobarContador(connection, "USUARIO", "ID_USUARIO",
                        siguienteDisponible(connection, "Usuario", "id_usuario"));
                comprobarContador(connection, "USUARIO_ROL", "ID_USUARIO_ROL",
                        siguienteDisponible(connection, "Usuario_rol", "id_usuario_rol"));
                comprobarContador(connection, "ORGANIZADOR", "ID_ORGANIZADOR", 6);
                comprobarContador(connection, "PROVEEDOR", "ID_PROVEEDOR", 6);
                comprobarContador(connection, "INVITADO", "ID_INVITADO", 8);

                String sql = """
                        INSERT INTO Usuario
                        (nombre, usuario, contraseña, estado,
                         fecha_registro, acepta_marketing, tipo_usuario)
                        VALUES
                        ('Usuario nuevo de prueba', 'nuevo.prueba',
                         '1234', 'ACTIVO', CURRENT_TIMESTAMP,
                         FALSE, 'ORGANIZADOR')
                        """;

                long idEsperado = siguienteDisponible(connection, "Usuario", "id_usuario");

                try (PreparedStatement statement
                        = connection.prepareStatement(
                                sql,
                                Statement.RETURN_GENERATED_KEYS)) {

                    assertEquals(1, statement.executeUpdate());

                    try (ResultSet keys
                            = statement.getGeneratedKeys()) {

                        assertTrue(keys.next());
                        assertEquals(idEsperado, keys.getLong(1));
                    }
                }
            }

        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    private void comprobarUsuario(
            ServiceAutenticacion service,
            String usuario,
            String tipoEsperado) {

        SesionUsuario sesion
                = service.iniciarSesion(usuario, "1234");

        assertNotNull(sesion);
        assertEquals(tipoEsperado, sesion.getTipoUsuario());
    }

    private void comprobarContador(
            Connection connection,
            String tabla,
            String columna,
            long siguienteId) throws Exception {

        String sql = """
                SELECT IDENTITY_BASE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement statement
                = connection.prepareStatement(sql)) {

            statement.setString(1, tabla);
            statement.setString(2, columna);

            try (ResultSet result = statement.executeQuery()) {

                assertTrue(result.next());
                assertEquals(siguienteId, result.getLong(1));
            }
        }
    }

    private static long siguienteDisponible(Connection connection, String tabla, String columna)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COALESCE(MAX(" + columna + "), 0) + 1 FROM " + tabla)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }
}
