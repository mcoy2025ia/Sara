import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MigracionesCU28Test {
    @Test
    void baseNuevaConservaCu12YExigeUnDestinatario() throws Exception {
        try (Connection c = nuevaBase()) {
            CargarDatosEjemplo.cargar(c);
            ejecutar(c, "SELECT motivo_rechazo FROM Solicitud_Ejecucion");
            ejecutar(c, "SELECT motivo_rechazo, observacion_correccion FROM Proveedor");
            ejecutar(c, "SELECT * FROM Documento_Proveedor");
            ejecutar(c, "SELECT * FROM Notificacion_Proveedor");
            comprobarDestinatarios(c);
        }
    }

    @Test
    void reabrirNoPierdeAvisosNiDatosDeEjemplo() throws Exception {
        String url = url();
        try (Connection c = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(c);
            ejecutar(c, "INSERT INTO Notificacion_interna(id_usuario, mensaje) VALUES(1, 'aviso usuario')");
            ejecutar(c, "INSERT INTO Notificacion_interna(id_invitado, mensaje) VALUES(1, 'aviso invitado')");
        }
        for (int i = 0; i < 2; i++) {
            DatabaseConnection.reiniciarInicializacion(); // cada vuelta simula reabrir la aplicación
            try (Connection c = DatabaseConnection.getConnection(url)) {
                assertEquals(2, contar(c, "Notificacion_interna"));
                assertEquals(2, contar(c, "Evento"));
                comprobarDestinatarios(c);
            }
        }
    }

    @Test
    void baseConRestriccionAnteriorSeActualizaSinPerderAvisos() throws Exception {
        try (Connection c = nuevaBase()) {
            CargarDatosEjemplo.cargar(c);
            permitirAvisosHistoricos(c);
            ejecutar(c, "INSERT INTO Notificacion_interna(id_usuario, mensaje) VALUES(1, 'conservar')");
            DatabaseConnection.inicializarEsquema(c);
            assertEquals(1, contar(c, "Notificacion_interna"));
            comprobarDestinatarios(c);
        }
    }

    @Test
    void avisoSinDestinatarioRequiereCorreccionSinBorrarloNiRetirarLaRestriccion() throws Exception {
        String url = url();
        try (Connection c = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(c);
            permitirAvisosHistoricos(c);
            ejecutar(c, "INSERT INTO Notificacion_interna(mensaje) VALUES('historico pendiente de identificar')");
        }
        DatabaseConnection.reiniciarInicializacion(); // simula reabrir la aplicación
        assertThrows(SQLException.class, () -> DatabaseConnection.getConnection(url));
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            assertEquals(1, contar(c, "Notificacion_interna"));
            assertThrows(SQLException.class, () -> ejecutar(c,
                    "INSERT INTO Notificacion_interna(id_usuario, id_invitado, mensaje) VALUES(1,1,'invalido')"));
            ejecutar(c, "UPDATE Notificacion_interna SET id_usuario=1");
        }
        DatabaseConnection.reiniciarInicializacion();
        try (Connection c = DatabaseConnection.getConnection(url)) {
            assertEquals(1, contar(c, "Notificacion_interna"));
            comprobarDestinatarios(c);
        }
    }

    private Connection nuevaBase() throws SQLException {
        return DatabaseConnection.getConnection(url());
    }

    private String url() {
        return "jdbc:h2:mem:cu28_migraciones_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    }

    private void permitirAvisosHistoricos(Connection c) throws SQLException {
        ejecutar(c, "ALTER TABLE Notificacion_interna DROP CONSTRAINT destinatario_notificacion");
        ejecutar(c, "ALTER TABLE Notificacion_interna ADD CONSTRAINT destinatario_notificacion CHECK(id_usuario IS NULL OR id_invitado IS NULL)");
    }

    private void comprobarDestinatarios(Connection c) {
        assertThrows(SQLException.class, () -> ejecutar(c,
                "INSERT INTO Notificacion_interna(mensaje) VALUES('sin destinatario')"));
        assertThrows(SQLException.class, () -> ejecutar(c,
                "INSERT INTO Notificacion_interna(id_usuario, id_invitado, mensaje) VALUES(1,1,'dos destinatarios')"));
    }

    private void ejecutar(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private int contar(Connection c, String tabla) throws SQLException {
        try (Statement s = c.createStatement(); var rs = s.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
