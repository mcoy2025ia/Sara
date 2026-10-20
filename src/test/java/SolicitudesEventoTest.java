import config.DatabaseConnection;
import controllers.ControllerEventos;
import entities.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repositories.*;
import services.*;
import static org.junit.jupiter.api.Assertions.*;

class SolicitudesEventoTest {
    String url;
    ServiceEventos service;
    ControllerEventos controller;
    SesionUsuario sesion;
    Evento evento;

    @BeforeEach void preparar() throws Exception {
        url = "jdbc:h2:mem:evento_solicitudes_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        BaseDatosPrueba.preparar(url);
        sql("INSERT INTO Usuario(id_usuario,nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) VALUES "
                + "(2,'Proveedor','proveedor','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'PROVEEDOR'),"
                + "(3,'Otra cuenta','otra','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ORGANIZADOR')");
        sql("INSERT INTO Proveedor(id_proveedor,id_usuario,nombre,estado) VALUES(1,2,'Servicios de prueba','VERIFICADO')");
        sql("INSERT INTO Organizador(id_organizador,id_usuario,estado) VALUES(2,3,'ACTIVO')");
        sql("INSERT INTO Servicio(id_servicio,id_proveedor,nombre,precio,estado) VALUES(1,1,'Decoración',60.00,'DISPONIBLE'),(2,1,'Música',50.00,'DISPONIBLE')");
        service = new ServiceEventos(new RepositoryEvento(url),new RepositoryCancelacion(url),
                new RepositoryFinalizacion(url,java.time.Clock.systemDefaultZone()),new RepositorySolicitudServicio(url));
        sesion = new ServiceAutenticacion(new RepositoryAutenticacion(url)).iniciarSesion("prueba","solo-prueba");
        controller = new ControllerEventos(service,sesion);
        evento = datos(1);
        controller.crearEvento(evento);
    }

    @Test void crearExigeSesionProveedorVerificadoYOrganizadorPropio() throws Exception {
        assertThrows(IllegalStateException.class, () -> new ControllerEventos(service).crearEvento(datos(1)));
        assertThrows(IllegalArgumentException.class, () -> controller.crearEvento(datos(2)));
        sql("UPDATE Proveedor SET estado='PENDIENTE'");
        assertThrows(IllegalArgumentException.class, () -> controller.crearEvento(datos(1)));
        assertEquals(1, cuenta("Evento"));
    }

    @Test void solicitarConservaImporteYNecesidadParaCu12() throws Exception {
        controller.solicitarServicio(evento.getIdEvento(),1);
        assertEquals(new BigDecimal("40.00"),controller.consultarPresupuesto(evento.getIdEvento()).disponible());
        sql("UPDATE Servicio SET precio=90 WHERE id_servicio=1");
        assertEquals(new BigDecimal("60.00"),controller.consultarPresupuesto(evento.getIdEvento()).comprometido());
        assertEquals(1,cuenta("Necesidad_Servicio_Evento"));
        assertEquals(1,cuenta("Notificacion_interna"));
        try (Connection c=DatabaseConnection.getConnection(url); Statement s=c.createStatement();
             ResultSet r=s.executeQuery("SELECT n.id_evento FROM Solicitud_Ejecucion s JOIN Necesidad_Servicio_Evento n ON n.id_necesidad_servicio=s.id_necesidad_servicio")) {
            assertTrue(r.next()); assertEquals(evento.getIdEvento(),r.getInt(1));
        }
    }

    @Test void presupuestoInsuficienteConservaEventoSinCrearOtroCompromiso() throws Exception {
        controller.solicitarServicio(evento.getIdEvento(),1);
        var error=assertThrows(IllegalArgumentException.class,()->controller.solicitarServicio(evento.getIdEvento(),2));
        assertTrue(error.getMessage().contains("10.00"));
        assertEquals(1,cuenta("Solicitud_Ejecucion"));
        assertEquals(1,cuenta("Necesidad_Servicio_Evento"));
        assertEquals("PLANEADO",new RepositoryEvento(url).buscarEvento(evento.getIdEvento(),1).getEstado());
    }

    @Test void falloDeAvisoRevierteSolicitudNecesidadYPresupuesto() throws Exception {
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo_aviso CHECK(id_usuario<>2)");
        assertThrows(IllegalStateException.class,()->controller.solicitarServicio(evento.getIdEvento(),1));
        assertEquals(0,cuenta("Solicitud_Ejecucion"));
        assertEquals(0,cuenta("Necesidad_Servicio_Evento"));
        assertEquals(0,controller.consultarPresupuesto(evento.getIdEvento()).comprometido().signum());
    }

    @Test void ajustarORetirarPermiteResolverFaltaDePresupuesto() {
        int solicitud=controller.solicitarServicio(evento.getIdEvento(),1);
        assertThrows(IllegalArgumentException.class,()->controller.ajustarPresupuesto(evento.getIdEvento(),new BigDecimal("59")));
        controller.ajustarPresupuesto(evento.getIdEvento(),new BigDecimal("110"));
        controller.solicitarServicio(evento.getIdEvento(),2);
        assertEquals(0,controller.consultarPresupuesto(evento.getIdEvento()).disponible().signum());
        controller.retirarSolicitudServicio(evento.getIdEvento(),solicitud);
        assertEquals(new BigDecimal("60.00"),controller.consultarPresupuesto(evento.getIdEvento()).disponible());
    }

    @Test void noRetiraUnaSolicitudConPropuesta() throws Exception {
        int solicitud=controller.solicitarServicio(evento.getIdEvento(),1);
        sql("INSERT INTO Propuesta(id_solicitud,descripcion,valor,estado) VALUES("+solicitud+",'Propuesta',60,'ENVIADA')");
        assertThrows(IllegalArgumentException.class,()->controller.retirarSolicitudServicio(evento.getIdEvento(),solicitud));
        assertEquals(new BigDecimal("60.00"),controller.consultarPresupuesto(evento.getIdEvento()).comprometido());
    }

    @Test void solicitudesAjenasSesionCerradaYActorSuspendidoNoPuedenOperar() throws Exception {
        Evento ajeno=datos(2); service.crearEvento(ajeno);
        assertThrows(IllegalArgumentException.class,()->controller.solicitarServicio(ajeno.getIdEvento(),1));
        sql("UPDATE Organizador SET estado='INACTIVO' WHERE id_organizador=1");
        assertThrows(IllegalStateException.class,()->controller.consultarPresupuesto(evento.getIdEvento()));
        sql("UPDATE Organizador SET estado='ACTIVO' WHERE id_organizador=1");
        sesion.cerrar();
        assertThrows(IllegalStateException.class,()->controller.solicitarServicio(evento.getIdEvento(),1));
    }

    @Test void proveedorSuspendidoOServicioNoDisponibleNoComprometenDinero() throws Exception {
        sql("UPDATE Usuario SET estado='INACTIVO' WHERE id_usuario=2");
        assertTrue(controller.listarServiciosSolicitables().isEmpty());
        assertThrows(IllegalArgumentException.class,()->controller.solicitarServicio(evento.getIdEvento(),1));
        sql("UPDATE Usuario SET estado='ACTIVO' WHERE id_usuario=2");
        sql("UPDATE Servicio SET estado='INACTIVO' WHERE id_servicio=1");
        assertThrows(IllegalArgumentException.class,()->controller.solicitarServicio(evento.getIdEvento(),1));
        assertEquals(0,cuenta("Solicitud_Ejecucion"));
    }

    @Test void contratoSustituyeSolicitudSinDuplicarYSeConservaAlFinalizar() throws Exception {
        int solicitud=controller.solicitarServicio(evento.getIdEvento(),1);
        sql("INSERT INTO Propuesta(id_propuesta,id_solicitud,descripcion,valor,estado) VALUES(1,"+solicitud+",'Propuesta',70,'ACEPTADA')");
        sql("INSERT INTO Contratacion(id_propuesta,id_evento,fecha_contratacion,valor_acordado,estado) VALUES(1,"+evento.getIdEvento()+",CURRENT_TIMESTAMP,70,'ACTIVA')");
        assertEquals(new BigDecimal("70.00"),controller.consultarPresupuesto(evento.getIdEvento()).comprometido());
        sql("UPDATE Contratacion SET estado='FINALIZADA'");
        assertEquals(new BigDecimal("70.00"),controller.consultarPresupuesto(evento.getIdEvento()).comprometido());
    }

    @Test void cancelarEventoCancelaSolicitudYPresupuestoYNotificaAlProveedor() throws Exception {
        controller.solicitarServicio(evento.getIdEvento(),1);
        controller.cancelarEvento(evento.getIdEvento(),true);
        assertEquals("CANCELADA",controller.listarSolicitudesServicio(evento.getIdEvento()).get(0).estado());
        assertEquals(0,controller.consultarPresupuesto(evento.getIdEvento()).comprometido().signum());
        assertEquals(2,cuenta("Notificacion_interna"));
    }

    @Test void dosSolicitudesSimultaneasNoSuperanElPresupuesto() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch inicio=new CountDownLatch(1);
        try {
            Callable<Boolean> solicitar=()->{ inicio.await(); try {controller.solicitarServicio(evento.getIdEvento(),1);return true;}
                catch(IllegalArgumentException e) {return false;} };
            Future<Boolean> a=pool.submit(solicitar), b=pool.submit(solicitar);
            inicio.countDown();
            assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertEquals(1,cuenta("Solicitud_Ejecucion"));
            assertEquals(new BigDecimal("40.00"),controller.consultarPresupuesto(evento.getIdEvento()).disponible());
        } finally {pool.shutdownNow();}
    }

    @Test void proveedorVerificadoRespondeEnCu12YOrganizadorSeleccionaSuPropuesta() {
        System.setProperty("nexteventux.db.url",url);
        try {
            int solicitud=controller.solicitarServicio(evento.getIdEvento(),1);
            SesionUsuario proveedor=new ServiceAutenticacion(new RepositoryAutenticacion(url)).iniciarSesion("proveedor","clave");
            ServiceSolicitudes respuestas=new ServiceSolicitudes(new RepositorySolicitudEjecucion(),new RepositoryPropuestaServicio(),proveedor);
            assertEquals(1,respuestas.listarSolicitudesProveedor(1).size());
            int propuesta=respuestas.enviarPropuesta(1,solicitud,new BigDecimal("60.00"),"Decoración del evento","Entrega acordada");
            ServiceSolicitudes organizador=new ServiceSolicitudes(new RepositorySolicitudEjecucion(),new RepositoryPropuestaServicio(),sesion);
            organizador.seleccionarPropuesta(1,propuesta);
            assertEquals("EN_NEGOCIACION",controller.listarSolicitudesServicio(evento.getIdEvento()).get(0).estado());
            assertEquals(new BigDecimal("60.00"),controller.consultarPresupuesto(evento.getIdEvento()).comprometido());
        } finally {System.clearProperty("nexteventux.db.url");}
    }

    @Test void solicitarDesdeCatalogoRespetaElMismoPresupuestoDeCu01() throws Exception {
        System.setProperty("nexteventux.db.url",url);
        try {
            ServiceSolicitudes catalogo=new ServiceSolicitudes(new RepositorySolicitudEjecucion(),new RepositoryPropuestaServicio(),sesion);
            catalogo.crearSolicitud(1,evento.getIdEvento(),1,"Decoración del salón");
            assertEquals(new BigDecimal("40.00"),controller.consultarPresupuesto(evento.getIdEvento()).disponible());
            assertThrows(IllegalArgumentException.class,()->catalogo.crearSolicitud(1,evento.getIdEvento(),2,"Música"));
            assertEquals(1,cuenta("Solicitud_Ejecucion"));
            assertEquals(1,cuenta("Necesidad_Servicio_Evento"));
        } finally {System.clearProperty("nexteventux.db.url");}
    }

    private Evento datos(int organizador) {
        LocalDateTime inicio=LocalDateTime.of(2027,1,10,10,0);
        return new Evento(organizador,"Celebración","Social","Descripción",inicio,inicio.plusHours(3),"Bogotá",new BigDecimal("100.00"),10);
    }
    private void sql(String sql) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url); Statement s=c.createStatement()) {s.execute(sql);}
    }
    private int cuenta(String tabla) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+tabla)) {r.next();return r.getInt(1);}
    }
}
