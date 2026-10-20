import config.DatabaseConnection;
import controllers.ControllerEventos;
import entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.*;
import services.*;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class SesionCancelacionTest {
    @TempDir Path directorio;
    private String url;
    private RepositoryEvento repository;
    private ServiceEventos service;
    private ServiceAutenticacion login;
    private SesionUsuario sesion;
    private ControllerEventos controller;
    private Evento propio, ajeno;

    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:"+directorio.resolve("cancelacion-sesion");
        BaseDatosPrueba.preparar(url);
        sql("INSERT INTO Usuario (id_usuario,nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) VALUES "
                +"(10,'Otra organizadora','otra','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ORGANIZADOR'),"
                +"(30,'Administrador','admin','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ADMINISTRADOR'),"
                +"(40,'Proveedor','proveedor','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'PROVEEDOR')");
        sql("INSERT INTO Organizador (id_organizador,id_usuario,estado) VALUES (20,10,'ACTIVO')");
        sql("INSERT INTO Proveedor (id_proveedor,id_usuario,nombre,estado) VALUES (50,40,'Proveedor','APROBADO')");
        repository=new RepositoryEvento(url);
        service=new ServiceEventos(repository,new RepositoryCancelacion(url));
        login=new ServiceAutenticacion(new RepositoryAutenticacion(url));
        sesion=login.iniciarSesion("otra","clave");
        controller=new ControllerEventos(service,sesion);
        propio=evento(20,"Evento propio"); ajeno=evento(1,"Evento ajeno");
    }

    @Test void listaYCancelaSoloEventosPropiosUsandoElActorDeLaSesion() {
        assertEquals(10,sesion.getIdUsuario()); assertEquals(20,sesion.getIdActor());
        assertEquals("Otra organizadora",controller.nombreOrganizadorCancelacion());
        assertEquals(java.util.List.of(propio.getIdEvento()),controller.listarEventosParaCancelar().stream().map(Evento::getIdEvento).toList());
        controller.cancelarEvento(propio.getIdEvento(),true);
        assertEquals("CANCELADO",repository.buscarEvento(propio.getIdEvento(),20).getEstado());
        assertEquals("PLANEADO",repository.buscarEvento(ajeno.getIdEvento(),1).getEstado());
    }

    @Test void noCancelaEventosAjenosNiExponeSusAvisos() throws Exception {
        sql("INSERT INTO Notificacion_interna (id_usuario,id_evento,mensaje) VALUES (1,"+ajeno.getIdEvento()+",'Aviso privado')");
        assertThrows(IllegalArgumentException.class,()->controller.cancelarEvento(ajeno.getIdEvento(),true));
        assertTrue(controller.listarAvisosCancelacion(ajeno.getIdEvento()).isEmpty());
        assertEquals("PLANEADO",repository.buscarEvento(ajeno.getIdEvento(),1).getEstado());
        assertEquals(1,new RepositoryCancelacion(url).listarAvisos(ajeno.getIdEvento(),1).size());
    }

    @Test void sinSesionAdministradorYProveedorNoPuedenAcceder() {
        comprobarBloqueo(new ControllerEventos(service));
        for(String usuario:new String[]{"admin","proveedor"})
            comprobarBloqueo(new ControllerEventos(service,login.iniciarSesion(usuario,"clave")));
    }

    @Test void cerrarSesionBloqueaElControladorQueTeniaElFormulario() {
        controller.listarEventosParaCancelar();
        sesion.cerrar();
        comprobarBloqueo(controller);
    }

    @Test void revalidaUsuarioYOrganizadorDespuesDelLogin() throws Exception {
        sql("UPDATE Usuario SET estado='INACTIVO' WHERE id_usuario=10");
        comprobarBloqueo(controller);
        sql("UPDATE Usuario SET estado='ACTIVO' WHERE id_usuario=10");
        sql("UPDATE Organizador SET estado='INACTIVO' WHERE id_organizador=20");
        comprobarBloqueo(controller);
    }

    @Test void noAceptaCambiarElActorDeLaSesionPorOtroOrganizador() {
        sesion.setIdActor(1);
        comprobarBloqueo(controller);
    }

    @Test void revalidaElEstadoSiElEventoFinalizaDespuesDeCargarLaLista() throws Exception {
        controller.listarEventosParaCancelar();
        sql("UPDATE Evento SET estado='FINALIZADO' WHERE id_evento="+propio.getIdEvento());
        assertThrows(IllegalArgumentException.class,()->controller.cancelarEvento(propio.getIdEvento(),true));
        assertEquals("FINALIZADO",repository.buscarEvento(propio.getIdEvento(),20).getEstado());
        assertTrue(controller.listarAvisosCancelacion(propio.getIdEvento()).isEmpty());
    }

    @Test void laSesionNoSustituyeLaConfirmacionExplicita() {
        assertThrows(IllegalArgumentException.class,()->controller.cancelarEvento(propio.getIdEvento(),false));
        assertEquals("PLANEADO",repository.buscarEvento(propio.getIdEvento(),20).getEstado());
        assertTrue(controller.listarAvisosCancelacion(propio.getIdEvento()).isEmpty());
    }

    private void comprobarBloqueo(ControllerEventos acceso) {
        assertThrows(IllegalStateException.class,acceso::listarEventosParaCancelar);
        assertThrows(IllegalStateException.class,acceso::nombreOrganizadorCancelacion);
        assertThrows(IllegalStateException.class,()->acceso.listarAvisosCancelacion(propio.getIdEvento()));
        assertThrows(IllegalStateException.class,()->acceso.cancelarEvento(propio.getIdEvento(),true));
        assertEquals("PLANEADO",repository.buscarEvento(propio.getIdEvento(),20).getEstado());
    }
    private Evento evento(int organizador,String nombre) {
        LocalDateTime inicio=LocalDateTime.of(2026,11,1,9,0);
        Evento evento=new Evento(organizador,nombre,"Social","",inicio,inicio.plusHours(2),"Bogotá",BigDecimal.TEN,10);
        service.crearEvento(evento); return evento;
    }
    private void sql(String sql) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) {s.execute(sql);}
    }
}
