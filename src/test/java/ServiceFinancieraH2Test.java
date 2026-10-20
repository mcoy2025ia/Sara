import config.CargarDatosEjemplo;
import config.DatabaseConnection;
import entities.SesionUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryEvento;
import repositories.RepositoryMovimientoFinanciero;
import repositories.RepositoryPlanificacion;
import services.ServiceFinanciera;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CU06: consultar el estado financiero de un evento.
 * Usa una base H2 en memoria con seed.sql. En los datos de ejemplo, el evento 1
 * ("Boda Laura y Daniel") pertenece al organizador 1 (laura.gomez), tiene presupuesto
 * de 15.000.000, dos contrataciones ACTIVA (1.800.000 y 2.200.000) y cuatro
 * movimientos EGRESO que suman 4.000.000.
 */
public class ServiceFinancieraH2Test {

    private static final double DELTA = 0.001;

    private ServiceFinanciera service;

    @BeforeEach
    void prepararBaseDatos() throws Exception {
        System.setProperty("nexteventux.db.url", "jdbc:h2:mem:cu06_test;DB_CLOSE_DELAY=-1");
        try (Connection connection = DatabaseConnection.getConnection()) {
            CargarDatosEjemplo.cargar(connection);
        }
        service = new ServiceFinanciera(
                new RepositoryMovimientoFinanciero(),
                new RepositoryPlanificacion(),
                new RepositoryEvento());
    }

    @AfterEach
    void limpiar() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
        System.clearProperty("nexteventux.db.url");
    }

    @Test
    void calculaPresupuestoComprometidoYDisponible() {
        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        assertEquals(true, estado.get("Presupuesto_Registrado"));
        assertEquals(15_000_000, numero(estado, "Presupuesto"), DELTA);
        assertEquals(4_000_000, numero(estado, "Comprometido"), DELTA);
        assertEquals(11_000_000, numero(estado, "Disponible"), DELTA);
        assertEquals(4_000_000.0 / 15_000_000.0, numero(estado, "Porcentaje_Usado"), DELTA);
        assertEquals(false, estado.get("Excede_Presupuesto"));
        assertEquals(2, ((List<?>) estado.get("Compromisos")).size());
        assertEquals(4, ((List<?>) estado.get("Movimientos")).size());
    }

    @Test
    void balanceEsIngresosMenosEgresos() throws Exception {
        ejecutar("INSERT INTO Movimiento_financiero "
                + "(id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado) "
                + "VALUES (1, 1, 1, 1000000.00, CURRENT_TIMESTAMP, 'Abono del cliente', 'ingreso', 'REGISTRADO')");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        // El tipo en minúsculas también cuenta como ingreso.
        assertEquals(1_000_000, numero(estado, "Ingresos"), DELTA);
        assertEquals(4_000_000, numero(estado, "Egresos"), DELTA);
        assertEquals(-3_000_000, numero(estado, "Balance"), DELTA);
    }

    @Test
    void comprometidoExcluyeContratacionesCanceladas() throws Exception {
        ejecutar("UPDATE Contratacion SET estado = 'CANCELADA' WHERE id_contratacion = 2");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        assertEquals(1_800_000, numero(estado, "Comprometido"), DELTA);
        assertEquals(13_200_000, numero(estado, "Disponible"), DELTA);
        assertEquals(1, ((List<?>) estado.get("Compromisos")).size());
    }

    @Test
    void contratacionesFinalizadasSiguenComprometidas() throws Exception {
        // Finalizar una contratación no devuelve el dinero al presupuesto.
        ejecutar("UPDATE Contratacion SET estado = 'FINALIZADA' WHERE id_evento = 1");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        assertEquals(4_000_000, numero(estado, "Comprometido"), DELTA);
        assertEquals(11_000_000, numero(estado, "Disponible"), DELTA);
        assertEquals(2, ((List<?>) estado.get("Compromisos")).size());
    }

    @Test
    void alertaCuandoSeExcedeElPresupuesto() throws Exception {
        ejecutar("UPDATE Evento SET presupuesto = 3000000.00 WHERE id_evento = 1");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        assertEquals(true, estado.get("Excede_Presupuesto"));
        assertEquals(-1_000_000, numero(estado, "Disponible"), DELTA);
    }

    @Test
    void eventoSinPresupuestoNoMarcaAlerta() throws Exception {
        ejecutar("UPDATE Evento SET presupuesto = NULL WHERE id_evento = 1");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        assertEquals(false, estado.get("Presupuesto_Registrado"));
        assertEquals(false, estado.get("Excede_Presupuesto"));
        assertEquals(0, numero(estado, "Porcentaje_Usado"), DELTA);
        assertEquals(4_000_000, numero(estado, "Comprometido"), DELTA);
    }

    @Test
    void eventoSinContratacionesNiMovimientos() {
        // El evento 2 pertenece al organizador 2 (carlos.rodriguez).
        Map<String, Object> estado = service.consultarEstadoFinanciero(2, sesion(2, 2, "ORGANIZADOR"));

        assertEquals(5_000_000, numero(estado, "Presupuesto"), DELTA);
        assertEquals(0, numero(estado, "Comprometido"), DELTA);
        assertEquals(0, numero(estado, "Balance"), DELTA);
        assertTrue(((List<?>) estado.get("Compromisos")).isEmpty());
        assertTrue(((List<?>) estado.get("Movimientos")).isEmpty());
    }

    @Test
    void rechazaEventoDeOtroOrganizador() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.consultarEstadoFinanciero(2, sesionLaura()));
        assertEquals("Selecciona un evento existente de tu cuenta.", error.getMessage());
    }

    @Test
    void rechazaEventoInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarEstadoFinanciero(999, sesionLaura()));
        assertThrows(IllegalArgumentException.class,
                () -> service.consultarEstadoFinanciero(0, sesionLaura()));
    }

    @Test
    void rechazaSinSesionDeOrganizador() {
        assertThrows(IllegalStateException.class,
                () -> service.consultarEstadoFinanciero(1, null));
        // Usuario 3 es un proveedor.
        assertThrows(IllegalStateException.class,
                () -> service.consultarEstadoFinanciero(1, sesion(3, 1, "PROVEEDOR")));
    }

    @Test
    void importesSeCalculanConBigDecimalSinPerderPrecision() throws Exception {
        // Con double, 0.10 + 0.20 da 0.30000000000000004; con BigDecimal es exacto.
        ejecutar("INSERT INTO Movimiento_financiero "
                + "(id_propuesta, id_evento, id_contratacion, valor, fecha, descripcion, tipo, estado) "
                + "VALUES (1, 1, 1, 0.10, CURRENT_TIMESTAMP, 'Ingreso A', 'INGRESO', 'REGISTRADO'), "
                + "(1, 1, 1, 0.20, CURRENT_TIMESTAMP, 'Ingreso B', 'INGRESO', 'REGISTRADO')");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        assertTrue(estado.get("Ingresos") instanceof BigDecimal);
        assertEquals(0, new BigDecimal("0.30").compareTo((BigDecimal) estado.get("Ingresos")));
        assertEquals(0, new BigDecimal("11000000").compareTo((BigDecimal) estado.get("Disponible")));
    }

    @Test
    void contratacionesDeOtroEventoNoAfectanElPresupuesto() throws Exception {
        // Contratación ACTIVA del evento 2 (organizador 2) sobre su propia solicitud.
        ejecutar("INSERT INTO Contratacion (id_propuesta, id_evento, fecha_contratacion, estado, valor_acordado) "
                + "VALUES (3, 2, CURRENT_TIMESTAMP, 'ACTIVA', 900000.00)");

        Map<String, Object> evento1 = service.consultarEstadoFinanciero(1, sesionLaura());
        Map<String, Object> evento2 = service.consultarEstadoFinanciero(2, sesion(2, 2, "ORGANIZADOR"));

        assertEquals(4_000_000, numero(evento1, "Comprometido"), DELTA);
        assertEquals(900_000, numero(evento2, "Comprometido"), DELTA);
        assertEquals(4_100_000, numero(evento2, "Disponible"), DELTA);
    }

    @Test
    void ignoraContratacionCuyaSolicitudEsDeOtroEvento() throws Exception {
        // La propuesta 3 viene de una solicitud del evento 2, pero la contratación dice evento 1.
        ejecutar("INSERT INTO Contratacion (id_propuesta, id_evento, fecha_contratacion, estado, valor_acordado) "
                + "VALUES (3, 1, CURRENT_TIMESTAMP, 'ACTIVA', 900000.00)");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        assertEquals(4_000_000, numero(estado, "Comprometido"), DELTA);
        assertEquals(2, ((List<?>) estado.get("Compromisos")).size());
    }

    @Test
    void usaValorDeLaPropuestaSiNoHayValorAcordado() throws Exception {
        ejecutar("UPDATE Contratacion SET valor_acordado = NULL WHERE id_contratacion = 1");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        // Propuesta 1 vale 1.800.000, igual que el valor acordado original.
        assertEquals(4_000_000, numero(estado, "Comprometido"), DELTA);
        assertEquals(0, ((Number) estado.get("Contrataciones_Sin_Valor")).intValue());
    }

    @Test
    void contratacionSinNingunValorNoSeCuentaComoCeroYSeInforma() throws Exception {
        ejecutar("UPDATE Contratacion SET valor_acordado = NULL WHERE id_contratacion = 2");
        ejecutar("UPDATE Propuesta SET valor = NULL WHERE id_propuesta = 2");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        // No toma el precio de catálogo del servicio: solo suma lo que sí tiene valor.
        assertEquals(1_800_000, numero(estado, "Comprometido"), DELTA);
        assertEquals(1, ((Number) estado.get("Contrataciones_Sin_Valor")).intValue());
        assertEquals(2, ((List<?>) estado.get("Compromisos")).size());
    }

    @Test
    void muestraLaPlanificacionActivaAunqueHayaUnBorradorMasReciente() throws Exception {
        ejecutar("INSERT INTO Planificacion (id_evento, version, descripcion, estado) "
                + "VALUES (1, 3, 'Borrador con nuevos cambios', 'BORRADOR')");

        Map<String, Object> estado = service.consultarEstadoFinanciero(1, sesionLaura());

        assertEquals("ACTIVA", estado.get("Planificacion_Estado"));
        assertEquals(2, ((Number) estado.get("Planificacion_Version")).intValue());
    }

    @Test
    void presupuestoDisponibleParaOtrosCasosDeUso() throws Exception {
        assertEquals(0, new BigDecimal("11000000")
                .compareTo(service.consultarPresupuestoDisponible(1, sesionLaura())));

        ejecutar("UPDATE Evento SET presupuesto = NULL WHERE id_evento = 1");
        assertEquals(null, service.consultarPresupuestoDisponible(1, sesionLaura()));

        assertThrows(IllegalArgumentException.class,
                () -> service.consultarPresupuestoDisponible(2, sesionLaura()));
    }

    private static SesionUsuario sesionLaura() {
        return sesion(1, 1, "ORGANIZADOR");
    }

    private static SesionUsuario sesion(int usuario, int actor, String tipo) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(usuario);
        sesion.setIdActor(actor);
        sesion.setTipoUsuario(tipo);
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor("ACTIVO");
        return sesion;
    }

    private static double numero(Map<String, Object> estado, String clave) {
        return ((Number) estado.get(clave)).doubleValue();
    }

    private static void ejecutar(String sql) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
