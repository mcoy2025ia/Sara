import controllers.ControllerInvitados;
import entities.Invitacion;
import entities.Invitado;
import entities.SesionUsuario;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryEvento;
import repositories.RepositoryInvitacion;
import repositories.RepositoryInvitado;
import repositories.RepositoryLugar;
import services.ServiceInvitados;
import static org.junit.jupiter.api.Assertions.*;

class ServiceInvitadosTest {
    private static final int EVENTO = DatosInvitacionesPrueba.EVENTO;
    private static final int OTRO_EVENTO = DatosInvitacionesPrueba.OTRO_EVENTO;
    private static final int GRANDE = DatosInvitacionesPrueba.LUGAR_GRANDE;
    private static final int PEQUENO = DatosInvitacionesPrueba.LUGAR_PEQUENO;

    @TempDir Path directorio;
    private String url;
    private ServiceInvitados service;
    private RepositoryInvitacion invitaciones;
    private RepositoryInvitado invitados;

    @BeforeEach
    void prepararBase() throws Exception {
        url = "jdbc:h2:" + directorio.resolve("invitados");
        DatosInvitacionesPrueba.preparar(url);
        invitaciones = new RepositoryInvitacion(url);
        invitados = new RepositoryInvitado(url);
        service = new ServiceInvitados(invitaciones, invitados, new RepositoryLugar(url), new RepositoryEvento(url));
    }

    // ---------------------------------------------------------------- organizador en sesión
    @Test
    void cadaOrganizadorSoloVeYGestionaLasInvitacionesDeSusEventos() {
        Invitacion propia = crear(EVENTO, GRANDE);
        service.agregarInvitado(propia.getIdInvitacion(), invitado("Ana", "ana@correo.com"));
        int ana = invitados.listarInvitados(propia.getIdInvitacion()).get(0).getIdInvitado();

        ControllerInvitados duenio = new ControllerInvitados(service, organizador(1));
        assertEquals(4, duenio.listarEventos().size());
        assertEquals(1, duenio.listarInvitaciones(EVENTO).size());

        ControllerInvitados otro = new ControllerInvitados(service, organizador(2));
        assertTrue(otro.listarEventos().isEmpty());
        assertAjeno(() -> otro.listarInvitaciones(EVENTO));
        assertAjeno(() -> otro.crearInvitacion(OTRO_EVENTO, invitacion(GRANDE, "Hola")));
        assertAjeno(() -> otro.actualizarInvitacion(propia.getIdInvitacion(), invitacion(PEQUENO, "Cambio")));
        assertAjeno(() -> otro.revocarInvitacion(propia.getIdInvitacion()));
        assertAjeno(() -> otro.agregarInvitado(propia.getIdInvitacion(), invitado("Luis", "luis@correo.com")));
        assertAjeno(() -> otro.quitarInvitado(ana));

        assertTrue(invitaciones.listarInvitaciones(OTRO_EVENTO).isEmpty());
        Invitacion guardada = invitaciones.buscarInvitacion(propia.getIdInvitacion());
        assertEquals(Invitacion.ESTADO_ENVIADA, guardada.getEstadoRespuesta());
        assertEquals(1, invitados.listarInvitados(propia.getIdInvitacion()).size());
    }

    private static SesionUsuario organizador(int idOrganizador) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setTipoUsuario("ORGANIZADOR");
        sesion.setIdActor(idOrganizador);
        return sesion;
    }

    private static void assertAjeno(org.junit.jupiter.api.function.Executable accion) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, accion);
        assertTrue(error.getMessage().contains("tus propios eventos"), error.getMessage());
    }

    // ---------------------------------------------------------------- crear
    @Test
    void controladorCreaInvitacionEnviadaConFechaDeEnvio() {
        Invitacion invitacion = invitacion(GRANDE, "  Te esperamos  ");
        new ControllerInvitados(service).crearInvitacion(EVENTO, invitacion);
        Invitacion guardada = invitaciones.buscarInvitacion(invitacion.getIdInvitacion());
        assertEquals(EVENTO, guardada.getIdEvento());
        assertEquals("Te esperamos", guardada.getMensaje());
        assertEquals(Invitacion.ESTADO_ENVIADA, guardada.getEstadoRespuesta());
        assertNotNull(guardada.getFechaEnvio());
    }

    @Test
    void rechazaCrearSinDatosValidosSinGuardar() {
        rechazarCrear(EVENTO, null, "Completa");
        rechazarCrear(99, invitacion(GRANDE, "Hola"), "evento registrado");
        rechazarCrear(DatosInvitacionesPrueba.EVENTO_CANCELADO, invitacion(GRANDE, "Hola"), "cancelado");
        rechazarCrear(EVENTO, invitacion(99, "Hola"), "lugar registrado");
        rechazarCrear(EVENTO, invitacion(GRANDE, "   "), "mensaje");
        rechazarCrear(EVENTO, invitacion(GRANDE, "x".repeat(501)), "500");
        assertTrue(invitaciones.listarInvitaciones(EVENTO).isEmpty());
    }

    @Test
    void aceptaMensajeEnElLimiteDelEsquema() {
        service.crearInvitacion(EVENTO, invitacion(GRANDE, "x".repeat(500)));
        assertEquals(1, invitaciones.listarInvitaciones(EVENTO).size());
    }

    // ---------------------------------------------------------------- actualizar y revocar
    @Test
    void actualizaMensajeYLugarConservandoEstadoYFecha() {
        Invitacion creada = crear(EVENTO, GRANDE);
        service.actualizarInvitacion(creada.getIdInvitacion(), invitacion(PEQUENO, "Nuevo mensaje"));
        Invitacion actualizada = invitaciones.buscarInvitacion(creada.getIdInvitacion());
        assertEquals("Nuevo mensaje", actualizada.getMensaje());
        assertEquals(PEQUENO, actualizada.getIdLugar());
        assertEquals(Invitacion.ESTADO_ENVIADA, actualizada.getEstadoRespuesta());
        assertEquals(creada.getFechaEnvio().withNano(0), actualizada.getFechaEnvio().withNano(0));
    }

    @Test
    void noPermiteCambiarAUnLugarSinAforoSuficiente() {
        Invitacion creada = crear(EVENTO, GRANDE);
        service.agregarInvitado(creada.getIdInvitacion(), invitado("Ana", "ana@correo.com"));
        service.agregarInvitado(creada.getIdInvitacion(), invitado("Luis", "luis@correo.com"));
        service.agregarInvitado(creada.getIdInvitacion(), invitado("Eva", "eva@correo.com"));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.actualizarInvitacion(creada.getIdInvitacion(), invitacion(PEQUENO, "Hola")));
        assertTrue(error.getMessage().contains("aforo"), error.getMessage());
        assertEquals(GRANDE, invitaciones.buscarInvitacion(creada.getIdInvitacion()).getIdLugar());
    }

    @Test
    void revocaInvitacionYNoPermiteEditarlaNiRevocarlaOtraVez() {
        Invitacion creada = crear(EVENTO, GRANDE);
        service.revocarInvitacion(creada.getIdInvitacion());
        assertEquals(Invitacion.ESTADO_REVOCADA, invitaciones.buscarInvitacion(creada.getIdInvitacion()).getEstadoRespuesta());
        assertMensaje(() -> service.revocarInvitacion(creada.getIdInvitacion()), "revocada");
        assertMensaje(() -> service.actualizarInvitacion(creada.getIdInvitacion(), invitacion(GRANDE, "Hola")), "revocada");
        assertMensaje(() -> service.agregarInvitado(creada.getIdInvitacion(), invitado("Ana", "ana@correo.com")), "revocada");
        assertMensaje(() -> service.revocarInvitacion(999), "no existe");
    }

    // ---------------------------------------------------------------- invitados
    @Test
    void agregaInvitadoPendienteConCorreoNormalizado() {
        Invitacion creada = crear(EVENTO, GRANDE);
        service.agregarInvitado(creada.getIdInvitacion(),
                new Invitado("  Ana Pérez ", " Ana@Correo.com ", "300 123 4567", " "));
        Invitado guardado = invitados.listarInvitados(creada.getIdInvitacion()).get(0);
        assertEquals("Ana Pérez", guardado.getNombre());
        assertEquals("ana@correo.com", guardado.getCorreo());
        assertEquals("300 123 4567", guardado.getTelefono());
        assertNull(guardado.getDireccion());
        assertEquals(Invitado.ESTADO_PENDIENTE, guardado.getEstado());
    }

    @Test
    void validaDatosDelInvitado() {
        int id = crear(EVENTO, GRANDE).getIdInvitacion();
        assertMensaje(() -> service.agregarInvitado(id, invitado(" ", "a@b.co")), "nombre");
        assertMensaje(() -> service.agregarInvitado(id, invitado("x".repeat(101), "a@b.co")), "nombre");
        assertMensaje(() -> service.agregarInvitado(id, invitado("Ana", "sin-arroba")), "correo");
        assertMensaje(() -> service.agregarInvitado(id, new Invitado("Ana", "a@b.co", "abc", null)), "teléfono");
        assertMensaje(() -> service.agregarInvitado(id, new Invitado("Ana", "a@b.co", null, "x".repeat(256))), "dirección");
        assertTrue(invitados.listarInvitados(id).isEmpty());
    }

    @Test
    void r40NoPermiteElMismoInvitadoEnDosInvitacionesActivasDelEvento() {
        int primera = crear(EVENTO, GRANDE).getIdInvitacion();
        int segunda = crear(EVENTO, GRANDE).getIdInvitacion();
        service.agregarInvitado(primera, invitado("Ana", "ana@correo.com"));
        assertMensaje(() -> service.agregarInvitado(segunda, invitado("Ana P", "ANA@correo.com")), "invitación activa");
        assertMensaje(() -> service.agregarInvitado(primera, invitado("Ana", "ana@correo.com")), "invitación activa");
        assertTrue(invitados.listarInvitados(segunda).isEmpty());
    }

    @Test
    void r40PermiteElMismoCorreoEnOtroEventoOSiLaAnteriorFueRevocada() {
        int primera = crear(EVENTO, GRANDE).getIdInvitacion();
        service.agregarInvitado(primera, invitado("Ana", "ana@correo.com"));
        int deOtroEvento = crear(OTRO_EVENTO, GRANDE).getIdInvitacion();
        service.agregarInvitado(deOtroEvento, invitado("Ana", "ana@correo.com"));

        service.revocarInvitacion(primera);
        int nueva = crear(EVENTO, GRANDE).getIdInvitacion();
        service.agregarInvitado(nueva, invitado("Ana", "ana@correo.com"));
        assertEquals(1, invitados.listarInvitados(nueva).size());
    }

    @Test
    void validaAforoContandoInvitadosYAcompanantes() throws Exception {
        int id = crear(EVENTO, PEQUENO).getIdInvitacion();
        service.agregarInvitado(id, invitado("Ana", "ana@correo.com"));
        int idAna = invitados.listarInvitados(id).get(0).getIdInvitado();
        DatosInvitacionesPrueba.agregarAcompanante(url, idAna);
        assertEquals(2, service.contarAsistentes(EVENTO, PEQUENO));
        assertMensaje(() -> service.agregarInvitado(id, invitado("Luis", "luis@correo.com")), "aforo");
        assertEquals(2, service.contarAsistentesPorInvitacion(EVENTO).get(id));
    }

    @Test
    void lasInvitacionesRevocadasLiberanAforo() {
        int id = crear(EVENTO, PEQUENO).getIdInvitacion();
        service.agregarInvitado(id, invitado("Ana", "ana@correo.com"));
        service.agregarInvitado(id, invitado("Luis", "luis@correo.com"));
        int otra = crear(EVENTO, PEQUENO).getIdInvitacion();
        assertMensaje(() -> service.agregarInvitado(otra, invitado("Eva", "eva@correo.com")), "aforo");
        service.revocarInvitacion(id);
        service.agregarInvitado(otra, invitado("Eva", "eva@correo.com"));
        assertEquals(1, service.contarAsistentes(EVENTO, PEQUENO));
    }

    @Test
    void quitaInvitadoSoloSiNoTieneRegistrosAsociados() throws Exception {
        int id = crear(EVENTO, GRANDE).getIdInvitacion();
        service.agregarInvitado(id, invitado("Ana", "ana@correo.com"));
        service.agregarInvitado(id, invitado("Luis", "luis@correo.com"));
        Invitado ana = invitados.listarInvitados(id).get(0);
        Invitado luis = invitados.listarInvitados(id).get(1);
        DatosInvitacionesPrueba.agregarAcompanante(url, ana.getIdInvitado());
        assertMensaje(() -> service.quitarInvitado(ana.getIdInvitado()), "acompañantes");
        service.quitarInvitado(luis.getIdInvitado());
        assertEquals(1, invitados.listarInvitados(id).size());
        assertMensaje(() -> service.quitarInvitado(999), "no existe");
    }

    @Test
    void enUnEventoCanceladoOFinalizadoNoSeModificanInvitacionesNiInvitados() throws Exception {
        for (String estado : new String[]{"CANCELADO", "FINALIZADO"}) {
            Invitacion inv = crear(OTRO_EVENTO, GRANDE);
            service.agregarInvitado(inv.getIdInvitacion(), invitado("Ana", estado.toLowerCase() + "@correo.com"));
            int ana = invitados.listarInvitados(inv.getIdInvitacion()).get(0).getIdInvitado();
            cambiarEstadoEvento(OTRO_EVENTO, estado);

            String motivo = estado.toLowerCase() + "; ya no se pueden modificar";
            assertMensaje(() -> service.actualizarInvitacion(inv.getIdInvitacion(), invitacion(GRANDE, "Nuevo texto")), motivo);
            assertMensaje(() -> service.revocarInvitacion(inv.getIdInvitacion()), motivo);
            assertMensaje(() -> service.agregarInvitado(inv.getIdInvitacion(), invitado("Luis", "luis@correo.com")), motivo);
            assertMensaje(() -> service.quitarInvitado(ana), motivo);

            assertEquals("Te invitamos", invitaciones.buscarInvitacion(inv.getIdInvitacion()).getMensaje());
            assertTrue(invitaciones.buscarInvitacion(inv.getIdInvitacion()).estaActiva());
            assertEquals(1, invitados.listarInvitados(inv.getIdInvitacion()).size());
            cambiarEstadoEvento(OTRO_EVENTO, "PLANEADO");
        }
    }

    // ---------------------------------------------------------------- utilidades
    private void cambiarEstadoEvento(int evento, String estado) throws Exception {
        try (java.sql.Connection c = config.DatabaseConnection.getConnection(url);
             java.sql.Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE Evento SET estado = '" + estado + "' WHERE id_evento = " + evento);
        }
    }

    private Invitacion invitacion(int lugar, String mensaje) {
        Invitacion invitacion = new Invitacion();
        invitacion.setIdLugar(lugar);
        invitacion.setMensaje(mensaje);
        return invitacion;
    }

    private Invitacion crear(int evento, int lugar) {
        Invitacion invitacion = invitacion(lugar, "Te invitamos");
        service.crearInvitacion(evento, invitacion);
        return invitacion;
    }

    private Invitado invitado(String nombre, String correo) {
        return new Invitado(nombre, correo, null, null);
    }

    private void rechazarCrear(int evento, Invitacion invitacion, String motivo) {
        assertMensaje(() -> service.crearInvitacion(evento, invitacion), motivo);
    }

    private void assertMensaje(org.junit.jupiter.api.function.Executable accion, String motivo) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, accion);
        assertTrue(error.getMessage().contains(motivo), error.getMessage());
    }
}
