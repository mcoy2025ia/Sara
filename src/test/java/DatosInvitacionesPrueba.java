import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Statement;

/** Base aislada para las pruebas de CU-16 y CU-17: un organizador, dos lugares y cuatro eventos. */
class DatosInvitacionesPrueba {
    static final int LUGAR_GRANDE = 1;   // aforo 100
    static final int LUGAR_PEQUENO = 2;  // aforo 2
    /** Organizador dueño de todos los eventos de prueba. */
    static final int ORGANIZADOR = 1;
    static final int EVENTO = 1;
    static final int OTRO_EVENTO = 2;
    static final int EVENTO_CANCELADO = 3;
    static final int EVENTO_PASADO = 4;

    static void preparar(String url) throws Exception {
        BaseDatosPrueba.preparar(url);
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Lugar (id_lugar, nombre, direccion, capacidad, estado) VALUES "
                    + "(1, 'Salón Grande', 'Calle 1', 100, 'DISPONIBLE'), "
                    + "(2, 'Sala Pequeña', 'Calle 2', 2, 'DISPONIBLE')");
            statement.executeUpdate("INSERT INTO Evento (id_evento, id_organizador, nombre, fecha_inicio, fecha_fin, "
                    + "ubicacion, estado, fecha_creacion) VALUES "
                    + "(1, 1, 'Boda', TIMESTAMP '2026-11-15 15:00:00', TIMESTAMP '2026-11-15 23:00:00', 'Salón Grande', 'PLANEADO', CURRENT_TIMESTAMP), "
                    + "(2, 1, 'Cumpleaños', TIMESTAMP '2026-12-01 15:00:00', TIMESTAMP '2026-12-01 20:00:00', 'Sala Pequeña', 'PLANEADO', CURRENT_TIMESTAMP), "
                    + "(3, 1, 'Cancelado', TIMESTAMP '2026-12-10 15:00:00', TIMESTAMP '2026-12-10 20:00:00', 'Salón Grande', 'CANCELADO', CURRENT_TIMESTAMP), "
                    + "(4, 1, 'Pasado', TIMESTAMP '2020-01-10 15:00:00', TIMESTAMP '2020-01-10 20:00:00', 'Salón Grande', 'PLANEADO', CURRENT_TIMESTAMP)");
        }
    }

    static void agregarAcompanante(String url, int idInvitado) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Acompanante (id_invitado, nombre, tipo) VALUES ("
                    + idInvitado + ", 'Acompañante', 'PAREJA')");
        }
    }

    /** Inserta directamente una invitación (por ejemplo, para un evento que ya pasó). */
    static int insertarInvitacion(String url, int evento, int lugar) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Invitacion (id_evento, id_lugar, fecha_envio, mensaje, estado_respuesta) "
                    + "VALUES (" + evento + ", " + lugar + ", CURRENT_TIMESTAMP, 'Hola', 'ENVIADA')", Statement.RETURN_GENERATED_KEYS);
            try (java.sql.ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
}
