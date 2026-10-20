import controllers.ControllerInvitados;
import entities.Invitacion;
import entities.InvitacionRecibida;
import entities.Invitado;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryEvento;
import repositories.RepositoryInvitacion;
import repositories.RepositoryInvitado;
import repositories.RepositoryLugar;
import services.ServiceInvitados;
import static org.junit.jupiter.api.Assertions.*;

/** CU-16: el invitado consulta sus invitaciones y responde si asistirá. */
class RespuestaInvitacionTest {
    private static final int EVENTO = DatosInvitacionesPrueba.EVENTO;
    private static final int GRANDE = DatosInvitacionesPrueba.LUGAR_GRANDE;
    private static final int PEQUENO = DatosInvitacionesPrueba.LUGAR_PEQUENO;
    private static final String TEL = "300 111 2222";

    @TempDir Path directorio;
    private String url;
    private ServiceInvitados service;
    private RepositoryInvitacion invitaciones;
    private RepositoryInvitado invitados;

    @BeforeEach
    void prepararBase() throws Exception {
        url = "jdbc:h2:" + directorio.resolve("respuestas");
        DatosInvitacionesPrueba.preparar(url);
        invitaciones = new RepositoryInvitacion(url);
        invitados = new RepositoryInvitado(url);
        service = new ServiceInvitados(invitaciones, invitados, new RepositoryLugar(url), new RepositoryEvento(url));
    }

    // ---------------------------------------------------------------- R37 consultar
    @Test
    void consultaLasInvitacionesActivasDelCorreoSinImportarMayusculas() {
        int primera = crear(EVENTO, GRANDE);
        int segunda = crear(DatosInvitacionesPrueba.OTRO_EVENTO, GRANDE);
        agregar(primera, "Ana", "ana@correo.com");
        agregar(segunda, "Ana", "ana@correo.com");
        agregar(primera, "Luis", "luis@correo.com");

        List<InvitacionRecibida> encontradas = new ControllerInvitados(service).consultarInvitacionesPorCorreo("  ANA@correo.com ", TEL);
        assertEquals(2, encontradas.size());
        InvitacionRecibida boda = encontradas.get(0);
        assertEquals("Boda", boda.getEvento());
        assertEquals("Salón Grande", boda.getLugar());
        assertEquals(100, boda.getCapacidad());
        assertEquals(2, boda.getOcupados());
        assertEquals(Invitado.ESTADO_PENDIENTE, boda.getEstadoInvitado());
    }

    @Test
    void noMuestraInvitacionesRevocadasYValidaElCorreo() {
        int id = crear(EVENTO, GRANDE);
        agregar(id, "Ana", "ana@correo.com");
        service.revocarInvitacion(id);
        assertTrue(service.consultarInvitacionesPorCorreo("ana@correo.com", TEL).isEmpty());
        assertMensaje(() -> service.consultarInvitacionesPorCorreo("sin-arroba", TEL), "correo válido");
        assertMensaje(() -> service.consultarInvitacionesPorCorreo(" ", TEL), "correo válido");
    }

    // ---------------------------------------------------------------- R38 responder
    @Test
    void confirmarMarcaAlInvitadoYDejaLaInvitacionAceptada() {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com");
        agregar(id, "Luis", "luis@correo.com");
        new ControllerInvitados(service).gestionarRespuestaInvitacion(ana, "ana@correo.com", "300-111-2222", "confirmado");
        assertEquals(Invitado.ESTADO_CONFIRMADO, invitados.buscarInvitado(ana).getEstado());
        assertEquals(Invitacion.ESTADO_ACEPTADA, invitaciones.buscarInvitacion(id).getEstadoRespuesta());
    }

    @Test
    void laInvitacionQuedaRechazadaSoloCuandoTodosRechazan() {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com");
        int luis = agregar(id, "Luis", "luis@correo.com");
        responder(ana, Invitado.ESTADO_RECHAZADO);
        assertEquals(Invitacion.ESTADO_ENVIADA, invitaciones.buscarInvitacion(id).getEstadoRespuesta());
        responder(luis, Invitado.ESTADO_RECHAZADO);
        assertEquals(Invitacion.ESTADO_RECHAZADA, invitaciones.buscarInvitacion(id).getEstadoRespuesta());
    }

    @Test
    void validaLaRespuestaYNoRepiteLaMisma() {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com");
        assertMensaje(() -> responder(ana, "QUIZAS"), "Elige");
        assertMensaje(() -> responder(ana, null), "Elige");
        assertMensaje(() -> service.gestionarRespuestaInvitacion(999, "x@correo.com", TEL, Invitado.ESTADO_CONFIRMADO), "no existe");
        responder(ana, Invitado.ESTADO_CONFIRMADO);
        assertMensaje(() -> responder(ana, Invitado.ESTADO_CONFIRMADO), "Ya habías confirmado");
    }

    @Test
    void noPermiteResponderInvitacionesRevocadasNiEventosPasados() throws Exception {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com");
        service.revocarInvitacion(id);
        assertMensaje(() -> responder(ana, Invitado.ESTADO_CONFIRMADO), "revocada");

        int pasada = DatosInvitacionesPrueba.insertarInvitacion(url, DatosInvitacionesPrueba.EVENTO_PASADO, GRANDE);
        Invitado luis = new Invitado(0, pasada, "Luis", TEL, null, Invitado.ESTADO_PENDIENTE, "luis@correo.com");
        invitados.guardarInvitado(luis);
        assertMensaje(() -> responder(luis.getIdInvitado(), Invitado.ESTADO_CONFIRMADO), "ya comenzó");
        assertEquals(Invitado.ESTADO_PENDIENTE, invitados.buscarInvitado(luis.getIdInvitado()).getEstado());
    }

    // ---------------------------------------------------------------- R39 cupos
    @Test
    void rechazarLiberaElCupoDelInvitadoYSusAcompanantes() throws Exception {
        int id = crear(EVENTO, PEQUENO);
        int ana = agregar(id, "Ana", "ana@correo.com");
        DatosInvitacionesPrueba.agregarAcompanante(url, ana);
        assertEquals(2, service.contarAsistentes(EVENTO, PEQUENO));
        assertMensaje(() -> agregar(id, "Luis", "luis@correo.com"), "aforo");

        responder(ana, Invitado.ESTADO_RECHAZADO);
        assertEquals(0, service.contarAsistentes(EVENTO, PEQUENO));
        agregar(id, "Luis", "luis@correo.com");
        assertEquals(1, service.contarAsistentes(EVENTO, PEQUENO));
    }

    @Test
    void volverAConfirmarDespuesDeRechazarExigeCupo() throws Exception {
        int id = crear(EVENTO, PEQUENO);
        int ana = agregar(id, "Ana", "ana@correo.com");
        DatosInvitacionesPrueba.agregarAcompanante(url, ana);
        responder(ana, Invitado.ESTADO_RECHAZADO);
        agregar(id, "Luis", "luis@correo.com");
        // Quedan 1 de 2 cupos: Ana necesita 2 (ella y su acompañante).
        assertMensaje(() -> responder(ana, Invitado.ESTADO_CONFIRMADO), "cupo");
        assertEquals(Invitado.ESTADO_RECHAZADO, invitados.buscarInvitado(ana).getEstado());

        int luis = invitados.listarInvitados(id).get(1).getIdInvitado();
        responder(luis, Invitado.ESTADO_RECHAZADO);
        responder(ana, Invitado.ESTADO_CONFIRMADO);
        assertEquals(Invitado.ESTADO_CONFIRMADO, invitados.buscarInvitado(ana).getEstado());
    }

    // ---------------------------------------------------------------- permisos del invitado
    @Test
    void nadiePuedeConsultarNiResponderPorOtroInvitado() {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com", "300 111 2222");
        int luis = agregar(id, "Luis", "luis@correo.com", "310 999 8888");

        // Luis conoce el correo de Ana, pero no su teléfono: no ve sus invitaciones.
        assertTrue(service.consultarInvitacionesPorCorreo("ana@correo.com", "310 999 8888").isEmpty());
        assertEquals(1, service.consultarInvitacionesPorCorreo("ana@correo.com", "3001112222").size());

        // Tampoco puede responder por ella, ni con sus propios datos ni con un teléfono equivocado.
        assertMensaje(() -> service.gestionarRespuestaInvitacion(ana, "luis@correo.com", "310 999 8888",
                Invitado.ESTADO_RECHAZADO), "Solo el invitado");
        assertMensaje(() -> service.gestionarRespuestaInvitacion(ana, "ana@correo.com", "310 999 8888",
                Invitado.ESTADO_RECHAZADO), "Solo el invitado");
        assertEquals(Invitado.ESTADO_PENDIENTE, invitados.buscarInvitado(ana).getEstado());

        // Cada uno sí responde la suya.
        service.gestionarRespuestaInvitacion(ana, "ANA@correo.com", "(300) 111-2222", Invitado.ESTADO_CONFIRMADO);
        service.gestionarRespuestaInvitacion(luis, "luis@correo.com", "3109998888", Invitado.ESTADO_RECHAZADO);
        assertEquals(Invitado.ESTADO_CONFIRMADO, invitados.buscarInvitado(ana).getEstado());
        assertEquals(Invitado.ESTADO_RECHAZADO, invitados.buscarInvitado(luis).getEstado());
    }

    @Test
    void sinTelefonoRegistradoNoSePuedeVerificarAlInvitado() {
        int id = crear(EVENTO, GRANDE);
        int ana = agregar(id, "Ana", "ana@correo.com", null);
        assertMensaje(() -> service.consultarInvitacionesPorCorreo("ana@correo.com", " "), "teléfono");
        assertTrue(service.consultarInvitacionesPorCorreo("ana@correo.com", "3001112222").isEmpty());
        assertMensaje(() -> service.gestionarRespuestaInvitacion(ana, "ana@correo.com", "3001112222",
                Invitado.ESTADO_CONFIRMADO), "Solo el invitado");
    }

    // ---------------------------------------------------------------- utilidades
    /** Responde como el propio invitado, con su correo y teléfono registrados. */
    private void responder(int idInvitado, String respuesta) {
        Invitado invitado = invitados.buscarInvitado(idInvitado);
        service.gestionarRespuestaInvitacion(idInvitado, invitado.getCorreo(), invitado.getTelefono(), respuesta);
    }

    private int crear(int evento, int lugar) {
        Invitacion invitacion = new Invitacion();
        invitacion.setIdLugar(lugar);
        invitacion.setMensaje("Te invitamos");
        service.crearInvitacion(evento, invitacion);
        return invitacion.getIdInvitacion();
    }

    private int agregar(int idInvitacion, String nombre, String correo) {
        return agregar(idInvitacion, nombre, correo, TEL);
    }

    private int agregar(int idInvitacion, String nombre, String correo, String telefono) {
        Invitado invitado = new Invitado(nombre, correo, telefono, null);
        service.agregarInvitado(idInvitacion, invitado);
        return invitado.getIdInvitado();
    }

    private void assertMensaje(org.junit.jupiter.api.function.Executable accion, String motivo) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, accion);
        assertTrue(error.getMessage().contains(motivo), error.getMessage());
    }
}
