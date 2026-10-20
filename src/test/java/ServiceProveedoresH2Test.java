
import config.DatabaseConnection;
import controllers.ControllerProveedores;
import entities.Servicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceProveedoresH2Test {

    private ControllerProveedores controller;
    private RepositoryServicio repositoryServicio;
    private RepositoryDisponibilidadServicio repositoryDisponibilidad;

    @BeforeEach
    void prepararBaseDatos() throws Exception {

        System.setProperty(
                "nexteventux.db.url",
                "jdbc:h2:mem:cu11;DB_CLOSE_DELAY=-1");

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {

            statement.execute("SET REFERENTIAL_INTEGRITY FALSE");

            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre, empresa, correo,
                     telefono, direccion, estado)
                    VALUES
                    (9001, 9001, 'Proveedor CU11', 'Empresa CU11',
                     'cu11@nexteventux.local', '3000000000',
                     'Bogota', 'ACTIVO')
                    """);
        }

        repositoryServicio = new RepositoryServicio();
        repositoryDisponibilidad
                = new RepositoryDisponibilidadServicio();

        ServiceProveedores service
                = new ServiceProveedores(
                        repositoryServicio,
                        repositoryDisponibilidad);

        controller = new ControllerProveedores(service);
    }

    @AfterEach
    void limpiar() throws Exception {

        try (Connection connection = DatabaseConnection.getConnection(); Statement statement = connection.createStatement()) {

            statement.execute("DROP ALL OBJECTS");
        }

        System.clearProperty("nexteventux.db.url");
    }

    @Test
    void flujoCU11DebePersistirEnH2() throws Exception {

        Servicio servicio = new Servicio();

        servicio.setIdProveedor(9001);
        servicio.setNombre("Fotografia CU11");
        servicio.setDescripcion(
                "Fotografia para eventos sociales");
        servicio.setPrecio(750000);
        servicio.setCategoria("FOTOGRAFIA");
        servicio.setTipo("SERVICIO");
        servicio.setCantidadMaximaPersonas(150);
        servicio.setCiudadZona("Bogota");
        servicio.setAnticipacionMinima(5);
        servicio.setEstado("BORRADOR");

        controller.crearServicio(servicio);

        assertTrue(servicio.getIdServicio() > 0);

        Servicio guardado
                = controller.buscarServicio(
                        servicio.getIdServicio());

        assertNotNull(guardado);
        assertEquals(
                "Fotografia CU11",
                guardado.getNombre());

        Date fecha = Date.from(
                LocalDate.now()
                        .plusDays(15)
                        .atStartOfDay(
                                ZoneId.systemDefault())
                        .toInstant());

        controller.registrarDisponibilidadServicio(
                9001,
                servicio.getIdServicio(),
                fecha,
                "09:00 - 13:00");

        assertEquals(
                1,
                controller
                        .listarDisponibilidadesServicio(
                                servicio.getIdServicio())
                        .size());

        controller.publicarServicio(
                9001,
                servicio.getIdServicio());

        assertEquals(
                "DISPONIBLE",
                controller
                        .buscarServicio(
                                servicio.getIdServicio())
                        .getEstado());

        try (Connection connection
                = DatabaseConnection.getConnection(); Statement statement
                = connection.createStatement()) {

            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_proveedor, id_evento,
                     fecha_solicitud, estado, id_servicio)
                    VALUES
                    (9001, 9001,
                     CURRENT_TIMESTAMP,
                     'PENDIENTE',
                    """ + servicio.getIdServicio() + ")");
        }

        assertThrows(
                IllegalStateException.class,
                () -> controller.retirarServicio(
                        9001,
                        servicio.getIdServicio()));

        try (Connection connection
                = DatabaseConnection.getConnection(); Statement statement
                = connection.createStatement()) {

            statement.executeUpdate("""
                    UPDATE Solicitud_Ejecucion
                    SET estado = 'RESPONDIDA'
                    WHERE id_servicio =
                    """ + servicio.getIdServicio());
        }

        controller.retirarServicio(
                9001,
                servicio.getIdServicio());

        assertEquals(
                "RETIRADO",
                controller
                        .buscarServicio(
                                servicio.getIdServicio())
                        .getEstado());
    }

    @Test
    void proveedorNoPuedeModificarDisponibilidadesAjenas() {

        Servicio servicio = new Servicio();

        servicio.setIdProveedor(9001);
        servicio.setNombre("Servicio de prueba");
        servicio.setDescripcion("Servicio para comprobar permisos");
        servicio.setPrecio(500000);
        servicio.setCategoria("FOTOGRAFIA");
        servicio.setTipo("SERVICIO");
        servicio.setCiudadZona("Bogota");
        servicio.setAnticipacionMinima(5);
        servicio.setEstado("BORRADOR");

        controller.crearServicio(servicio);

        Date fecha = Date.from(
                LocalDate.now()
                        .plusDays(15)
                        .atStartOfDay(
                                ZoneId.systemDefault())
                        .toInstant());

        controller.registrarDisponibilidadServicio(
                9001,
                servicio.getIdServicio(),
                fecha,
                "09:00 - 12:00");

        int idDisponibilidad = controller
                .listarDisponibilidadesServicio(
                        servicio.getIdServicio())
                .get(0)
                .getIdDisponibilidad();

        // El proveedor 9002 no es dueño del servicio
        assertThrows(
                IllegalStateException.class,
                () -> controller.registrarDisponibilidadServicio(
                        9002,
                        servicio.getIdServicio(),
                        fecha,
                        "14:00 - 17:00"));

        assertThrows(
                IllegalStateException.class,
                () -> controller.eliminarDisponibilidad(
                        9002,
                        servicio.getIdServicio(),
                        idDisponibilidad));

        // El horario original debe seguir guardado
        assertEquals(
                1,
                controller.listarDisponibilidadesServicio(
                        servicio.getIdServicio()).size());
    }
}
