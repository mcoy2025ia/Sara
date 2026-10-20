import config.*;
import controllers.ControllerEventos;
import entities.*;
import repositories.*;
import services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FinalizacionTest {
    String url;
    ServiceEventos service;
    ControllerEventos controller;
    SesionUsuario sesion;
    List<Fotografia> fotos;
    final Clock reloj=Clock.fixed(Instant.parse("2027-01-01T18:00:00Z"), ZoneId.of("America/Bogota"));

    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:mem:finalizar"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";
        try(Connection c=DatabaseConnection.getConnection(url)) { CargarDatosEjemplo.cargar(c); }
        sql("UPDATE Evento SET estado='CONFIRMADO',fecha_fin=TIMESTAMP '2026-12-31 23:00:00',id_lugar=1 WHERE id_evento=1");
        sql("INSERT INTO Disponibilidad_Servicio VALUES (1,1,DATE '2026-11-15',TIME '15:00:00',TIME '23:00:00','RESERVADO')");
        sql("INSERT INTO Disponibilidad_Servicio VALUES (2,3,DATE '2026-11-15',TIME '15:00:00',TIME '23:00:00','RESERVADO')");
        sql("UPDATE Contratacion SET id_disponibilidad=id_contratacion");
        sql("UPDATE Lugar SET estado='RESERVADO' WHERE id_lugar=1");
        service=new ServiceEventos(new RepositoryEvento(url),new RepositoryCancelacion(url),new RepositoryFinalizacion(url,reloj));
        sesion=new ServiceAutenticacion(new RepositoryAutenticacion(url)).iniciarSesion("laura.gomez","clave123");
        controller=new ControllerEventos(service,sesion);
        var imagen=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var salida=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(imagen,"png",salida);
        fotos=List.of(new Fotografia("evento.png",salida.toByteArray()));
    }

    @Test void finalizaLiberaGuardaFotosYComparteSoloConConfirmados() throws Exception {
        controller.finalizarEvento(1,fotos);
        assertEquals("FINALIZADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
        assertEquals("DISPONIBLE",valor("SELECT estado FROM Disponibilidad_Servicio WHERE id_disponibilidad=1"));
        assertEquals("DISPONIBLE",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        assertEquals("FINALIZADA",valor("SELECT estado FROM Contratacion WHERE id_contratacion=1"));
        assertEquals("TRUE",valor("SELECT enviada FROM Finalizacion_evento WHERE id_evento=1"));
        assertEquals("2",valor("SELECT COUNT(*) FROM Entrega_galeria WHERE id_evento=1"));
        assertEquals("2",valor("SELECT COUNT(*) FROM Notificacion_interna WHERE id_evento=1"));
        assertEquals("0",valor("SELECT COUNT(*) FROM Entrega_galeria WHERE id_invitado IN (2,4,6)"));
        assertEquals(List.of("Andrés Torres","Mariana López"),controller.proveedoresParaCalificar(1));
        assertEquals("ENVIADA",valor("SELECT estado_respuesta FROM Invitacion WHERE id_invitacion=1"));
        try(Connection c=DatabaseConnection.getConnection(url);var s=c.createStatement();var r=s.executeQuery("SELECT contenido FROM Fotografia_evento")) {
            assertTrue(r.next()); assertArrayEquals(fotos.get(0).contenido(),r.getBytes(1));
        }
    }

    @Test void bloqueaAntesDelFinInclusoSiYaPasoElInicio() throws Exception {
        sql("UPDATE Evento SET fecha_fin=TIMESTAMP '2027-01-01 14:00:00' WHERE id_evento=1");
        assertTrue(assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,fotos)).getMessage().contains("01/01/2027 14:00"));
        sinCambios();
    }
    @Test void tampocoFinalizaExactamenteALaHoraDeFin() throws Exception {
        sql("UPDATE Evento SET fecha_fin=TIMESTAMP '2027-01-01 13:00:00' WHERE id_evento=1");
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,fotos)); sinCambios();
    }
    @Test void exigeConfirmado() throws Exception {
        for(String estado:List.of("PLANEADO","CANCELADO","FINALIZADO")) {
            sql("UPDATE Evento SET estado='"+estado+"' WHERE id_evento=1");
            assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,fotos));
            assertEquals("0",valor("SELECT COUNT(*) FROM Finalizacion_evento"));
        }
    }
    @Test void exigeSesionActivaYPropiedad() throws Exception {
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(2,fotos));
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(999,fotos));
        assertThrows(IllegalStateException.class,()->new ControllerEventos(service).finalizarEvento(1,fotos));
        sql("UPDATE Usuario SET estado='INACTIVO' WHERE id_usuario="+sesion.getIdUsuario());
        assertThrows(IllegalStateException.class,()->controller.finalizarEvento(1,fotos)); sinCambios();
    }
    @Test void rechazaFotosAusentesInvalidasYExcesivas() throws Exception {
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,List.of()));
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,List.of(new Fotografia("foto.png",new byte[]{1,2,3}))));
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,Collections.nCopies(11,fotos.get(0))));
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,List.of(new Fotografia("grande.png",new byte[5*1024*1024+1]))));
        sinCambios();
    }
    @Test void falloDeLiberacionRevierteFotosYReservas() throws Exception {
        sql("ALTER TABLE Lugar ADD CONSTRAINT fallo CHECK (id_lugar<>1 OR estado<>'DISPONIBLE')");
        assertThrows(IllegalStateException.class,()->controller.finalizarEvento(1,fotos)); sinCambios();
        assertEquals("0",valor("SELECT COUNT(*) FROM Fotografia_evento"));
    }
    @Test void errorDeEnvioConservaLiberacionYReintentaSinFotosNiDuplicados() throws Exception {
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo CHECK (id_evento IS NULL OR id_evento<>1)");
        assertTrue(assertThrows(IllegalStateException.class,()->controller.finalizarEvento(1,fotos)).getMessage().contains("Reintenta"));
        assertEquals("CONFIRMADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
        assertEquals("DISPONIBLE",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        assertEquals("DISPONIBLE",valor("SELECT estado FROM Disponibilidad_Servicio WHERE id_disponibilidad=1"));
        assertEquals("FALSE",valor("SELECT enviada FROM Finalizacion_evento"));
        assertTrue(controller.galeriaPendiente(1));
        assertEquals("0",valor("SELECT COUNT(*) FROM Notificacion_interna WHERE id_evento=1"));
        assertTrue(controller.proveedoresParaCalificar(1).isEmpty());
        assertThrows(IllegalArgumentException.class,()->controller.cancelarEvento(1,true));
        sql("ALTER TABLE Notificacion_interna DROP CONSTRAINT fallo");
        // Otro controlador/repository simula reabrir la pantalla tras el error.
        new RepositoryFinalizacion(url,reloj).finalizar(1,sesion.getIdActor(),List.of());
        assertEquals("FINALIZADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
        assertFalse(controller.galeriaPendiente(1));
        assertEquals("1",valor("SELECT COUNT(*) FROM Fotografia_evento"));
        assertEquals("2",valor("SELECT COUNT(*) FROM Notificacion_interna WHERE id_evento=1"));
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,fotos));
        assertEquals("2",valor("SELECT COUNT(*) FROM Notificacion_interna WHERE id_evento=1"));
    }
    @Test void conservaReservasCompartidasConOtroEvento() throws Exception {
        sql("UPDATE Evento SET id_lugar=1 WHERE id_evento=2");
        sql("UPDATE Solicitud_Ejecucion SET id_servicio=1 WHERE id_solicitud=3");
        sql("INSERT INTO Contratacion (id_propuesta,id_evento,fecha_contratacion,estado,id_disponibilidad) VALUES (3,2,CURRENT_TIMESTAMP,'CONFIRMADA',1)");
        controller.finalizarEvento(1,fotos);
        assertEquals("RESERVADO",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        assertEquals("RESERVADO",valor("SELECT estado FROM Disponibilidad_Servicio WHERE id_disponibilidad=1"));
        assertEquals("PLANEADO",valor("SELECT estado FROM Evento WHERE id_evento=2"));
    }
    @Test void rechazaReservasSinVinculoValido() throws Exception {
        sql("UPDATE Contratacion SET id_disponibilidad=NULL WHERE id_contratacion=1");
        assertThrows(IllegalArgumentException.class,()->controller.finalizarEvento(1,fotos)); sinCambios();
    }
    @Test void sinConfirmadosFinalizaSinCrearAvisos() throws Exception {
        sql("UPDATE Invitado SET estado='RECHAZADO'");
        controller.finalizarEvento(1,fotos);
        assertEquals("FINALIZADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
        assertEquals("0",valor("SELECT COUNT(*) FROM Entrega_galeria"));
    }

    @Test void invitadoConfirmadoVeFotosPersistidasSoloConCorreoYTelefonoCorrectos() {
        controller.finalizarEvento(1,fotos);
        var invitados=new ServiceInvitados(new RepositoryInvitacion(url),new RepositoryInvitado(url),new RepositoryLugar(url),new RepositoryEvento(url));
        var compartidas=invitados.consultarGaleria(" SOFIA@EMAIL.COM ","300-555-1111");
        assertEquals(1,compartidas.size());
        assertArrayEquals(fotos.get(0).contenido(),compartidas.get(0).contenido());
        assertTrue(compartidas.get(0).nombre().contains("Boda Laura y Daniel"));
        assertTrue(invitados.consultarGaleria("sofia@email.com","9999999999").isEmpty());
        assertTrue(invitados.consultarGaleria("juan@email.com","3014442222").isEmpty());
        assertThrows(IllegalArgumentException.class,()->invitados.consultarGaleria("sofia@email.com",""));
        assertThrows(IllegalArgumentException.class,()->invitados.consultarGaleria("invalido","3005551111"));
    }

    @Test void noExponeGaleriaHastaCompletarElEnvio() throws Exception {
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo CHECK (id_evento IS NULL OR id_evento<>1)");
        assertThrows(IllegalStateException.class,()->controller.finalizarEvento(1,fotos));
        var invitado=new RepositoryInvitado(url);
        assertTrue(invitado.listarGaleria("sofia@email.com","3005551111").isEmpty());
        sql("ALTER TABLE Notificacion_interna DROP CONSTRAINT fallo");
        controller.finalizarEvento(1,List.of());
        assertEquals(1,invitado.listarGaleria("sofia@email.com","3005551111").size());
    }

    @Test void noIncluyeConfirmadosConInvitacionRevocada() throws Exception {
        sql("UPDATE Invitacion SET estado_respuesta='REVOCADA' WHERE id_invitacion=1");
        controller.finalizarEvento(1,fotos);
        assertTrue(new RepositoryInvitado(url).listarGaleria("sofia@email.com","3005551111").isEmpty());
        assertEquals("0",valor("SELECT COUNT(*) FROM Entrega_galeria WHERE id_invitado=1"));
    }

    @Test void cancelarElUltimoEventoCompartidoLiberaRecursosAunqueFalteEnviarLaGaleriaDelPrimero() throws Exception {
        sql("UPDATE Evento SET id_lugar=1 WHERE id_evento=2");
        sql("UPDATE Invitacion SET id_lugar=1 WHERE id_evento=2");
        sql("UPDATE Solicitud_Ejecucion SET id_servicio=1 WHERE id_solicitud=3");
        sql("INSERT INTO Contratacion (id_propuesta,id_evento,fecha_contratacion,estado,id_disponibilidad) VALUES (3,2,CURRENT_TIMESTAMP,'ACTIVA',1)");
        sql("ALTER TABLE Notificacion_interna ADD CONSTRAINT fallo CHECK (id_evento IS NULL OR id_evento<>1)");
        assertThrows(IllegalStateException.class,()->controller.finalizarEvento(1,fotos));
        assertEquals("RESERVADO",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        var otra=new ServiceAutenticacion(new RepositoryAutenticacion(url)).iniciarSesion("carlos.rodriguez","clave456");
        service.cancelarEvento(2,otra,true);
        assertEquals("DISPONIBLE",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        assertEquals("DISPONIBLE",valor("SELECT estado FROM Disponibilidad_Servicio WHERE id_disponibilidad=1"));
    }

    void sinCambios() throws Exception {
        assertEquals("CONFIRMADO",valor("SELECT estado FROM Evento WHERE id_evento=1"));
        assertEquals("RESERVADO",valor("SELECT estado FROM Lugar WHERE id_lugar=1"));
        assertEquals("RESERVADO",valor("SELECT estado FROM Disponibilidad_Servicio WHERE id_disponibilidad=1"));
        assertEquals("0",valor("SELECT COUNT(*) FROM Finalizacion_evento"));
    }
    void sql(String texto) throws Exception { try(Connection c=DatabaseConnection.getConnection(url);var s=c.createStatement()) { s.execute(texto); } }
    String valor(String texto) throws Exception { try(Connection c=DatabaseConnection.getConnection(url);var s=c.createStatement();var r=s.executeQuery(texto)) { r.next(); return r.getString(1); } }
}
