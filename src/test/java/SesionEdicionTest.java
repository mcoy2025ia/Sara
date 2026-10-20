import config.DatabaseConnection;
import controllers.ControllerEventos;
import entities.EdicionEvento;
import entities.Evento;
import entities.SesionUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryEvento;
import services.ServiceAutenticacion;
import services.ServiceEventos;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class SesionEdicionTest {
    @TempDir Path directorio;
    private String url;
    private RepositoryEvento repository;
    private ServiceEventos service;
    private SesionUsuario sesion;
    private ControllerEventos controller;
    private Evento propio, ajeno;

    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:"+directorio.resolve("sesion");
        BaseDatosPrueba.preparar(url);
        sql("INSERT INTO Usuario (id_usuario,nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) "
                +"VALUES (10,'Otra organizadora','otra','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ORGANIZADOR')");
        sql("INSERT INTO Organizador (id_organizador,id_usuario,estado) VALUES (20,10,'ACTIVO')");
        repository=new RepositoryEvento(url);
        service=new ServiceEventos(repository);
        propio=evento(20,"Evento propio"); ajeno=evento(1,"Evento ajeno");
        sesion=new ServiceAutenticacion(new RepositoryAutenticacion(url)).iniciarSesion("otra","clave");
        controller=new ControllerEventos(service,sesion);
    }

    @Test void loginFiltraPorOrganizadorAunqueElIdDeUsuarioSeaDistinto() {
        assertEquals(10,sesion.getIdUsuario()); assertEquals(20,sesion.getIdActor());
        assertEquals("Otra organizadora",controller.nombreOrganizadorEdicion());
        assertEquals(java.util.List.of(propio.getIdEvento()),controller.listarEventosParaEditar().stream().map(Evento::getIdEvento).toList());
        controller.editarEvento(propio.getIdEvento(),cambios());
        assertEquals("Actualizado",repository.buscarEvento(propio.getIdEvento(),20).getNombre());
    }

    @Test void rechazaConsultarYEditarUnEventoAjenoPorSuId() {
        assertThrows(IllegalArgumentException.class,()->controller.consultarEventoParaEditar(ajeno.getIdEvento()));
        assertThrows(IllegalArgumentException.class,()->controller.editarEvento(ajeno.getIdEvento(),cambios()));
        assertEquals("Evento ajeno",repository.buscarEvento(ajeno.getIdEvento(),1).getNombre());
    }

    @Test void sinSesionNoSePuedeListarConsultarNiGuardar() {
        comprobarBloqueo(new ControllerEventos(service));
    }

    @Test void cerrarSesionInvalidaElControladorDelFormularioAbierto() {
        controller.consultarEventoParaEditar(propio.getIdEvento());
        sesion.cerrar();
        comprobarBloqueo(controller);
    }

    @Test void administradorYProveedorNoPuedenEditarEventos() {
        for(String tipo:new String[]{"ADMINISTRADOR","PROVEEDOR"}) {
            sesion.setTipoUsuario(tipo);
            comprobarBloqueo(controller);
        }
    }

    @Test void revalidaUsuarioYOrganizadorEnLaBaseDespuesDelLogin() throws Exception {
        sql("UPDATE Usuario SET estado='INACTIVO' WHERE id_usuario=10");
        comprobarBloqueo(controller);
        sql("UPDATE Usuario SET estado='ACTIVO' WHERE id_usuario=10");
        sql("UPDATE Organizador SET estado='INACTIVO' WHERE id_organizador=20");
        comprobarBloqueo(controller);
    }

    @Test void noAceptaCambiarElOrganizadorDeLaSesionPorUnoAjeno() {
        sesion.setIdActor(1);
        comprobarBloqueo(controller);
    }

    private void comprobarBloqueo(ControllerEventos acceso) {
        assertThrows(IllegalStateException.class,acceso::listarEventosParaEditar);
        assertThrows(IllegalStateException.class,()->acceso.consultarEventoParaEditar(propio.getIdEvento()));
        assertThrows(IllegalStateException.class,()->acceso.editarEvento(propio.getIdEvento(),cambios()));
        assertEquals("Evento propio",repository.buscarEvento(propio.getIdEvento(),20).getNombre());
    }
    private Evento evento(int organizador,String nombre) {
        LocalDateTime inicio=LocalDateTime.of(2026,11,1,9,0);
        Evento evento=new Evento(organizador,nombre,"Social","Descripción",inicio,inicio.plusHours(2),"Bogotá",BigDecimal.TEN,10);
        service.crearEvento(evento); return evento;
    }
    private EdicionEvento cambios() {
        return new EdicionEvento("Actualizado","Social",propio.getFechaInicio(),propio.getFechaFin(),"Bogotá");
    }
    private void sql(String sql) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) { s.execute(sql); }
    }
}
