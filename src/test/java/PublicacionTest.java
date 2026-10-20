import config.DatabaseConnection;
import entities.Evento;
import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;
import services.ServiceAutenticacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryEvento;
import repositories.RepositoryPublicacion;
import services.ServicePublicacion;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PublicacionTest {
    @TempDir Path directorio;
    private String url;
    private ServicePublicacion service;
    private RepositoryEvento eventos;
    private Evento evento;
    private SesionUsuario organizador, administrador, otro;

    @BeforeEach void preparar() throws Exception {
        url = "jdbc:h2:" + directorio.resolve("publicacion");
        BaseDatosPrueba.preparar(url);
        ejecutar("INSERT INTO Usuario (id_usuario,nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) "
                + "VALUES (20,'Administrador de prueba','admin','solo-prueba','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ADMINISTRADOR')");
        ejecutar("INSERT INTO Rol (id_rol,nombre,estado) VALUES (20,'ADMINISTRADOR','ACTIVO')");
        ejecutar("INSERT INTO Usuario_rol (id_usuario_rol,id_usuario,id_rol) VALUES (20,20,20)");
        ejecutar("INSERT INTO Usuario (id_usuario,nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) "
                + "VALUES (30,'Otro organizador','otro','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ORGANIZADOR')");
        ejecutar("INSERT INTO Organizador (id_organizador,id_usuario,estado) VALUES (99,30,'ACTIVO')");
        var login = new ServiceAutenticacion(new RepositoryAutenticacion(url));
        organizador = login.iniciarSesion("prueba","solo-prueba");
        administrador = login.iniciarSesion("admin","solo-prueba");
        otro = login.iniciarSesion("otro","clave");
        eventos = new RepositoryEvento(url);
        service = new ServicePublicacion(new RepositoryPublicacion(url), eventos);
        evento = new Evento(1,"Fiesta privada de Ana", "Social", "Datos personales de invitados",
                LocalDateTime.of(2026,10,1,9,0),LocalDateTime.of(2026,10,1,18,0),
                "Dirección privada",new BigDecimal("1234567.89"),50);
        evento.setEstado("FINALIZADO");
        eventos.guardarEvento(evento);
    }

    @Test void registraSolicitudYNotificaALosAdministradoresSinPublicar() {
        solicitar();
        assertEquals("PENDIENTE",service.listarSolicitudes(organizador).get(0).estado());
        assertEquals(1,service.notificaciones(administrador).size());
        assertTrue(service.listarPlantillas(organizador).isEmpty());
        assertTrue(service.notificaciones(organizador).isEmpty());
    }

    @Test void apruebaYPublicaSoloDatosEstructurales() {
        solicitar();
        service.revisar(administrador,idSolicitud(),true,null);
        var plantilla=service.listarPlantillas(organizador).get(0);
        assertEquals("Plantilla de evento Social",plantilla.nombre());
        assertEquals("Social",plantilla.tipo());
        assertEquals(540,plantilla.duracionMinutos());
        assertFalse(plantilla.toString().contains("Ana"));
        assertFalse(plantilla.toString().contains("1234567"));
        assertEquals("APROBADA",service.listarSolicitudes(organizador).get(0).estado());
        assertTrue(service.notificaciones(organizador).get(0).contains("se publicó"));
        assertEquals(evento.getPresupuesto(),eventos.listarEventos().get(0).getPresupuesto());
        assertEquals("FINALIZADO",eventos.listarEventos().get(0).getEstado());
    }

    @Test void rechazaConMotivoYPermiteUnaNuevaSolicitudExplicita() {
        solicitar();
        service.revisar(administrador,idSolicitud(),false,"Contiene datos privados");
        assertTrue(service.listarPlantillas(organizador).isEmpty());
        assertEquals("RECHAZADA",service.listarSolicitudes(organizador).get(0).estado());
        assertTrue(service.notificaciones(organizador).get(0).contains("Contiene datos privados"));
        solicitar();
        assertEquals(2,service.listarSolicitudes(organizador).size());
        assertEquals("PENDIENTE",service.listarSolicitudes(organizador).get(0).estado());
    }

    @Test void bloqueaEventoNoFinalizado() throws Exception {
        ejecutar("UPDATE Evento SET estado='PLANEADO'");
        assertThrows(IllegalArgumentException.class,this::solicitar);
        assertTrue(service.listarSolicitudes(organizador).isEmpty());
        assertTrue(service.notificaciones(administrador).isEmpty());
    }

    @Test void bloqueaEventoAjenoOInexistente() {
        assertThrows(IllegalArgumentException.class,()->service.solicitar(otro,evento.getIdEvento()));
        assertThrows(IllegalArgumentException.class,()->service.solicitar(organizador,999));
        assertTrue(service.listarSolicitudes(organizador).isEmpty());
    }

    @Test void noDuplicaSolicitudNiPublicacion() {
        solicitar();
        assertThrows(IllegalArgumentException.class,this::solicitar);
        int id=idSolicitud();
        service.revisar(administrador,id,true,null);
        assertThrows(IllegalArgumentException.class,()->service.revisar(administrador,id,true,null));
        assertThrows(IllegalArgumentException.class,this::solicitar);
        assertEquals(1,service.listarPlantillas(organizador).size());
        assertEquals(1,service.notificaciones(organizador).size());
    }

    @Test void exigeAdministradorConRolActivo() throws Exception {
        solicitar();
        assertThrows(IllegalStateException.class,()->service.revisar(organizador,idSolicitud(),true,null));
        ejecutar("UPDATE Usuario SET estado='SUSPENDIDO' WHERE id_usuario=20");
        assertThrows(IllegalStateException.class,()->service.revisar(administrador,idSolicitud(),true,null));
        assertEquals("PENDIENTE",service.listarSolicitudes(organizador).get(0).estado());
    }

    @Test void noEnviaSiNoHayAdministradorDisponible() throws Exception {
        ejecutar("DELETE FROM Usuario_rol");
        assertThrows(IllegalArgumentException.class,this::solicitar);
        assertTrue(service.listarSolicitudes(organizador).isEmpty());
    }

    @Test void exigeMotivoDeRechazo() {
        solicitar();
        for(String motivo:new String[]{null," ","x".repeat(251)})
            assertThrows(IllegalArgumentException.class,()->service.revisar(administrador,idSolicitud(),false,motivo));
        assertEquals("PENDIENTE",service.listarSolicitudes(organizador).get(0).estado());
    }

    @Test void revalidaEstadoAntesDeAprobar() throws Exception {
        solicitar();
        ejecutar("UPDATE Evento SET estado='CANCELADO'");
        assertThrows(IllegalArgumentException.class,()->service.revisar(administrador,idSolicitud(),true,null));
        assertTrue(service.listarPlantillas(organizador).isEmpty());
    }

    @Test void revierteAprobacionSiFallaLaNotificacionYPermiteReintentar() throws Exception {
        solicitar();
        ejecutar("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo_prueba CHECK (id_usuario <> 1)");
        assertThrows(IllegalStateException.class,()->service.revisar(administrador,idSolicitud(),true,null));
        assertTrue(service.listarPlantillas(organizador).isEmpty());
        assertEquals("PENDIENTE",service.listarSolicitudes(organizador).get(0).estado());
        ejecutar("ALTER TABLE Notificacion_interna DROP CONSTRAINT fallo_prueba");
        service.revisar(administrador,idSolicitud(),true,null);
        assertEquals(1,service.listarPlantillas(organizador).size());
    }

    @Test void revierteSolicitudSiFallaElEnvioInterno() throws Exception {
        ejecutar("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo_prueba CHECK (id_usuario <> 20)");
        assertThrows(IllegalStateException.class,this::solicitar);
        assertTrue(service.listarSolicitudes(organizador).isEmpty());
        ejecutar("ALTER TABLE Notificacion_interna DROP CONSTRAINT fallo_prueba");
        solicitar();
        assertEquals(1,service.listarSolicitudes(organizador).size());
    }

    @Test void conservaPlantillasYNotificacionesAlReabrirH2() {
        solicitar();
        service.revisar(administrador,idSolicitud(),true,null);
        var nueva = new ServicePublicacion(new RepositoryPublicacion(url),new RepositoryEvento(url));
        assertEquals(1,nueva.listarPlantillas(organizador).size());
        assertEquals(1,nueva.notificaciones(organizador).size());
    }

    private void solicitar() { service.solicitar(organizador,evento.getIdEvento()); }
    private int idSolicitud() { return service.listarSolicitudes(organizador).get(0).id(); }
    private void ejecutar(String sql) throws Exception {
        try(Connection connection=DatabaseConnection.getConnection(url); Statement statement=connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
