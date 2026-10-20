
import config.DatabaseConnection;
import entities.ImagenServicio;
import entities.PreguntaServicio;
import entities.Servicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.RepositoryDisponibilidadServicio;
import repositories.RepositoryImagenServicio;
import repositories.RepositoryPreguntaServicio;
import repositories.RepositoryServicio;
import services.ServiceProveedores;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PublicacionServicioH2Test {

    private ServiceProveedores service;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty(
                "nexteventux.db.url",
                "jdbc:h2:mem:cu11publicacion;DB_CLOSE_DELAY=-1");

        try (Connection connection
                = DatabaseConnection.getConnection(); Statement statement
                = connection.createStatement()) {

            statement.execute(
                    "SET REFERENTIAL_INTEGRITY FALSE");

            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre, empresa,
                     correo, telefono, direccion, estado)
                    VALUES
                    (9001, 9001, 'Proveedor prueba',
                     'Empresa prueba', 'proveedor@prueba.com',
                     '3000000000', 'Bogota', 'APROBADO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa,
                     telefono, estado)
                    VALUES
                    (9002, 9002, 'Organizador prueba',
                     '3000000001', 'ACTIVO')
                    """);
        }

        service = new ServiceProveedores(
                new RepositoryServicio(),
                new RepositoryDisponibilidadServicio(),
                new RepositoryImagenServicio(),
                new RepositoryPreguntaServicio());
    }

    @AfterEach
    void limpiar() throws Exception {

        try (Connection connection
                = DatabaseConnection.getConnection(); Statement statement
                = connection.createStatement()) {

            statement.execute("DROP ALL OBJECTS");
        }

        System.clearProperty(
                "nexteventux.db.url");
    }

    @Test
    void debeGuardarImagenPreguntaYRespuesta() {

        Servicio servicio = new Servicio();

        servicio.setIdProveedor(9001);
        servicio.setNombre("Servicio CU11");
        servicio.setDescripcion("Servicio de prueba");
        servicio.setPrecio(500000);
        servicio.setCategoria("PRUEBA");
        servicio.setTipo("Servicio");
        servicio.setCiudadZona("Bogota");
        servicio.setAnticipacionMinima(2);
        servicio.setEstado("BORRADOR");

        service.crearServicio(servicio);

        Date fecha = Date.from(
                LocalDate.now()
                        .plusDays(10)
                        .atStartOfDay(
                                ZoneId.systemDefault())
                        .toInstant());

        service.registrarDisponibilidadServicio(
                9001,
                servicio.getIdServicio(),
                fecha,
                "09:00 - 12:00");

        service.agregarImagenServicio(
                9001,
                servicio.getIdServicio(),
                "C:/imagenes/referencia.jpg");

        List<ImagenServicio> imagenes
                = service.listarImagenesServicio(
                        servicio.getIdServicio());

        assertEquals(1, imagenes.size());
        assertEquals(
                "C:/imagenes/referencia.jpg",
                imagenes.get(0).getRuta());

        service.publicarServicio(
                9001,
                servicio.getIdServicio());

        service.formularPreguntaPublica(
                servicio.getIdServicio(),
                9002,
                "¿Incluye montaje?");

        List<PreguntaServicio> preguntas
                = service.listarPreguntasPublicas(
                        servicio.getIdServicio());

        assertEquals(1, preguntas.size());
        assertEquals(
                "PENDIENTE",
                preguntas.get(0).getEstado());

        service.responderPreguntaPublica(
                9001,
                preguntas.get(0).getIdPregunta(),
                "Si, el montaje esta incluido.");

        preguntas
                = service.listarPreguntasPublicas(
                        servicio.getIdServicio());

        assertEquals(
                "RESPONDIDA",
                preguntas.get(0).getEstado());

        assertEquals(
                "Si, el montaje esta incluido.",
                preguntas.get(0).getRespuesta());
    }
}
