import config.DatabaseConnection;
import controllers.ControllerPublicacion;
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

class SesionPublicacionTest {
    @TempDir Path directorio;
    private String url;
    private ServicePublicacion service;
    private SesionUsuario organizador, administrador, otro, proveedor;
    private ControllerPublicacion solicitudes, revision;
    private Evento propio, ajeno;

    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:"+directorio.resolve("publicacion-sesion");
        BaseDatosPrueba.preparar(url);
        usuario(10,"otra","ORGANIZADOR"); usuario(30,"admin","ADMINISTRADOR"); usuario(40,"proveedor","PROVEEDOR");
        sql("INSERT INTO Organizador (id_organizador,id_usuario,estado) VALUES (20,10,'ACTIVO')");
        sql("INSERT INTO Proveedor (id_proveedor,id_usuario,nombre,estado) VALUES (50,40,'Proveedor','APROBADO')");
        sql("INSERT INTO Rol (id_rol,nombre,estado) VALUES (30,'ADMINISTRADOR','ACTIVO')");
        sql("INSERT INTO Usuario_rol (id_usuario_rol,id_usuario,id_rol) VALUES (30,30,30)");
        var login=new ServiceAutenticacion(new RepositoryAutenticacion(url));
        organizador=login.iniciarSesion("otra","clave");
        otro=login.iniciarSesion("prueba","solo-prueba");
        administrador=login.iniciarSesion("admin","clave");
        proveedor=login.iniciarSesion("proveedor","clave");
        var eventos=new RepositoryEvento(url);
        propio=evento(eventos,20,"Evento privado propio"); ajeno=evento(eventos,1,"Evento ajeno");
        service=new ServicePublicacion(new RepositoryPublicacion(url),eventos);
        solicitudes=new ControllerPublicacion(service,organizador);
        revision=new ControllerPublicacion(service,administrador);
    }

    @Test void usaElOrganizadorDeLaSesionYFiltraEventosSolicitudesYAvisos() {
        assertEquals(10,organizador.getIdUsuario()); assertEquals(20,organizador.getIdActor());
        assertEquals(java.util.List.of(propio.getIdEvento()),solicitudes.listarEventos().stream().map(Evento::getIdEvento).toList());
        solicitudes.solicitar(propio.getIdEvento());
        service.solicitar(otro,ajeno.getIdEvento());
        assertEquals(1,solicitudes.listarSolicitudes().size());
        assertEquals(2,revision.listarSolicitudes().size());
        assertEquals(2,revision.notificaciones().size());
        revision.revisar(solicitudes.listarSolicitudes().get(0).id(),true,null);
        assertEquals(1,solicitudes.notificaciones().size());
        assertTrue(service.notificaciones(otro).isEmpty());
        assertFalse(solicitudes.listarPlantillas().get(0).toString().contains("privado"));
    }

    @Test void noPermiteSolicitarEventosAjenosNiRevisarComoOrganizador() {
        assertThrows(IllegalArgumentException.class,()->solicitudes.solicitar(ajeno.getIdEvento()));
        solicitudes.solicitar(propio.getIdEvento());
        assertThrows(IllegalStateException.class,()->solicitudes.revisar(solicitudes.listarSolicitudes().get(0).id(),true,null));
        assertEquals("PENDIENTE",solicitudes.listarSolicitudes().get(0).estado());
    }

    @Test void administradorNoPuedeSuplantarAlOrganizador() {
        assertTrue(revision.esAdministrador());
        assertThrows(IllegalStateException.class,revision::listarEventos);
        assertThrows(IllegalStateException.class,()->revision.solicitar(propio.getIdEvento()));
        assertTrue(revision.listarSolicitudes().isEmpty());
    }

    @Test void sinSesionOComoProveedorNoSePuedeAcceder() {
        comprobarBloqueo(new ControllerPublicacion(service,null));
        comprobarBloqueo(new ControllerPublicacion(service,proveedor));
    }

    @Test void cerrarSesionBloqueaLecturasYOperacionesDeAmbosPerfiles() {
        solicitudes.solicitar(propio.getIdEvento());
        organizador.cerrar(); administrador.cerrar();
        comprobarBloqueo(solicitudes); comprobarBloqueo(revision);
        assertEquals("PENDIENTE",new RepositoryPublicacion(url).listarSolicitudes(20).get(0).estado());
    }

    @Test void revocarElRolDelAdministradorImpideLeerYRevisar() throws Exception {
        solicitudes.solicitar(propio.getIdEvento());
        sql("UPDATE Rol SET estado='INACTIVO' WHERE id_rol=30");
        comprobarBloqueo(revision);
        assertEquals("PENDIENTE",solicitudes.listarSolicitudes().get(0).estado());
    }

    @Test void revalidaUsuarioYOrganizadorDespuesDelLogin() throws Exception {
        sql("UPDATE Usuario SET estado='INACTIVO' WHERE id_usuario=10");
        comprobarBloqueo(solicitudes);
        sql("UPDATE Usuario SET estado='ACTIVO' WHERE id_usuario=10");
        sql("UPDATE Organizador SET estado='INACTIVO' WHERE id_organizador=20");
        comprobarBloqueo(solicitudes);
    }

    @Test void noAceptaCambiarElActorPorElDeOtraCuenta() {
        organizador.setIdActor(1);
        comprobarBloqueo(solicitudes);
    }

    @Test void unUsuarioSinTipoAdministradorNoRecibeSolicitudesAunqueTengaEseRol() throws Exception {
        sql("UPDATE Usuario SET tipo_usuario='ORGANIZADOR' WHERE id_usuario=30");
        comprobarBloqueo(revision);
        assertThrows(IllegalArgumentException.class,()->solicitudes.solicitar(propio.getIdEvento()));
        assertTrue(solicitudes.listarSolicitudes().isEmpty());
    }

    private void comprobarBloqueo(ControllerPublicacion controller) {
        assertThrows(IllegalStateException.class,controller::listarSolicitudes);
        assertThrows(IllegalStateException.class,controller::listarPlantillas);
        assertThrows(IllegalStateException.class,controller::notificaciones);
        assertThrows(IllegalStateException.class,()->controller.solicitar(propio.getIdEvento()));
        assertThrows(IllegalStateException.class,()->controller.revisar(1,true,null));
    }
    private Evento evento(RepositoryEvento repository,int organizador,String nombre) {
        LocalDateTime inicio=LocalDateTime.of(2026,9,1,9,0);
        Evento evento=new Evento(organizador,nombre,"Social","Datos privados",inicio,inicio.plusHours(2),"Lugar privado",BigDecimal.TEN,10);
        evento.setEstado("FINALIZADO"); repository.guardarEvento(evento); return evento;
    }
    private void usuario(int id,String nombre,String tipo) throws Exception {
        sql("INSERT INTO Usuario (id_usuario,nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) VALUES ("
                +id+",'"+nombre+"','"+nombre+"','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'"+tipo+"')");
    }
    private void sql(String sql) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) { s.execute(sql); }
    }
}
