
import config.DatabaseConnection;
import controllers.ControllerContratacionOrganizador;
import entities.Contratacion;
import entities.Organizador;
import entities.SesionUsuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryContratacion;
import repositories.RepositoryOrganizador;
import services.ServiceContratacionOrganizador;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CU026ContratarOrganizadorTest {

    private String url;
    private RepositoryContratacion repositoryContratacion;
    private RepositoryOrganizador repositoryOrganizador;
    private ServiceContratacionOrganizador servicio;
    private ControllerContratacionOrganizador controlador;

    private final int idCliente = 9001;
    private final int idUsuarioOrganizador = 9002;
    private final int idOrganizador = 9001;

    @BeforeEach
    void preparar() throws Exception {

        url = "jdbc:h2:mem:cu26_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1";

        try (Connection connection =
                     DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado,
                     fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (9001, 'Cliente CU26', 'cliente_cu26', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'CLIENTE')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña, estado,
                     fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (9002, 'Organizador CU26', 'organizador_cu26', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, telefono, estado,
                     ciudad, especialidad, portafolio, tarifa_base,
                     estado_verificacion)
                    VALUES
                    (9001, 9002, 'Eventos CU26', '3000000000', 'ACTIVO',
                     'Bogota', 'Bodas y eventos sociales',
                     'Organizacion de bodas y celebraciones',
                     1500000, 'VERIFICADO')
                    """);
        }

        repositoryContratacion = new RepositoryContratacion(url);
        repositoryOrganizador = new RepositoryOrganizador(url);

        servicio = new ServiceContratacionOrganizador(
                repositoryContratacion,
                repositoryOrganizador);

        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdUsuario(idCliente);
        sesion.setNombre("Cliente CU26");
        sesion.setTipoUsuario("CLIENTE");
        sesion.setEstadoUsuario("ACTIVO");

        controlador = new ControllerContratacionOrganizador(servicio, sesion);
    }

    @AfterEach
    void limpiar() throws Exception {

        try (Connection connection =
                     DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {

            statement.execute("DROP ALL OBJECTS");
        }
    }

    @Test
    void debeEncontrarOrganizadorActivoYVerificado() {

        Map<String, Object> filtros = filtrosValidos();

        List<Organizador> resultado =
                controlador.consultarOrganizadores(filtros);

        assertEquals(1, resultado.size());
        assertEquals(idOrganizador, resultado.get(0).id());
        assertEquals("Organizador CU26", resultado.get(0).nombre());
        assertEquals(
                new BigDecimal("1500000.00"),
                resultado.get(0).tarifaBase());
    }

    @Test
    void debeRegistrarSolicitudPendienteYNotificarOrganizador()
            throws Exception {

        LocalDate fecha = LocalDate.now().plusDays(40);

        Contratacion datos = new Contratacion(
                0,
                idOrganizador,
                "Boda",
                "Bogota",
                fecha,
                new BigDecimal("2000000"),
                "Celebracion familiar");

        controlador.contratarOrganizador(idOrganizador, datos);

        assertTrue(datos.getIdContratacion() > 0);
        assertEquals("PENDIENTE", datos.getEstado());

        Contratacion guardada = repositoryContratacion.buscarContratacion(
                datos.getIdContratacion());

        assertNotNull(guardada);
        assertEquals(idCliente, guardada.getIdCliente());
        assertEquals(idOrganizador, guardada.getIdOrganizador());
        assertEquals("PENDIENTE", guardada.getEstado());
        assertEquals(fecha, guardada.getFechaAproximada());

        try (Connection connection = DatabaseConnection.getConnection(url);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*)
                     FROM Notificacion_interna
                     WHERE id_usuario = ?
                       AND mensaje LIKE 'Nueva solicitud de contratacion%'
                     """)) {

            statement.setInt(1, idUsuarioOrganizador);

            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }

        assertEquals(
                1,
                controlador.consultarContrataciones(null).size());
    }

    @Test
    void noDebePermitirContratarOrganizadorNoVerificado() {

        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                    UPDATE Organizador
                    SET estado_verificacion = 'PENDIENTE'
                    WHERE id_organizador = 9001
                    """);

        } catch (Exception e) {
            fail("No se pudo preparar el escenario de organizador pendiente.", e);
        }

        Contratacion datos = new Contratacion(
                0,
                idOrganizador,
                "Boda",
                "Bogota",
                LocalDate.now().plusDays(40),
                new BigDecimal("2000000"),
                null);

        assertThrows(
                IllegalStateException.class,
                () -> controlador.contratarOrganizador(
                        idOrganizador, datos));
    }

    @Test
    void debeRechazarPresupuestoNegativo() {

        Map<String, Object> filtros = filtrosValidos();
        filtros.put("presupuestoEstimado", new BigDecimal("-1"));

        assertThrows(
                IllegalArgumentException.class,
                () -> controlador.consultarOrganizadores(filtros));
    }

    @Test
    void debeInformarCuandoNoHayCoincidencias() {

        Map<String, Object> filtros = filtrosValidos();
        filtros.put("ciudad", "Cali");

        List<Organizador> resultado =
                controlador.consultarOrganizadores(filtros);

        assertTrue(resultado.isEmpty());
    }

    private Map<String, Object> filtrosValidos() {

        Map<String, Object> filtros = new HashMap<>();
        filtros.put("ciudad", "Bogota");
        filtros.put("tipoEvento", "Boda");
        filtros.put("presupuestoEstimado", new BigDecimal("2000000"));
        filtros.put(
                "fechaAproximada",
                LocalDate.now().plusDays(40));

        return filtros;
    }
}
