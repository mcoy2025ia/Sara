import config.DatabaseConnection;
import controllers.ControllerInvitados;
import entities.Invitacion;
import entities.Invitado;
import entities.Evento;
import entities.Regalo;
import entities.SesionUsuario;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryEvento;
import repositories.RepositoryInvitacion;
import repositories.RepositoryInvitado;
import repositories.RepositoryLugar;
import repositories.RepositoryRegalo;
import repositories.RepositoryReservaRegalo;
import services.ServiceInvitados;
import static org.junit.jupiter.api.Assertions.*;

/** CU-20: el organizador arma la mesa de regalos y los invitados reservan sin duplicados (R43). */
class MesaRegalosTest {
    private static final int EVENTO = DatosInvitacionesPrueba.EVENTO;
    private static final int GRANDE = DatosInvitacionesPrueba.LUGAR_GRANDE;

    @TempDir Path directorio;
    private String url;
    private ServiceInvitados service;
    private ControllerInvitados controller;
    private int invitacion;
    private int ana;
    private int luis;

    @BeforeEach
    void prepararBase() throws Exception {
        url = "jdbc:h2:" + directorio.resolve("regalos");
        DatosInvitacionesPrueba.preparar(url);
        service = new ServiceInvitados(new RepositoryInvitacion(url), new RepositoryInvitado(url),
                new RepositoryLugar(url), new RepositoryEvento(url), new RepositoryRegalo(url),
                new RepositoryReservaRegalo(url));
        controller = new ControllerInvitados(service, organizador(DatosInvitacionesPrueba.ORGANIZADOR));
        invitacion = crearInvitacion(EVENTO);
        ana = agregarInvitado(invitacion, "Ana", "ana@correo.com", "300 111 2222");
        luis = agregarInvitado(invitacion, "Luis", "luis@correo.com", "310 999 8888");
    }

    // ---------------------------------------------------------------- organizador
    @Test
    void elOrganizadorAgregaRegalosDisponibles() {
        int cobija = agregarRegalo(invitacion, "  Cobija  ", "Cobija suave", "60000");
        List<Regalo> mesa = controller.listarRegalos(invitacion);
        assertEquals(1, mesa.size());
        Regalo regalo = mesa.get(0);
        assertEquals(cobija, regalo.getIdRegalo());
        assertEquals("Cobija", regalo.getNombre());
        assertEquals(Regalo.ESTADO_DISPONIBLE, regalo.getEstado());
        assertEquals(0, new BigDecimal("60000").compareTo(regalo.getPrecioEstimado()));
        assertFalse(regalo.estaReservado());
    }

    @Test
    void validaLosDatosDelRegalo() {
        assertMensaje(() -> agregarRegalo(invitacion, "  ", null, "1000"), "nombre");
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "0"), "mayor que cero");
        assertMensaje(() -> service.agregarRegalo(DatosInvitacionesPrueba.ORGANIZADOR, invitacion, new Regalo("Cobija", null, null)), "mayor que cero");
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", "x".repeat(256), "1000"), "255");
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "100000000000"), "demasiado alto");
        assertTrue(controller.listarRegalos(invitacion).isEmpty());
    }

    @Test
    void unPrecioQueSeRedondeariaACeroNoSeGuarda() {
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "0.001"), "dos decimales");
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "0.004"), "dos decimales");
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "1000.555"), "dos decimales");
        assertTrue(controller.listarRegalos(invitacion).isEmpty());

        agregarRegalo(invitacion, "Cobija", null, "0.01");
        agregarRegalo(invitacion, "Libro", null, "45000.50");
        List<Regalo> mesa = controller.listarRegalos(invitacion);
        assertEquals(0, new BigDecimal("0.01").compareTo(mesa.get(0).getPrecioEstimado()));
        assertEquals(0, new BigDecimal("45000.50").compareTo(mesa.get(1).getPrecioEstimado()));
    }

    @Test
    void soloElOrganizadorDelEventoConSesionGestionaLaMesa() {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");

        ControllerInvitados sinSesion = new ControllerInvitados(service);
        assertMensaje(() -> sinSesion.listarRegalos(invitacion), "Inicia sesión como organizador");
        assertMensaje(() -> sinSesion.agregarRegalo(invitacion, new Regalo("Libro", null, new BigDecimal("1000"))),
                "Inicia sesión como organizador");
        assertMensaje(() -> sinSesion.quitarRegalo(cobija), "Inicia sesión como organizador");

        ControllerInvitados otroOrganizador = new ControllerInvitados(service, organizador(99));
        assertMensaje(() -> otroOrganizador.listarRegalos(invitacion), "tus propios eventos");
        assertMensaje(() -> otroOrganizador.agregarRegalo(invitacion, new Regalo("Libro", null, new BigDecimal("1000"))),
                "tus propios eventos");
        assertMensaje(() -> otroOrganizador.quitarRegalo(cobija), "tus propios eventos");
        assertMensaje(() -> service.quitarRegalo(99, cobija), "tus propios eventos");

        List<Regalo> mesa = controller.listarRegalos(invitacion);
        assertEquals(1, mesa.size());
        assertEquals("Cobija", mesa.get(0).getNombre());
    }

    @Test
    void noSeModificaLaMesaDeUnaInvitacionRevocada() {
        service.revocarInvitacion(invitacion);
        assertMensaje(() -> agregarRegalo(invitacion, "Cobija", null, "1000"), "revocada");
    }

    @Test
    void permiteVariasUnidadesComoRegalosSeparados() {
        agregarRegalo(invitacion, "Pañitos", null, "25000");
        agregarRegalo(invitacion, "Pañitos", null, "25000");
        assertEquals(2, controller.listarRegalos(invitacion).size());
    }

    @Test
    void noSePuedeQuitarUnRegaloReservado() {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        reservar(ana, cobija);
        assertMensaje(() -> controller.quitarRegalo(cobija), "ya fue reservado");

        cancelar(ana, cobija);
        controller.quitarRegalo(cobija); // con la reserva cancelada sí se puede quitar
        assertTrue(controller.listarRegalos(invitacion).isEmpty());
    }

    // ---------------------------------------------------------------- invitado
    @Test
    void elInvitadoReservaUnRegaloYOtroNoPuedeReservarlo() throws Exception {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        assertTrue(controller.validarDisponibilidadRegalo(cobija));

        reservar(ana, cobija);

        assertFalse(controller.validarDisponibilidadRegalo(cobija));
        Regalo paraAna = mesa(ana).get(0);
        assertTrue(paraAna.estaReservadoPor(ana));
        Regalo paraLuis = mesa(luis).get(0);
        assertTrue(paraLuis.estaReservado());
        assertFalse(paraLuis.estaReservadoPor(luis));
        assertEquals("Ana", paraLuis.getReservadoPor());
        assertEquals(Regalo.ESTADO_RESERVADO, estadoEnTabla(cobija));

        assertMensaje(() -> reservar(luis, cobija), "otro invitado");
        assertMensaje(() -> reservar(ana, cobija), "Ya tienes reservado");
        assertEquals(1, contarReservasActivas(cobija));
    }

    @Test
    void cancelarLiberaElRegaloParaOtroInvitado() throws Exception {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        reservar(ana, cobija);
        assertMensaje(() -> cancelar(luis, cobija), "No tienes una reserva");

        cancelar(ana, cobija);
        assertTrue(controller.validarDisponibilidadRegalo(cobija));
        assertEquals(Regalo.ESTADO_DISPONIBLE, estadoEnTabla(cobija));

        reservar(luis, cobija);
        assertTrue(mesa(luis).get(0).estaReservadoPor(luis));
    }

    @Test
    void soloVeYReservaLosRegalosDeSuInvitacion() {
        int otraInvitacion = crearInvitacion(DatosInvitacionesPrueba.OTRO_EVENTO);
        int ajena = agregarRegalo(otraInvitacion, "Libro", null, "40000");
        agregarRegalo(invitacion, "Cobija", null, "60000");

        List<Regalo> mesa = mesa(ana);
        assertEquals(1, mesa.size());
        assertEquals("Cobija", mesa.get(0).getNombre());
        assertMensaje(() -> reservar(ana, ajena), "no pertenece");
    }

    @Test
    void noSeReservaEnUnEventoQueYaComenzo() throws Exception {
        int pasada = DatosInvitacionesPrueba.insertarInvitacion(url, DatosInvitacionesPrueba.EVENTO_PASADO, GRANDE);
        int pedro = insertarInvitado(pasada, "pedro@correo.com");
        int regalo = insertarRegalo(pasada, "Reloj");
        assertMensaje(() -> reservar(pedro, regalo), "ya comenzó");
    }

    @Test
    void conReservasDuplicadasAntiguasCadaInvitadoVeLaSuya() throws Exception {
        // Como en seed.sql: el mismo regalo quedó reservado por dos invitados.
        int panitos = agregarRegalo(invitacion, "Pañitos", null, "25000");
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO Reserva_regalo (id_evento, id_invitado, id_regalo, fecha_reserva, estado) VALUES "
                    + "(" + EVENTO + ", " + ana + ", " + panitos + ", CURRENT_TIMESTAMP, 'RESERVADO'), "
                    + "(" + EVENTO + ", " + luis + ", " + panitos + ", CURRENT_TIMESTAMP, 'RESERVADO')");
        }
        assertTrue(mesa(ana).get(0).estaReservadoPor(ana));
        assertTrue(mesa(luis).get(0).estaReservadoPor(luis));

        cancelar(ana, panitos);
        assertFalse(controller.validarDisponibilidadRegalo(panitos)); // Luis todavía lo tiene
        cancelar(luis, panitos);
        assertTrue(controller.validarDisponibilidadRegalo(panitos));
    }

    @Test
    void validaQueElInvitadoYElRegaloExistan() {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        assertMensaje(() -> controller.reservarRegalo(999, "x@correo.com", "3000000000", cobija), "invitado no existe");
        assertMensaje(() -> reservar(ana, 999), "regalo no existe");
        assertMensaje(() -> controller.consultarRegalosDisponibles(999, "x@correo.com", "3000000000"), "invitado no existe");
    }

    // ---------------------------------------------------------------- permisos del invitado
    @Test
    void nadieVeNiReservaEnLaMesaDeOtroInvitado() throws Exception {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        String msg = "Solo el invitado";

        // Luis conoce el id y el correo de Ana, pero no su teléfono.
        assertMensaje(() -> controller.consultarRegalosDisponibles(ana, "ana@correo.com", "310 999 8888"), msg);
        assertMensaje(() -> controller.reservarRegalo(ana, "luis@correo.com", "310 999 8888", cobija), msg);
        assertMensaje(() -> controller.reservarRegalo(ana, "ana@correo.com", "3109998888", cobija), msg);
        assertEquals(0, contarReservasActivas(cobija));

        // Ana reserva con sus datos; Luis no puede cancelarle la reserva haciéndose pasar por ella.
        controller.reservarRegalo(ana, "ANA@correo.com", "(300) 111-2222", cobija);
        assertMensaje(() -> controller.cancelarReservaRegalo(ana, "ana@correo.com", "310 999 8888", cobija), msg);
        assertEquals(1, contarReservasActivas(cobija));
        assertTrue(controller.consultarRegalosDisponibles(ana, "ana@correo.com", "3001112222").get(0).estaReservadoPor(ana));
    }

    @Test
    void siElEventoSeCancelaAntesDeEscribirLaReservaNoQuedaActiva() throws Exception {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        // El servicio valida con una foto del evento tomada antes de la cancelación...
        List<Evento> antesDeCancelar = new RepositoryEvento(url).listarEventos();
        RepositoryEvento eventoDesactualizado = new RepositoryEvento(url) {
            @Override
            public List<Evento> listarEventos() {
                return antesDeCancelar;
            }
        };
        ServiceInvitados conCarrera = new ServiceInvitados(new RepositoryInvitacion(url), new RepositoryInvitado(url),
                new RepositoryLugar(url), eventoDesactualizado, new RepositoryRegalo(url), new RepositoryReservaRegalo(url));
        // ...y el evento se cancela entre esa validación y la escritura.
        ejecutar("UPDATE Evento SET estado = 'CANCELADO' WHERE id_evento = " + EVENTO);

        Invitado a = new RepositoryInvitado(url).buscarInvitado(ana);
        assertMensaje(() -> conCarrera.reservarRegalo(ana, a.getCorreo(), a.getTelefono(), cobija), "cancelado");
        assertEquals(0, contarReservasActivas(cobija));
        assertEquals(Regalo.ESTADO_DISPONIBLE, estadoEnTabla(cobija));
    }

    @Test
    void siLaInvitacionSeRevocaAntesDeEscribirLaReservaNoQuedaActiva() throws Exception {
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        Invitado a = new RepositoryInvitado(url).buscarInvitado(ana);
        // La invitación se revoca directamente en la BD, después de lo que el servicio ya validó.
        RepositoryReservaRegalo reservas = new RepositoryReservaRegalo(url);
        ejecutar("UPDATE Invitacion SET estado_respuesta = 'REVOCADA' WHERE id_invitacion = " + invitacion);
        assertMensaje(() -> reservas.registrarReserva(new entities.ReservaRegalo(0, EVENTO, ana, cobija,
                java.time.LocalDateTime.now(), entities.ReservaRegalo.ESTADO_RESERVADO)), "revocada");
        assertEquals(0, contarReservasActivas(cobija));
    }

    @Test
    void sinTelefonoRegistradoNoSeUsaLaMesa() {
        int sinTelefono = agregarInvitado(invitacion, "Eva", "eva@correo.com", null);
        int cobija = agregarRegalo(invitacion, "Cobija", null, "60000");
        assertMensaje(() -> controller.consultarRegalosDisponibles(sinTelefono, "eva@correo.com", "3001112222"), "Solo el invitado");
        assertMensaje(() -> controller.reservarRegalo(sinTelefono, "eva@correo.com", "", cobija), "Solo el invitado");
    }

    // ---------------------------------------------------------------- utilidades
    /** Acciones hechas por el propio invitado, con su correo y teléfono registrados. */
    private List<Regalo> mesa(int idInvitado) {
        Invitado i = new RepositoryInvitado(url).buscarInvitado(idInvitado);
        return controller.consultarRegalosDisponibles(idInvitado, i.getCorreo(), i.getTelefono());
    }

    private void reservar(int idInvitado, int idRegalo) {
        Invitado i = new RepositoryInvitado(url).buscarInvitado(idInvitado);
        controller.reservarRegalo(idInvitado, i.getCorreo(), i.getTelefono(), idRegalo);
    }

    private void cancelar(int idInvitado, int idRegalo) {
        Invitado i = new RepositoryInvitado(url).buscarInvitado(idInvitado);
        controller.cancelarReservaRegalo(idInvitado, i.getCorreo(), i.getTelefono(), idRegalo);
    }

    private static SesionUsuario organizador(int idOrganizador) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setTipoUsuario("ORGANIZADOR");
        sesion.setIdActor(idOrganizador);
        return sesion;
    }

    private int crearInvitacion(int evento) {
        Invitacion datos = new Invitacion();
        datos.setIdLugar(GRANDE);
        datos.setMensaje("Te invitamos");
        service.crearInvitacion(evento, datos);
        return datos.getIdInvitacion();
    }

    private int agregarInvitado(int idInvitacion, String nombre, String correo, String telefono) {
        Invitado invitado = new Invitado(nombre, correo, telefono, null);
        service.agregarInvitado(idInvitacion, invitado);
        return invitado.getIdInvitado();
    }

    private int agregarRegalo(int idInvitacion, String nombre, String descripcion, String precio) {
        Regalo regalo = new Regalo(nombre, descripcion, new BigDecimal(precio));
        controller.agregarRegalo(idInvitacion, regalo);
        return regalo.getIdRegalo();
    }

    private int insertarInvitado(int idInvitacion, String correo) throws Exception {
        return insertar("INSERT INTO Invitado (id_invitacion, nombre, telefono, estado, correo) VALUES ("
                + idInvitacion + ", 'Pedro', '3005550000', 'PENDIENTE', '" + correo + "')");
    }

    private int insertarRegalo(int idInvitacion, String nombre) throws Exception {
        return insertar("INSERT INTO Regalo (id_invitacion, nombre, estado, precio_estimado) VALUES ("
                + idInvitacion + ", '" + nombre + "', 'DISPONIBLE', 10000)");
    }

    private void ejecutar(String sql) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        }
    }

    private int insertar(String sql) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement()) {
            s.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = s.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private String estadoEnTabla(int idRegalo) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT estado FROM Regalo WHERE id_regalo = " + idRegalo)) {
            r.next();
            return r.getString(1);
        }
    }

    private int contarReservasActivas(int idRegalo) throws Exception {
        try (Connection c = DatabaseConnection.getConnection(url); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM Reserva_regalo WHERE estado = 'RESERVADO' AND id_regalo = " + idRegalo)) {
            r.next();
            return r.getInt(1);
        }
    }

    private void assertMensaje(org.junit.jupiter.api.function.Executable accion, String motivo) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, accion);
        assertTrue(error.getMessage().contains(motivo), error.getMessage());
    }
}
