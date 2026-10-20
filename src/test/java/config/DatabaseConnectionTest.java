package config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatabaseConnectionTest {

    private static final String MIGRACION = "010_organizador_usuario_unico.sql";

    private String url;
    private String homeAnterior;

    @BeforeEach
    void preparar() {
        url = "jdbc:h2:mem:esquema_version_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        homeAnterior = System.getProperty("nexteventux.home");
        DatabaseConnection.reiniciarInicializacion();
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        } catch (SQLException ignorada) {
            // La base de la prueba que falla al abrir ya se cerró.
        }
        if (homeAnterior == null) {
            System.clearProperty("nexteventux.home");
        } else {
            System.setProperty("nexteventux.home", homeAnterior);
        }
        DatabaseConnection.reiniciarInicializacion();
    }

    @Test
    void laRutaPorDefectoEsAbsolutaYNoDependeDelDirectorioActual() {
        System.clearProperty("nexteventux.home");

        String ruta = DatabaseConnection.urlPorDefecto();

        assertTrue(ruta.startsWith("jdbc:h2:"));
        assertTrue(ruta.endsWith(";DB_CLOSE_DELAY=-1"));
        String archivo = ruta.substring("jdbc:h2:".length(), ruta.indexOf(';'));
        assertTrue(Path.of(archivo).isAbsolute(), "La ruta debe ser absoluta: " + archivo);
        assertTrue(archivo.contains(".nexteventux"));
    }

    @Test
    void laCarpetaSePuedeCambiarConNexteventuxHome(@org.junit.jupiter.api.io.TempDir Path carpeta) {
        System.setProperty("nexteventux.home", carpeta.toString());

        String archivo = DatabaseConnection.urlPorDefecto();

        assertTrue(archivo.contains(carpeta.toAbsolutePath().toString().replace('\\', '/')));
    }

    @Test
    void laMigracionNuevaSeRegistraUnaSolaVez() throws Exception {
        DatabaseConnection.getConnection(url).close();
        DatabaseConnection.reiniciarInicializacion();
        DatabaseConnection.getConnection(url).close();

        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM Esquema_version WHERE nombre = '" + MIGRACION + "'")) {
            result.next();
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void unUsuarioNoPuedeTenerDosOrganizadores() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            insertarUsuario(statement, 1);
            statement.executeUpdate(
                    "INSERT INTO Organizador (id_organizador, id_usuario, estado) VALUES (1, 1, 'ACTIVO')");

            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO Organizador (id_organizador, id_usuario, estado) VALUES (2, 1, 'ACTIVO')"));
        }
    }

    @Test
    void unaBaseConOrganizadoresRepetidosSeDetieneSinBorrarDatos() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            insertarUsuario(statement, 1);
            // Simula una base anterior a la migración: sin índice ni registro.
            statement.execute("DROP INDEX uq_organizador_usuario");
            statement.execute("DELETE FROM Esquema_version WHERE nombre = '" + MIGRACION + "'");
            statement.executeUpdate(
                    "INSERT INTO Organizador (id_organizador, id_usuario, estado) VALUES (1, 1, 'ACTIVO')");
            statement.executeUpdate(
                    "INSERT INTO Organizador (id_organizador, id_usuario, estado) VALUES (2, 1, 'ACTIVO')");
        }
        DatabaseConnection.reiniciarInicializacion();

        SQLException error = assertThrows(SQLException.class, () -> DatabaseConnection.getConnection(url));
        assertTrue(error.getMessage().contains("Organizador.id_usuario"), error.getMessage());

        // La migración no quedó registrada y los datos se conservaron.
        DatabaseConnection.reiniciarInicializacion();
        try (Connection connection = openSinInicializar();
                Statement statement = connection.createStatement()) {
            try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM Organizador")) {
                result.next();
                assertEquals(2, result.getInt(1));
            }
            try (ResultSet result = statement.executeQuery(
                    "SELECT COUNT(*) FROM Esquema_version WHERE nombre = '" + MIGRACION + "'")) {
                result.next();
                assertEquals(0, result.getInt(1));
            }
            statement.executeUpdate("DELETE FROM Organizador WHERE id_organizador = 2");
        }

        // Corregido el dato, la aplicación abre y la migración se aplica.
        DatabaseConnection.reiniciarInicializacion();
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COUNT(*) FROM Esquema_version WHERE nombre = '" + MIGRACION + "'")) {
            result.next();
            assertEquals(1, result.getInt(1));
            assertFalse(result.next());
        }
    }

    @Test
    void unaBaseConUsuariosRepetidosSinDistinguirMayusculasSeDetiene() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
                Statement statement = connection.createStatement()) {
            insertarUsuario(statement, 1);
            statement.execute("DROP INDEX uq_usuario_usuario");
            statement.execute("DELETE FROM Esquema_version WHERE nombre = '011_usuario_unico.sql'");
            statement.executeUpdate("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                    + "fecha_registro, acepta_marketing, tipo_usuario) VALUES (2, 'Repetido', 'PRUEBA1', "
                    + "'x', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE')");
        }
        DatabaseConnection.reiniciarInicializacion();

        SQLException error = assertThrows(SQLException.class, () -> DatabaseConnection.getConnection(url));
        assertTrue(error.getMessage().contains("Usuario.LOWER(usuario)"), error.getMessage());

        DatabaseConnection.reiniciarInicializacion();
        try (Connection connection = openSinInicializar();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM Usuario")) {
            result.next();
            assertEquals(2, result.getInt(1));
        }
    }

    private Connection openSinInicializar() throws SQLException {
        return java.sql.DriverManager.getConnection(url, "sa", "");
    }

    private static void insertarUsuario(Statement statement, int id) throws SQLException {
        statement.executeUpdate("INSERT INTO Usuario (id_usuario, nombre, usuario, contraseña, estado, "
                + "fecha_registro, acepta_marketing, tipo_usuario) VALUES (" + id
                + ", 'Prueba', 'prueba" + id + "', 'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')");
    }
}
