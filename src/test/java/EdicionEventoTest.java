import entities.EdicionEvento;
import entities.Evento;
import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;
import services.ServiceAutenticacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import config.DatabaseConnection;
import repositories.RepositoryEvento;
import services.ServiceEventos;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class EdicionEventoTest {
    @TempDir Path directorio;
    private String url;
    private RepositoryEvento repository;
    private ServiceEventos service;
    private Evento original;
    private SesionUsuario sesion, otraSesion;

    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:"+directorio.resolve("edicion");
        BaseDatosPrueba.preparar(url);
        sql("INSERT INTO Usuario (id_usuario,nombre,usuario,contraseña,estado,tipo_usuario,fecha_registro,acepta_marketing) VALUES (2,'Otro','otro','clave','ACTIVO','ORGANIZADOR',CURRENT_TIMESTAMP,FALSE)");
        sql("INSERT INTO Organizador (id_organizador,id_usuario,estado) VALUES (2,2,'ACTIVO')");
        ServiceAutenticacion login = new ServiceAutenticacion(new RepositoryAutenticacion(url));
        sesion=login.iniciarSesion("prueba","solo-prueba");
        otraSesion=login.iniciarSesion("otro","clave");
        repository=new RepositoryEvento(url); service=new ServiceEventos(repository);
        Evento evento=new Evento(1,"Evento original","Social","Descripción conservada",LocalDateTime.of(2026,11,1,9,0),
                LocalDateTime.of(2026,11,1,18,0),"Bogotá",new BigDecimal("1500000.25"),100);
        service.crearEvento(evento);
        original=repository.buscarEvento(evento.getIdEvento(),1);
    }

    @Test void precargaSoloEventosDelOrganizadorEnPlaneacion() {
        assertEquals(original.getNombre(),service.consultarEventoParaEditar(original.getIdEvento(),sesion).getNombre());
        assertThrows(IllegalArgumentException.class,()->service.consultarEventoParaEditar(original.getIdEvento(),otraSesion));
        assertThrows(IllegalArgumentException.class,()->service.consultarEventoParaEditar(999,sesion));
        assertThrows(IllegalArgumentException.class,()->service.editarEvento(original.getIdEvento(),otraSesion,cambios()));
        assertThrows(IllegalArgumentException.class,()->service.editarEvento(999,sesion,cambios()));
    }

    @Test void guardaSoloLosCamposPermitidosYConservaElEstado() {
        service.editarEvento(original.getIdEvento(),sesion,cambios());
        Evento editado=new RepositoryEvento(url).buscarEvento(original.getIdEvento(),1);
        assertEquals("Actualizado",editado.getNombre());
        assertEquals("Cultural",editado.getTipo());
        assertEquals(cambios().inicio(),editado.getFechaInicio());
        assertEquals(cambios().fin(),editado.getFechaFin());
        assertEquals("Medellín",editado.getUbicacion());
        assertEquals(original.getIdEvento(),editado.getIdEvento());
        assertEquals(original.getIdOrganizador(),editado.getIdOrganizador());
        assertEquals(original.getPresupuesto(),editado.getPresupuesto());
        assertEquals(original.getInvitadosIniciales(),editado.getInvitadosIniciales());
        assertEquals(original.getDescripcion(),editado.getDescripcion());
        assertEquals(original.getFechaCreacion(),editado.getFechaCreacion());
        assertEquals(original.getEstado(),editado.getEstado());
    }

    @Test void bloqueaOtrosEstadosSinCambiarLosDatos() throws Exception {
        for(String estado:new String[]{"CONFIRMADO","CANCELADO","FINALIZADO"}) {
            sql("UPDATE Evento SET estado='"+estado+"'");
            assertThrows(IllegalArgumentException.class,()->service.consultarEventoParaEditar(original.getIdEvento(),sesion));
            assertThrows(IllegalArgumentException.class,()->service.editarEvento(original.getIdEvento(),sesion,cambios()));
            assertEquals(original.getNombre(),repository.buscarEvento(original.getIdEvento(),1).getNombre());
            assertEquals(estado,repository.buscarEvento(original.getIdEvento(),1).getEstado());
        }
    }

    @Test void revalidaElEstadoSiCambiaDespuesDePrecargar() throws Exception {
        service.consultarEventoParaEditar(original.getIdEvento(),sesion);
        sql("UPDATE Evento SET estado='CONFIRMADO'");
        assertThrows(IllegalArgumentException.class,()->service.editarEvento(original.getIdEvento(),sesion,cambios()));
        assertEquals(original.getNombre(),repository.buscarEvento(original.getIdEvento(),1).getNombre());
    }

    @Test void rechazaDatosInvalidosSinModificarElEvento() {
        LocalDateTime inicio=original.getFechaInicio(), fin=original.getFechaFin();
        EdicionEvento[] invalidos={null,
                new EdicionEvento(" ","Social",inicio,fin,"Bogotá"),
                new EdicionEvento("x".repeat(151),"Social",inicio,fin,"Bogotá"),
                new EdicionEvento("Evento",null,inicio,fin,"Bogotá"),
                new EdicionEvento("Evento","x".repeat(101),inicio,fin,"Bogotá"),
                new EdicionEvento("Evento","Social",null,fin,"Bogotá"),
                new EdicionEvento("Evento","Social",inicio,null,"Bogotá"),
                new EdicionEvento("Evento","Social",inicio,inicio.minusHours(1),"Bogotá"),
                new EdicionEvento("Evento","Social",inicio,fin,""),
                new EdicionEvento("Evento","Social",inicio,fin,"x".repeat(256))};
        for(EdicionEvento datos:invalidos) assertThrows(IllegalArgumentException.class,()->service.editarEvento(original.getIdEvento(),sesion,datos));
        comprobarOriginal();
    }

    @Test void falloTecnicoConservaTodosLosDatosYPermiteReintentar() throws Exception {
        sql("ALTER TABLE Evento ADD CONSTRAINT fallo_edicion CHECK (nombre <> 'Actualizado')");
        IllegalStateException error=assertThrows(IllegalStateException.class,()->service.editarEvento(original.getIdEvento(),sesion,cambios()));
        assertTrue(error.getMessage().contains("anteriores se conservaron"));
        comprobarOriginal();
        sql("ALTER TABLE Evento DROP CONSTRAINT fallo_edicion");
        service.editarEvento(original.getIdEvento(),sesion,cambios());
        assertEquals("Actualizado",repository.buscarEvento(original.getIdEvento(),1).getNombre());
    }

    @Test void permiteEditarHistoricosSinInventarPresupuestoNiInvitados() throws Exception {
        sql("UPDATE Evento SET presupuesto=NULL,invitados_iniciales=NULL");
        service.editarEvento(original.getIdEvento(),sesion,cambios());
        Evento editado=repository.buscarEvento(original.getIdEvento(),1);
        assertNull(editado.getPresupuesto()); assertNull(editado.getInvitadosIniciales());
        assertEquals(original.getDescripcion(),editado.getDescripcion());
    }

    @Test void noNormalizaNiReemplazaElEstadoAlGuardar() throws Exception {
        sql("UPDATE Evento SET estado='planeado'");
        service.editarEvento(original.getIdEvento(),sesion,cambios());
        assertEquals("planeado",repository.buscarEvento(original.getIdEvento(),1).getEstado());
    }

    private EdicionEvento cambios() {
        return new EdicionEvento("Actualizado","Cultural",LocalDateTime.of(2026,11,2,10,0),LocalDateTime.of(2026,11,2,19,0),"Medellín");
    }
    private void comprobarOriginal() {
        Evento e=repository.buscarEvento(original.getIdEvento(),1);
        assertEquals(original.getNombre(),e.getNombre()); assertEquals(original.getTipo(),e.getTipo());
        assertEquals(original.getFechaInicio(),e.getFechaInicio()); assertEquals(original.getFechaFin(),e.getFechaFin());
        assertEquals(original.getUbicacion(),e.getUbicacion()); assertEquals(original.getPresupuesto(),e.getPresupuesto());
        assertEquals(original.getInvitadosIniciales(),e.getInvitadosIniciales()); assertEquals(original.getEstado(),e.getEstado());
        assertEquals(original.getDescripcion(),e.getDescripcion()); assertEquals(original.getFechaCreacion(),e.getFechaCreacion());
    }
    private void sql(String query) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url);Statement s=c.createStatement()) {s.execute(query);}
    }
}
