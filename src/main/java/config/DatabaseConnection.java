package config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.h2.tools.RunScript;

public class DatabaseConnection {

    private static final String USER = "sa";
    private static final String PASSWORD = "";

    /**
     * Ruta de la base cuando no se indica nexteventux.db.url. Es absoluta para que
     * no dependa del directorio desde el que se lance la aplicación: una ruta
     * relativa creaba otra base vacía al ejecutar desde otra carpeta.
     * La carpeta se puede cambiar con -Dnexteventux.home=...
     */
    static String urlPorDefecto() {
        String carpeta = System.getProperty("nexteventux.home");
        Path base = carpeta == null || carpeta.isBlank()
                ? Path.of(System.getProperty("user.home"), ".nexteventux")
                : Path.of(carpeta);
        String archivo = base.toAbsolutePath().resolve("nexteventux").toString().replace('\\', '/');
        // DB_CLOSE_DELAY=-1 mantiene la base abierta mientras la aplicación se ejecuta:
        // abrir el archivo H2 en cada consulta costaba ~185 ms (RNF-07 y RNF-08).
        return "jdbc:h2:" + archivo + ";DB_CLOSE_DELAY=-1";
    }

    public static Connection getConnection() throws SQLException {
        return getConnection(
                System.getProperty("nexteventux.db.url", urlPorDefecto()));
    }

    public static Connection getConnection(String url) throws SQLException {
        Connection connection = DriverManager.getConnection(url, USER, PASSWORD);
        try {
            inicializarSiHaceFalta(url, connection);
            return connection;
        } catch (SQLException | RuntimeException e) {
            try {
                connection.close();
            } catch (SQLException cierre) {
                e.addSuppressed(cierre);
            }
            throw e;
        }
    }

    // Bases ya inicializadas durante esta ejecución. Volver a correr todos los
    // scripts en cada conexión costaba ~200 ms por consulta (RNF-07 y RNF-08).
    private static final java.util.Set<String> INICIALIZADAS =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Olvida qué bases ya se inicializaron, como ocurre al reiniciar la aplicación.
     * Lo usan las pruebas que verifican migraciones sobre bases existentes.
     */
    public static void reiniciarInicializacion() {
        INICIALIZADAS.clear();
    }

    private static synchronized void inicializarSiHaceFalta(String url, Connection connection)
            throws SQLException {
        // La tabla centinela detecta bases recreadas con la misma URL (por ejemplo, en memoria).
        if (INICIALIZADAS.contains(url) && existeTabla(connection, "ESQUEMA_INICIALIZADO")) {
            return;
        }
        inicializarEsquema(connection);
        try (java.sql.Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS Esquema_inicializado (id INT PRIMARY KEY)");
        }
        INICIALIZADAS.add(url);
    }

    public static synchronized void inicializarEsquema(Connection connection) throws SQLException {
        ejecutarScript(connection, "/schema.sql");
        try (java.sql.Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS Esquema_version ("
                    + "nombre VARCHAR(150) PRIMARY KEY, "
                    + "aplicada TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL)");
        }
        // Migraciones 001 a 009: repetibles, se ejecutan en cada inicialización.
        ejecutarScript(connection, "/migrations/001_evento_cu01.sql");
        ejecutarScript(connection, "/migrations/002_proveedores_cu11.sql");
        ejecutarScript(connection, "/migrations/003_proveedores_cu11_publicacion.sql");
        ejecutarScript(connection, "/migrations/002_publicacion_eventos.sql");
        ejecutarScript(connection, "/migrations/003_cancelacion_eventos.sql");
        ejecutarScript(connection, "/migrations/004_proveedores_cu12.sql");
        ejecutarScript(connection, "/migrations/005_proveedor_cu28.sql");
        ejecutarScript(connection, "/migrations/006_cu28_documentos_notificaciones.sql");
        ejecutarScript(connection, "/migrations/007_destinatario_notificacion_cu28.sql");
        ejecutarScript(connection, "/migrations/008_finalizacion_eventos.sql");
        ejecutarScript(connection, "/migrations/009_solicitudes_evento.sql");
        ejecutarScript(connection, "/migrations/002_marketing_cu21.sql");
        ejecutarScript(connection, "/migrations/004_cu26_contratar_organizador.sql");
        // CU-030: su migración inserta la configuración de evaluación; se ejecuta
        // solo mientras el esquema del caso de uso no esté completo.
        if (!esquemaCu30Instalado(connection)) {
            ejecutarScript(connection, "/migrations/004_cu30_verificar_categorizar_organizador.sql");
        }
        // Migraciones 010 en adelante: se ejecutan una sola vez y quedan en Esquema_version.
        // Cada una usa el número reservado en docs/hoja-de-ruta-correcciones.md.
        ejecutarUnaVez(connection, "010_organizador_usuario_unico.sql",
                c -> verificarSinDuplicados(c, "Organizador", "id_usuario"));
        ejecutarUnaVez(connection, "011_usuario_unico.sql",
                c -> verificarSinDuplicados(c, "Usuario", "LOWER(usuario)"));
        actualizarContadorEventos(connection);
    }

    @FunctionalInterface
    private interface Verificacion {
        void ejecutar(Connection connection) throws SQLException;
    }

    /**
     * Ejecuta una migración nueva solo si no está registrada. La verificación previa
     * se corre antes del script, de modo que un dato inválido detiene la apertura
     * con un mensaje claro sin modificar nada y sin registrar la migración.
     */
    private static void ejecutarUnaVez(Connection connection, String archivo, Verificacion previa)
            throws SQLException {
        if (migracionAplicada(connection, archivo)) {
            return;
        }
        if (previa != null) {
            previa.ejecutar(connection);
        }
        ejecutarScript(connection, "/migrations/" + archivo);
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Esquema_version (nombre) VALUES (?)")) {
            statement.setString(1, archivo);
            statement.executeUpdate();
        }
    }

    private static boolean migracionAplicada(Connection connection, String archivo) throws SQLException {
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM Esquema_version WHERE nombre = ?")) {
            statement.setString(1, archivo);
            try (java.sql.ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /**
     * Falla si la columna tiene valores repetidos, para poder exigir unicidad
     * sin borrar ni reasignar datos. Los nombres de tabla y columna son constantes
     * del código, nunca entradas del usuario.
     */
    static void verificarSinDuplicados(Connection connection, String tabla, String columna)
            throws SQLException {
        String sql = "SELECT " + columna + ", COUNT(*) FROM " + tabla
                + " WHERE " + columna + " IS NOT NULL GROUP BY " + columna
                + " HAVING COUNT(*) > 1 ORDER BY " + columna + " LIMIT 5";
        StringBuilder repetidos = new StringBuilder();
        try (java.sql.Statement statement = connection.createStatement();
                java.sql.ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                if (repetidos.length() > 0) {
                    repetidos.append(", ");
                }
                repetidos.append(result.getString(1)).append(" (").append(result.getInt(2)).append(" veces)");
            }
        }
        if (repetidos.length() > 0) {
            throw new SQLException("No se puede exigir un valor único en " + tabla + "." + columna
                    + " porque hay valores repetidos: " + repetidos
                    + ". Corrija esos registros y vuelva a abrir la aplicación.");
        }
    }

    private static boolean esquemaCu30Instalado(Connection connection) throws SQLException {
        return existeTabla(connection, "CONFIGURACION_EVALUACION_ORGANIZADOR")
                && existeTabla(connection, "DOCUMENTO_ORGANIZADOR")
                && existeTabla(connection, "EVALUACION_ORGANIZADOR")
                && existeTabla(connection, "MENSAJE_ORGANIZADOR")
                && existeTabla(connection, "BITACORA_ORGANIZADOR")
                && existeColumna(connection, "ORGANIZADOR", "ESTADO_VERIFICACION")
                && existeColumna(connection, "ORGANIZADOR", "VERSION_DATOS")
                && existeColumna(connection, "USUARIO", "DOCUMENTO_IDENTIDAD");
    }

    private static boolean existeTabla(Connection connection, String tabla) throws SQLException {
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ?")) {
            statement.setString(1, tabla);
            try (java.sql.ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static boolean existeColumna(Connection connection, String tabla, String columna) throws SQLException {
        try (java.sql.PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            statement.setString(1, tabla);
            statement.setString(2, columna);
            try (java.sql.ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    // Los ejemplos antiguos usaban IDs explícitos sin avanzar la secuencia H2.
    private static void actualizarContadorEventos(Connection connection) throws SQLException {
        try (java.sql.Statement statement = connection.createStatement(); java.sql.ResultSet result = statement.executeQuery(
                "SELECT IDENTITY_BASE, (SELECT COALESCE(MAX(id_evento), 0) FROM Evento) "
                + "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' "
                + "AND TABLE_NAME = 'EVENTO' AND COLUMN_NAME = 'ID_EVENTO'")) {
            if (result.next()) {
                long siguiente = result.getLong(1);
                long maximo = result.getLong(2);
                if (siguiente <= maximo) {
                    try (java.sql.Statement alter = connection.createStatement()) {
                        alter.execute("ALTER TABLE Evento ALTER COLUMN id_evento RESTART WITH " + (maximo + 1));
                    }
                }
            }
        }
    }

    private static void ejecutarScript(Connection connection, String recurso) throws SQLException {
        InputStream schema = DatabaseConnection.class.getResourceAsStream(recurso);
        if (schema == null) {
            throw new SQLException("No se encontró el recurso " + recurso);
        }
        try (InputStreamReader reader = new InputStreamReader(schema, StandardCharsets.UTF_8)) {
            RunScript.execute(connection, reader);
        } catch (java.io.IOException e) {
            throw new SQLException("No se pudo leer el esquema de la base de datos.", e);
        }
    }
}
