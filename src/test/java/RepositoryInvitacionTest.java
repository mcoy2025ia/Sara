import entities.Invitacion;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryInvitacion;
import static org.junit.jupiter.api.Assertions.*;

/** Reemplaza la prueba manual TestRepositoryInvitados con JUnit y una base aislada. */
class RepositoryInvitacionTest {
    @TempDir Path directorio;
    private RepositoryInvitacion repository;

    @BeforeEach
    void prepararBase() throws Exception {
        String url = "jdbc:h2:" + directorio.resolve("invitaciones");
        DatosInvitacionesPrueba.preparar(url);
        repository = new RepositoryInvitacion(url);
    }

    @Test
    void creaListaBuscaActualizaYEliminaInvitaciones() {
        LocalDateTime envio = LocalDateTime.of(2026, 10, 20, 10, 0);
        Invitacion invitacion = new Invitacion(DatosInvitacionesPrueba.EVENTO, DatosInvitacionesPrueba.LUGAR_GRANDE,
                envio, "Prueba CU17 Repository", Invitacion.ESTADO_ENVIADA);
        repository.guardarInvitacion(invitacion);
        assertTrue(invitacion.getIdInvitacion() > 0);

        List<Invitacion> invitaciones = repository.listarInvitaciones(DatosInvitacionesPrueba.EVENTO);
        assertEquals(1, invitaciones.size());
        assertTrue(repository.listarInvitaciones(DatosInvitacionesPrueba.OTRO_EVENTO).isEmpty());

        Invitacion encontrada = repository.buscarInvitacion(invitacion.getIdInvitacion());
        assertEquals("Prueba CU17 Repository", encontrada.getMensaje());
        assertEquals(envio, encontrada.getFechaEnvio());

        encontrada.setMensaje("Invitacion actualizada CU17");
        encontrada.setEstadoRespuesta(Invitacion.ESTADO_ACEPTADA);
        repository.actualizarInvitacion(encontrada);
        Invitacion actualizada = repository.buscarInvitacion(invitacion.getIdInvitacion());
        assertEquals("Invitacion actualizada CU17", actualizada.getMensaje());
        assertEquals(Invitacion.ESTADO_ACEPTADA, actualizada.getEstadoRespuesta());

        repository.eliminarInvitacion(invitacion.getIdInvitacion());
        assertNull(repository.buscarInvitacion(invitacion.getIdInvitacion()));
    }

    @Test
    void comunicaFalloDeConexion() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new RepositoryInvitacion("jdbc:desconocido:prueba").listarInvitaciones(1));
        assertTrue(error.getMessage().contains("No se pudieron consultar"));
    }
}
