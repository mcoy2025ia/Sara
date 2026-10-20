import config.*;
import entities.*;
import org.junit.jupiter.api.*;
import repositories.*;
import services.*;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class IntegracionCancelacionModulosTest {
    private String url, anterior;
    private ServiceEventos eventos;
    private ServiceInvitados invitados;
    private ServiceAvisosProveedor avisos;
    private ServiceAutenticacion login;
    private SesionUsuario organizador;
    private RepositoryDisponibilidadServicio disponibilidades;

    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:mem:integracion_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";
        anterior=System.getProperty("nexteventux.db.url"); System.setProperty("nexteventux.db.url",url);
        try(Connection c=DatabaseConnection.getConnection(url)) {CargarDatosEjemplo.cargar(c);}
        disponibilidades=new RepositoryDisponibilidadServicio();
        disponibilidad(1); disponibilidad(3);
        // Fixture de contratación: el productor de CU12/CU13 todavía debe integrarse.
        sql("UPDATE Disponibilidad_Servicio SET estado='RESERVADO'");
        sql("UPDATE Contratacion SET fecha_servicio=DATE '2026-11-15',hora_inicio=TIME '15:00:00',hora_fin=TIME '23:00:00'");
        sql("UPDATE Contratacion SET estado='CONFIRMADA' WHERE id_contratacion=1");
        sql("UPDATE Evento SET id_lugar=id_evento"); sql("UPDATE Lugar SET estado='RESERVADO'");
        eventos=new ServiceEventos(new RepositoryEvento(url),new RepositoryCancelacion(url));
        invitados=new ServiceInvitados(new RepositoryInvitacion(url),new RepositoryInvitado(url),new RepositoryLugar(url),new RepositoryEvento(url));
        avisos=new ServiceAvisosProveedor(new RepositoryAvisosProveedor(url));
        login=new ServiceAutenticacion(new RepositoryAutenticacion(url));
        organizador=login.iniciarSesion("laura.gomez","clave123");
    }

    @AfterEach void limpiar() throws Exception {
        if(anterior==null) System.clearProperty("nexteventux.db.url"); else System.setProperty("nexteventux.db.url",anterior);
        try(Connection c=DriverManager.getConnection(url,"sa","");Statement s=c.createStatement()) {s.execute("SHUTDOWN");}
    }

    @Test void cancelaConLosHorariosDeCu11YLosModulosLeenElResultado() {
        assertEquals(0,disponibilidades.contarDisponibilidades(1));
        eventos.cancelarEvento(1,organizador,true);
        assertEquals("CANCELADA",valor("SELECT estado FROM Contratacion WHERE id_contratacion=1"));
        assertNotNull(valor("SELECT id_disponibilidad FROM Contratacion WHERE id_contratacion=1"));
        assertEquals(1,disponibilidades.contarDisponibilidades(1));
        assertEquals(1,avisos.listar(login.iniciarSesion("decoraciones.arcoiris","clave789")).size());
        assertEquals(1,avisos.listar(login.iniciarSesion("sabores.eventos","clave321")).size());
        assertTrue(invitados.consultarInvitacionesPorCorreo("sofia@email.com","3005551111").isEmpty());
        assertEquals(1,invitados.consultarAvisosPorCorreo(" SOFIA@email.com ").size());
        assertThrows(IllegalArgumentException.class,()->invitados.gestionarRespuestaInvitacion(1,"sofia@email.com","3005551111","CONFIRMADO"));
    }

    @Test void noVinculaUnaReservaSiElHorarioNoCoincideYRevierteLosDemasVinculos() throws Exception {
        sql("UPDATE Contratacion SET hora_fin=TIME '22:00:00' WHERE id_contratacion=2");
        assertThrows(IllegalArgumentException.class,()->eventos.cancelarEvento(1,organizador,true));
        assertNull(valor("SELECT id_disponibilidad FROM Contratacion WHERE id_contratacion=1"));
        assertEquals("PLANEADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
        assertEquals(0,disponibilidades.contarDisponibilidades(1));
        assertTrue(invitados.consultarAvisosPorCorreo("sofia@email.com").isEmpty());
    }

    @Test void rechazaUnVinculoExplicitoConServicioDistinto() throws Exception {
        sql("UPDATE Contratacion SET id_disponibilidad=(SELECT id_disponibilidad FROM Disponibilidad_Servicio WHERE id_servicio=3) WHERE id_contratacion=1");
        assertThrows(IllegalArgumentException.class,()->eventos.cancelarEvento(1,organizador,true));
        assertEquals("PLANEADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
    }

    @Test void unaContratacionConfirmadaDeOtroEventoMantieneLaReserva() throws Exception {
        sql("INSERT INTO Contratacion (id_propuesta,id_evento,fecha_contratacion,estado,id_disponibilidad) "
                +"SELECT 3,2,CURRENT_TIMESTAMP,'CONFIRMADA',id_disponibilidad FROM Disponibilidad_Servicio WHERE id_servicio=1");
        eventos.cancelarEvento(1,organizador,true);
        assertEquals(0,disponibilidades.contarDisponibilidades(1));
    }

    @Test void conservaLaReservaDeOtraContratacionActivaSinVinculoDirecto() throws Exception {
        contratarSinVinculo("ACTIVA", "2026-11-15");
        eventos.cancelarEvento(1,organizador,true);
        assertEquals(0,disponibilidades.contarDisponibilidades(1));
        assertNull(valor("SELECT id_disponibilidad FROM Contratacion WHERE id_evento=2"));
        assertEquals("ACTIVA",valor("SELECT estado FROM Contratacion WHERE id_evento=2"));
    }

    @Test void liberaLaReservaAlCancelarLaUltimaContratacionConfirmadaSinVinculo() throws Exception {
        contratarSinVinculo("CONFIRMADA", "2026-11-15");
        eventos.cancelarEvento(1,organizador,true);
        assertEquals(0,disponibilidades.contarDisponibilidades(1));
        eventos.cancelarEvento(2,login.iniciarSesion("carlos.rodriguez","clave456"),true);
        assertEquals(1,disponibilidades.contarDisponibilidades(1));
        assertEquals("CANCELADA",valor("SELECT estado FROM Contratacion WHERE id_evento=2"));
    }

    @Test void unaContratacionDeOtraFechaNoRetieneLaReservaCancelada() throws Exception {
        contratarSinVinculo("CONFIRMADA", "2026-11-16");
        eventos.cancelarEvento(1,organizador,true);
        assertEquals(1,disponibilidades.contarDisponibilidades(1));
        assertEquals("CONFIRMADA",valor("SELECT estado FROM Contratacion WHERE id_evento=2"));
    }

    @Test void avisosDelProveedorSeFiltranPorSuCuentaYSeBloqueanAlSalir() throws Exception {
        eventos.cancelarEvento(1,organizador,true);
        SesionUsuario proveedor=login.iniciarSesion("decoraciones.arcoiris","clave789");
        assertEquals(1,avisos.listar(proveedor).size());
        assertTrue(avisos.listar(login.iniciarSesion("luna","1234")).isEmpty());
        assertThrows(IllegalStateException.class,()->avisos.listar(organizador));
        sql("UPDATE Proveedor SET estado='SUSPENDIDO' WHERE id_proveedor=1");
        assertThrows(IllegalStateException.class,()->avisos.listar(proveedor));
        proveedor.cerrar(); assertThrows(IllegalStateException.class,()->avisos.listar(proveedor));
        assertThrows(IllegalStateException.class,()->avisos.listar(null));
    }

    @Test void elAvisoDelInvitadoPermaneceSinMostrarInvitacionesAjenas() {
        eventos.cancelarEvento(1,organizador,true);
        assertEquals(1,invitados.consultarAvisosPorCorreo("juan@email.com").size());
        assertTrue(invitados.consultarAvisosPorCorreo("camila@email.com").isEmpty());
        assertTrue(invitados.consultarAvisosPorCorreo("desconocido@email.com").isEmpty());
        assertThrows(IllegalArgumentException.class,()->invitados.consultarAvisosPorCorreo(""));
        assertThrows(IllegalArgumentException.class,()->invitados.consultarAvisosPorCorreo("no es correo"));
    }

    @Test void reabrirLaBaseNoInventaServiciosParaSolicitudesHistoricas() throws Exception {
        sql("UPDATE Solicitud_Ejecucion SET id_servicio=NULL WHERE id_solicitud=1");
        try(Connection c=DatabaseConnection.getConnection(url)) {
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id_servicio FROM Solicitud_Ejecucion WHERE id_solicitud=1")) {
                r.next(); assertNull(r.getObject(1));
            }
        }
    }

    private void contratarSinVinculo(String estado, String fecha) throws Exception {
        sql("UPDATE Solicitud_Ejecucion SET id_servicio=1 WHERE id_solicitud=3");
        sql("INSERT INTO Contratacion (id_propuesta,id_evento,fecha_contratacion,estado,fecha_servicio,hora_inicio,hora_fin) "
                + "VALUES (3,2,CURRENT_TIMESTAMP,'"+estado+"',DATE '"+fecha+"',TIME '15:00:00',TIME '23:00:00')");
    }

    private void disponibilidad(int servicio) {
        DisponibilidadServicio d=new DisponibilidadServicio(); d.setIdServicio(servicio);
        d.setFecha(java.sql.Date.valueOf("2026-11-15"));d.setHorario("15:00 - 23:00");
        disponibilidades.guardarDisponibilidad(d);
    }
    private void sql(String sql) throws Exception {try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) {s.execute(sql);}}
    private String valor(String sql) {try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) {r.next();return r.getString(1);} catch(SQLException e) {throw new AssertionError(e);}}
}
