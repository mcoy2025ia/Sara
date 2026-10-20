
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.DatabaseConnection;
import entities.PropuestaServicio;
import entities.SesionUsuario;
import entities.SolicitudEjecucion;
import repositories.RepositorySolicitudEjecucion;
import repositories.RepositoryPropuestaServicio;
import services.ServiceSolicitudes;

public class ServiceSolicitudesH2Test {

    private static final String URL
            = "jdbc:h2:mem:cu12solicitudes;DB_CLOSE_DELAY=-1";

    private ServiceSolicitudes service;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty("nexteventux.db.url", URL);

        // La conexion inicializa el esquema y las migraciones de CU12
        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {

            // Estos registros solo se usan para probar las solicitudes
            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento,
                     id_servicio, fecha_solicitud, estado)
                    VALUES
                    (9001, 9001, 1, 1,
                     TIMESTAMP '2026-10-18 10:00:00', 'PENDIENTE'),
                    (9002, 9002, 1, 2,
                     TIMESTAMP '2026-10-18 11:00:00', 'PENDIENTE'),
                    (9003, 9001, 2, 1,
                     TIMESTAMP '2026-10-18 12:00:00', 'RESPONDIDA')
                    """);

            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        service = crearServicio(sesion("PROVEEDOR", 9001, 9001));
    }

    private ServiceSolicitudes crearServicio(SesionUsuario sesion) {
        return new ServiceSolicitudes(
                new RepositorySolicitudEjecucion(),
                new RepositoryPropuestaServicio(),
                sesion);
    }

    private SesionUsuario sesion(
            String rol,
            int idActor,
            int idUsuario) {

        SesionUsuario sesion = new SesionUsuario();
        sesion.setTipoUsuario(rol);
        sesion.setIdActor(idActor);
        sesion.setIdUsuario(idUsuario);
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor(
                "PROVEEDOR".equals(rol) ? "APROBADO" : "ACTIVO");
        return sesion;
    }

    private void prepararDatosSolicitud() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado)
                    VALUES
                    (9001, 9001, 'Organizador de prueba',
                     '3000000000', 'ACTIVO'),
                    (9002, 9003, 'Otro organizador',
                     '3000000002', 'ACTIVO')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre, empresa,
                     correo, telefono, direccion, estado)
                    VALUES
                    (9001, 9002, 'Proveedor de prueba',
                     'Servicios de prueba', 'cu12@ejemplo.com',
                     '3000000001', 'Bogota', 'APROBADO')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre,
                     fecha_inicio, fecha_fin, estado, fecha_creacion)
                    VALUES
                    (9001, 9001, 'Evento de prueba',
                     TIMESTAMP '2026-11-20 10:00:00',
                     TIMESTAMP '2026-11-20 18:00:00',
                     'PLANEADO', CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO Servicio
                    (id_servicio, id_proveedor, nombre, estado)
                    VALUES
                    (9001, 9001, 'Fotografia de prueba', 'DISPONIBLE')
                    """);
            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }

    @AfterEach
    void limpiar() throws Exception {

        try {
            // Conexion directa para limpiar sin ejecutar las migraciones
            try (Connection connection
                    = DriverManager.getConnection(URL, "sa", ""); Statement statement = connection.createStatement()) {

                statement.execute("DROP ALL OBJECTS");
            }
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    @Test
    void proveedorSoloConsultaSusSolicitudes() {

        List<SolicitudEjecucion> solicitudes
                = service.listarSolicitudesProveedor(9001);

        assertEquals(2, solicitudes.size());

        for (SolicitudEjecucion solicitud : solicitudes) {
            assertEquals(9001, solicitud.getIdProveedor());
        }
    }

    @Test
    void proveedorPuedeRechazarSolicitudPendiente() throws Exception {

        service.rechazarSolicitud(
                9001,
                9001,
                "No tengo disponibilidad para el evento.");

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                     SELECT estado, motivo_rechazo, fecha_respuesta
                     FROM Solicitud_Ejecucion
                     WHERE id_solicitud = 9001
                     """)) {

            assertTrue(result.next());
            assertEquals("RECHAZADA", result.getString("estado"));
            assertEquals(
                    "No tengo disponibilidad para el evento.",
                    result.getString("motivo_rechazo"));
            assertNotNull(result.getTimestamp("fecha_respuesta"));
        }
    }

    @Test
    void proveedorNoPuedeRechazarSolicitudAjena() {

        assertThrows(
                IllegalStateException.class,
                () -> service.rechazarSolicitud(
                        9001,
                        9002,
                        "Intento de rechazo ajeno."));

        List<SolicitudEjecucion> solicitudes
                = crearServicio(sesion("PROVEEDOR", 9002, 9002))
                        .listarSolicitudesProveedor(9002);

        assertEquals("PENDIENTE", solicitudes.get(0).getEstado());
    }

    @Test
    void noSePuedeRechazarUnaSolicitudYaRespondida() {

        assertThrows(
                IllegalStateException.class,
                () -> service.rechazarSolicitud(
                        9001,
                        9003,
                        "Intento de modificar una respuesta anterior."));
    }

    @Test
    void noSePuedeRechazarDosVeces() {

        service.rechazarSolicitud(
                9001,
                9001,
                "Primera respuesta.");

        assertThrows(
                IllegalStateException.class,
                () -> service.rechazarSolicitud(
                        9001,
                        9001,
                        "Segunda respuesta."));
    }

    @Test
    void solicitudRechazadaNoAdmitePropuestaPosterior()
            throws Exception {

        service.rechazarSolicitud(
                9001,
                9001,
                "No hay disponibilidad.");

        assertThrows(
                IllegalStateException.class,
                () -> service.enviarPropuesta(
                        9001,
                        9001,
                        new BigDecimal("100"),
                        "Alcance",
                        "Condiciones"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT s.estado,
                               (SELECT COUNT(*) FROM Propuesta p
                                WHERE p.id_solicitud = s.id_solicitud)
                                   AS cantidad
                        FROM Solicitud_Ejecucion s
                        WHERE s.id_solicitud = 9001
                        """)) {
            assertTrue(result.next());
            assertEquals("RECHAZADA", result.getString("estado"));
            assertEquals(0, result.getInt("cantidad"));
        }
    }

    @Test
    void precioDePropuestaInvalidoNoCreaPropuestaNiCambiaSolicitud()
            throws Exception {

        BigDecimal[] preciosInvalidos = {
                new BigDecimal("-50000"),
                new BigDecimal("-0.5"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("1.234"),
                new BigDecimal("10000000000.00")
        };

        for (BigDecimal precio : preciosInvalidos) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.enviarPropuesta(
                            9001,
                            9001,
                            precio,
                            "Alcance de prueba",
                            "Condiciones de prueba"));

            try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                         SELECT s.estado,
                                s.fecha_respuesta,
                                (SELECT COUNT(*)
                                 FROM Propuesta p
                                 WHERE p.id_solicitud = s.id_solicitud)
                                    AS cantidad_propuestas
                         FROM Solicitud_Ejecucion s
                         WHERE s.id_solicitud = 9001
                         """)) {

                assertTrue(result.next());
                assertEquals("PENDIENTE", result.getString("estado"));
                assertNull(result.getTimestamp("fecha_respuesta"));
                assertEquals(0, result.getInt("cantidad_propuestas"));
            }
        }
    }

    @Test
    void preciosPositivosSeGuardanYCreanVersionesEnOrden()
            throws Exception {

        int primera = service.enviarPropuesta(
                9001,
                9001,
                new BigDecimal("1500000.000"),
                "Alcance inicial",
                "Condiciones iniciales");

        int segunda = service.enviarPropuesta(
                9001,
                9001,
                new BigDecimal("1500000.50"),
                "Alcance actualizado",
                "Condiciones actualizadas");

        try (Connection connection = DatabaseConnection.getConnection();
                var statement = connection.prepareStatement("""
                        SELECT numero_version, valor, vigente
                        FROM Propuesta
                        WHERE id_propuesta IN (?, ?)
                        ORDER BY numero_version
                        """)) {

            statement.setInt(1, primera);
            statement.setInt(2, segunda);

            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt("numero_version"));
                assertEquals(
                        new BigDecimal("1500000.00"),
                        result.getBigDecimal("valor"));
                assertFalse(result.getBoolean("vigente"));

                assertTrue(result.next());
                assertEquals(2, result.getInt("numero_version"));
                assertEquals(
                        new BigDecimal("1500000.50"),
                        result.getBigDecimal("valor"));
                assertTrue(result.getBoolean("vigente"));
                assertFalse(result.next());
            }
        }

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT estado
                        FROM Solicitud_Ejecucion
                        WHERE id_solicitud = 9001
                        """)) {
            assertTrue(result.next());
            assertEquals("EN_NEGOCIACION", result.getString("estado"));
        }

        List<PropuestaServicio> historial
                = service.listarHistorialPropuestas(9001, 9001);
        assertEquals(2, historial.size());
        assertEquals(2, historial.get(0).getNumeroVersion());
        assertEquals(1, historial.get(1).getNumeroVersion());
        assertTrue(
                service.listarHistorialPropuestas(9001, 9002).isEmpty());
    }

    @Test
    void operacionesCu12RequierenSesionYRolPropietario() {

        assertThrows(
                IllegalArgumentException.class,
                () -> crearServicio(null));

        ServiceSolicitudes organizador
                = crearServicio(sesion("ORGANIZADOR", 9001, 9001));
        assertThrows(
                IllegalStateException.class,
                () -> organizador.enviarPropuesta(
                        9001,
                        9001,
                        new BigDecimal("100"),
                        "Alcance",
                        "Condiciones"));
        assertThrows(
                IllegalStateException.class,
                () -> organizador.listarSolicitudesProveedor(9001));
        assertThrows(
                IllegalStateException.class,
                () -> service.crearSolicitud(
                        9001, 9001, 9001, "Intento desde proveedor"));

        assertThrows(
                IllegalStateException.class,
                () -> service.enviarPropuesta(
                        9002,
                        9001,
                        new BigDecimal("100"),
                        "Alcance",
                        "Condiciones"));

        SesionUsuario sesionCerrada
                = sesion("PROVEEDOR", 9001, 9001);
        ServiceSolicitudes servicioSesionCerrada
                = crearServicio(sesionCerrada);
        sesionCerrada.cerrar();
        assertThrows(
                IllegalStateException.class,
                () -> servicioSesionCerrada.listarSolicitudesProveedor(9001));

        SesionUsuario usuarioInactivo
                = sesion("PROVEEDOR", 9001, 9001);
        usuarioInactivo.setEstadoUsuario("INACTIVO");
        assertThrows(
                IllegalStateException.class,
                () -> crearServicio(usuarioInactivo)
                        .listarSolicitudesProveedor(9001));

        SesionUsuario proveedorNoAprobado
                = sesion("PROVEEDOR", 9001, 9001);
        proveedorNoAprobado.setEstadoActor("PENDIENTE");
        assertThrows(
                IllegalStateException.class,
                () -> crearServicio(proveedorNoAprobado)
                        .listarSolicitudesProveedor(9001));
    }

    @Test
    void propuestaRequiereDatosValidosYSolicitudPropiaActiva()
            throws Exception {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.enviarPropuesta(
                        9001, 9001, new BigDecimal("100"), null, "Condiciones"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.enviarPropuesta(
                        9001, 9001, new BigDecimal("100"), "  ", "Condiciones"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.enviarPropuesta(
                        9001, 9001, new BigDecimal("100"), "A".repeat(256),
                        "Condiciones"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.enviarPropuesta(
                        9001, 9001, new BigDecimal("100"), "Alcance", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.enviarPropuesta(
                        9001, 9001, new BigDecimal("100"), "Alcance", "  "));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.enviarPropuesta(
                        9001, 9001, new BigDecimal("100"), "Alcance",
                        "C".repeat(1001)));

        assertThrows(
                IllegalStateException.class,
                () -> service.enviarPropuesta(
                        9001, 9999, new BigDecimal("100"),
                        "Alcance", "Condiciones"));
        assertThrows(
                IllegalStateException.class,
                () -> service.enviarPropuesta(
                        9001, 9002, new BigDecimal("100"),
                        "Alcance", "Condiciones"));
        assertThrows(
                IllegalStateException.class,
                () -> service.enviarPropuesta(
                        9001, 9003, new BigDecimal("100"),
                        "Alcance", "Condiciones"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT id_solicitud, estado,
                               (SELECT COUNT(*) FROM Propuesta p
                                WHERE p.id_solicitud = s.id_solicitud)
                        FROM Solicitud_Ejecucion s
                        WHERE id_solicitud IN (9001, 9002, 9003)
                        ORDER BY id_solicitud
                        """)) {
            assertTrue(result.next());
            assertEquals(9001, result.getInt(1));
            assertEquals("PENDIENTE", result.getString(2));
            assertEquals(0, result.getInt(3));
            assertTrue(result.next());
            assertEquals(9002, result.getInt(1));
            assertEquals(0, result.getInt(3));
            assertTrue(result.next());
            assertEquals(9003, result.getInt(1));
            assertEquals("RESPONDIDA", result.getString(2));
            assertEquals(0, result.getInt(3));
            assertFalse(result.next());
        }
    }

    @Test
    void errorAlGuardarPropuestaRevierteVersionAnteriorYSolicitud()
            throws Exception {

        int propuestaInicial = service.enviarPropuesta(
                9001,
                9001,
                new BigDecimal("50000"),
                "Alcance inicial",
                "Condiciones iniciales");

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE Propuesta
                    ADD CONSTRAINT ck_cu12_precio_prueba
                    CHECK (valor < 100000)
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> service.enviarPropuesta(
                        9001,
                        9001,
                        new BigDecimal("150000"),
                        "Alcance nuevo",
                        "Condiciones nuevas"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT p.vigente, p.valor, s.estado,
                               (SELECT COUNT(*) FROM Propuesta p2
                                WHERE p2.id_solicitud = s.id_solicitud)
                                   AS cantidad
                        FROM Propuesta p
                        JOIN Solicitud_Ejecucion s
                          ON s.id_solicitud = p.id_solicitud
                        WHERE p.id_propuesta = """ + propuestaInicial)) {
            assertTrue(result.next());
            assertTrue(result.getBoolean("vigente"));
            assertEquals(
                    new BigDecimal("50000.00"),
                    result.getBigDecimal("valor"));
            assertEquals("EN_NEGOCIACION", result.getString("estado"));
            assertEquals(1, result.getInt("cantidad"));
        }
    }

    @Test
    void falloAlActualizarEstadoRevierteLaPropuestaInsertada()
            throws Exception {

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("""
                    ALTER TABLE Solicitud_Ejecucion
                    ADD CONSTRAINT ck_cu12_estado_prueba
                    CHECK (id_solicitud <> 9001
                           OR estado <> 'EN_NEGOCIACION')
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> service.enviarPropuesta(
                        9001,
                        9001,
                        new BigDecimal("150000"),
                        "Alcance",
                        "Condiciones"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT estado, fecha_respuesta,
                               (SELECT COUNT(*) FROM Propuesta p
                                WHERE p.id_solicitud = s.id_solicitud)
                                   AS cantidad
                        FROM Solicitud_Ejecucion s
                        WHERE s.id_solicitud = 9001
                        """)) {
            assertTrue(result.next());
            assertEquals("PENDIENTE", result.getString("estado"));
            assertNull(result.getTimestamp("fecha_respuesta"));
            assertEquals(0, result.getInt("cantidad"));
        }
    }

    @Test
    void inicializarEsquemaRepetidamenteConservaPropuestas()
            throws Exception {

        int idPropuesta = service.enviarPropuesta(
                9001,
                9001,
                new BigDecimal("75000"),
                "Alcance persistente",
                "Condiciones persistentes");

        for (int intento = 0; intento < 3; intento++) {
            try (Connection connection = DatabaseConnection.getConnection();
                    var statement = connection.prepareStatement("""
                            SELECT valor, numero_version, vigente
                            FROM Propuesta
                            WHERE id_propuesta = ?
                            """)) {
                statement.setInt(1, idPropuesta);
                try (ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    assertEquals(
                            new BigDecimal("75000.00"),
                            result.getBigDecimal("valor"));
                    assertEquals(1, result.getInt("numero_version"));
                    assertTrue(result.getBoolean("vigente"));
                }
            }
        }
    }

    @Test
    void motivoDeRechazoEsObligatorio() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rechazarSolicitud(9001, 9001, null));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rechazarSolicitud(9001, 9001, "   "));
    }

    @Test
    void crearSolicitudValidaIdsYDescripcionAntesDeEscribir()
            throws Exception {

        ServiceSolicitudes organizador
                = crearServicio(sesion("ORGANIZADOR", 9001, 9001));

        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.crearSolicitud(0, 9001, 9001, "Necesidad"));
        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.crearSolicitud(9001, -1, 9001, "Necesidad"));
        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.crearSolicitud(9001, 9001, 0, "Necesidad"));
        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.crearSolicitud(9001, 9001, 9001, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.crearSolicitud(9001, 9001, 9001, "   "));
        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.crearSolicitud(
                        9001, 9001, 9001, "x".repeat(501)));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT COUNT(*) FROM Necesidad_Servicio_Evento
                        WHERE id_evento = 9001
                        """)) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    @Test
    void mismaNecesidadNoPuedeSolicitarseDosVecesAlMismoProveedor()
            throws Exception {

        prepararDatosSolicitud();
        ServiceSolicitudes organizador
                = crearServicio(sesion("ORGANIZADOR", 9001, 9001));

        int idSolicitud = organizador.crearSolicitud(
                9001, 9001, 9001, "Necesito fotografia.");

        int idNecesidad;
        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT id_necesidad_servicio
                        FROM Solicitud_Ejecucion
                        WHERE id_solicitud = """ + idSolicitud)) {
            assertTrue(result.next());
            idNecesidad = result.getInt(1);
        }

        assertThrows(
                IllegalStateException.class,
                () -> organizador.crearSolicitudParaNecesidad(
                        9001, 9001, 9001, idNecesidad));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT COUNT(*)
                        FROM Solicitud_Ejecucion
                        WHERE id_necesidad_servicio = """ + idNecesidad)) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void servicioNoDisponibleYEventoCerradoNoCreanSolicitud()
            throws Exception {

        prepararDatosSolicitud();
        ServiceSolicitudes organizador
                = crearServicio(sesion("ORGANIZADOR", 9001, 9001));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE Servicio
                    SET estado = 'RETIRADO'
                    WHERE id_servicio = 9001
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> organizador.crearSolicitud(
                        9001, 9001, 9001, "Necesidad"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE Servicio
                    SET estado = 'DISPONIBLE'
                    WHERE id_servicio = 9001
                    """);
        }

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE Proveedor
                    SET estado = 'PENDIENTE'
                    WHERE id_proveedor = 9001
                    """);
        }
        assertThrows(
                IllegalStateException.class,
                () -> organizador.crearSolicitud(
                        9001, 9001, 9001, "Necesidad"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE Proveedor
                    SET estado = 'APROBADO'
                    WHERE id_proveedor = 9001
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> organizador.crearSolicitud(
                        9001, 9999, 9001, "Necesidad"));
        assertThrows(
                IllegalStateException.class,
                () -> organizador.crearSolicitud(
                        9001, 9001, 9999, "Necesidad"));
        assertThrows(
                IllegalStateException.class,
                () -> crearServicio(sesion("ORGANIZADOR", 9002, 9003))
                        .crearSolicitud(
                                9002, 9001, 9001, "Necesidad ajena"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE Evento
                    SET estado = 'CANCELADO'
                    WHERE id_evento = 9001
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> organizador.crearSolicitud(
                        9001, 9001, 9001, "Necesidad"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT
                          (SELECT COUNT(*) FROM Necesidad_Servicio_Evento
                           WHERE id_evento = 9001) +
                          (SELECT COUNT(*) FROM Solicitud_Ejecucion
                           WHERE id_evento = 9001)
                        """)) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    @Test
    void organizadorPuedeCrearSolicitudParaSuEvento() throws Exception {

        prepararDatosSolicitud();

        int idSolicitud = crearServicio(
                sesion("ORGANIZADOR", 9001, 9001)).crearSolicitud(
                9001,
                9001,
                9001,
                "Necesito fotografia para mi evento.");

        assertTrue(idSolicitud > 0);

        try (Connection connection = DatabaseConnection.getConnection(); java.sql.PreparedStatement statement
                = connection.prepareStatement("""
                             SELECT s.estado,
                                    s.id_evento,
                                    s.id_servicio,
                                    n.descripcion
                             FROM Solicitud_Ejecucion s
                             JOIN Necesidad_Servicio_Evento n
                               ON n.id_necesidad_servicio =
                                  s.id_necesidad_servicio
                             WHERE s.id_solicitud = ?
                             """)) {

            statement.setInt(1, idSolicitud);

            try (ResultSet result = statement.executeQuery()) {

                assertTrue(result.next());
                assertEquals("PENDIENTE", result.getString("estado"));
                assertEquals(9001, result.getInt("id_evento"));
                assertEquals(9001, result.getInt("id_servicio"));
                assertEquals(
                        "Necesito fotografia para mi evento.",
                        result.getString("descripcion"));
            }
        }
    }

    @Test
    void soloUnaPropuestaPuedeEstarSeleccionadaPorNecesidad()
            throws Exception {

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado)
                    VALUES
                    (9001, 9001, 'Organizador de prueba',
                     '3000000000', 'ACTIVO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre,
                     fecha_inicio, fecha_fin, estado, fecha_creacion)
                    VALUES
                    (9001, 9001, 'Evento de prueba',
                     TIMESTAMP '2026-11-20 10:00:00',
                     TIMESTAMP '2026-11-20 18:00:00',
                     'PLANEADO', CURRENT_TIMESTAMP)
                    """);

            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_necesidad_servicio, id_evento, descripcion, estado)
                    VALUES
                    (9001, 9001, 'Necesito fotografia', 'ABIERTA')
                    """);

            statement.executeUpdate("""
                    UPDATE Solicitud_Ejecucion
                    SET id_evento = 9001,
                        id_necesidad_servicio = 9001,
                        estado = 'EN_NEGOCIACION'
                    WHERE id_solicitud = 9001
                    """);

            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento,
                     id_servicio, id_necesidad_servicio,
                     fecha_solicitud, estado)
                    VALUES
                    (9100, 9002, 9001, 2, 9001,
                     CURRENT_TIMESTAMP, 'EN_NEGOCIACION')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Propuesta
                    (id_propuesta, id_solicitud, numero_version,
                     valor, descripcion, condiciones,
                     fecha_envio, vigente, estado)
                    VALUES
                    (9101, 9001, 1, 800000,
                     'Primera propuesta', 'Condiciones A',
                     CURRENT_TIMESTAMP, FALSE, 'ENVIADA'),
                    (9103, 9001, 2, 850000,
                     'Version vigente', 'Condiciones A2',
                     CURRENT_TIMESTAMP, TRUE, 'ENVIADA'),
                    (9102, 9100, 1, 900000,
                     'Segunda propuesta', 'Condiciones B',
                     CURRENT_TIMESTAMP, TRUE, 'ENVIADA')
                    """);

            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        ServiceSolicitudes servicioOrganizador
                = crearServicio(sesion("ORGANIZADOR", 9001, 9001));
        List<PropuestaServicio> ofertasOrganizador
                = servicioOrganizador.listarPropuestasOrganizador(9001);
        assertEquals(2, ofertasOrganizador.size());
        assertTrue(ofertasOrganizador.stream()
                .anyMatch(propuesta -> propuesta.getIdPropuesta() == 9103));
        assertTrue(ofertasOrganizador.stream()
                .anyMatch(propuesta -> propuesta.getIdPropuesta() == 9102));
        assertFalse(ofertasOrganizador.stream()
                .anyMatch(propuesta -> propuesta.getIdPropuesta() == 9101));
        assertTrue(
                crearServicio(sesion("ORGANIZADOR", 9002, 9003))
                        .listarPropuestasOrganizador(9002)
                        .isEmpty());

        assertThrows(
                IllegalStateException.class,
                () -> servicioOrganizador.seleccionarPropuesta(9001, 9101));
        servicioOrganizador.seleccionarPropuesta(9001, 9103);

        // El organizador puede cambiar de seleccion antes de contratar
        servicioOrganizador.seleccionarPropuesta(9001, 9102);

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                     SELECT id_propuesta_seleccionada
                     FROM Necesidad_Servicio_Evento
                     WHERE id_necesidad_servicio = 9001
                     """)) {

            assertTrue(result.next());
            assertEquals(9102, result.getInt(1));
        }

        // Un organizador diferente no puede seleccionar la propuesta
        assertThrows(
                IllegalStateException.class,
                () -> servicioOrganizador.seleccionarPropuesta(9002, 9101));
        ServiceSolicitudes otroOrganizador
                = crearServicio(sesion("ORGANIZADOR", 9002, 9003));
        assertThrows(
                IllegalStateException.class,
                () -> otroOrganizador.seleccionarPropuesta(9002, 9101));
        // No se puede rechazar la propuesta seleccionada
        assertThrows(
                IllegalStateException.class,
                () -> servicioOrganizador.rechazarPropuesta(9001, 9102));

        // Si no esta seleccionada, el organizador puede rechazarla
        servicioOrganizador.rechazarPropuesta(9001, 9103);

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                     SELECT estado, vigente
                     FROM Propuesta
                     WHERE id_propuesta = 9103
                     """)) {

            assertTrue(result.next());
            assertEquals("RECHAZADA", result.getString("estado"));
            assertFalse(result.getBoolean("vigente"));
        }

        assertThrows(
                IllegalStateException.class,
                () -> service.enviarPropuesta(
                        9001,
                        9001,
                        new BigDecimal("950000"),
                        "Nueva version",
                        "Condiciones"));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT estado FROM Solicitud_Ejecucion
                        WHERE id_solicitud = 9001
                        """)) {
            assertTrue(result.next());
            assertEquals(
                    "PROPUESTA_RECHAZADA",
                    result.getString("estado"));
        }
    }

    @Test
    void noSePuedeCambiarLaSeleccionDespuesDeCrearContratacion()
            throws Exception {

        prepararDatosSolicitud();

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");
            statement.executeUpdate("""
                    INSERT INTO Necesidad_Servicio_Evento
                    (id_necesidad_servicio, id_evento, descripcion, estado)
                    VALUES
                    (9001, 9001, 'Necesidad contratada', 'ABIERTA')
                    """);
            statement.executeUpdate("""
                    UPDATE Solicitud_Ejecucion
                    SET id_evento = 9001,
                        id_necesidad_servicio = 9001,
                        estado = 'EN_NEGOCIACION'
                    WHERE id_solicitud = 9001
                    """);
            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento,
                     id_servicio, id_necesidad_servicio,
                     fecha_solicitud, estado)
                    VALUES
                    (9100, 9002, 9001, 2, 9001,
                     CURRENT_TIMESTAMP, 'EN_NEGOCIACION')
                    """);
            statement.executeUpdate("""
                    INSERT INTO Propuesta
                    (id_propuesta, id_solicitud, numero_version,
                     valor, descripcion, condiciones,
                     fecha_envio, vigente, estado)
                    VALUES
                    (9301, 9001, 1, 50000,
                     'Seleccionada', 'Condiciones A',
                     CURRENT_TIMESTAMP, TRUE, 'ENVIADA'),
                    (9302, 9100, 1, 60000,
                     'Alternativa', 'Condiciones B',
                     CURRENT_TIMESTAMP, TRUE, 'ENVIADA')
                    """);
            statement.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }

        ServiceSolicitudes organizador
                = crearServicio(sesion("ORGANIZADOR", 9001, 9001));
        organizador.seleccionarPropuesta(9001, 9301);

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO Contratacion
                    (id_propuesta, id_evento, fecha_contratacion,
                     estado, condiciones, valor_acordado)
                    VALUES
                    (9301, 9001, CURRENT_TIMESTAMP,
                     'ACTIVA', 'Condiciones A', 50000)
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> organizador.seleccionarPropuesta(9001, 9302));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT id_propuesta_seleccionada
                        FROM Necesidad_Servicio_Evento
                        WHERE id_necesidad_servicio = 9001
                        """)) {
            assertTrue(result.next());
            assertEquals(9301, result.getInt(1));
        }
    }
}
