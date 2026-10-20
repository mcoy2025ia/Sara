import config.DatabaseConnection;
import controllers.ControllerAdministracion;
import entities.*;
import repositories.*;
import services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class SesionAdministracionTest {
    private String url;
    private RepositoryProveedor proveedores;
    private ServiceAutenticacion login;
    private SesionUsuario sesion;
    private ControllerAdministracion controller;

    @BeforeEach void preparar() throws Exception {
        url="jdbc:h2:mem:sesion_admin_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1";
        sql("INSERT INTO Usuario (id_usuario,nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) VALUES "
                +"(1,'Proveedor','proveedor','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'PROVEEDOR'),"
                +"(10,'Administradora','admin','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ADMINISTRADOR'),"
                +"(20,'Organizador','organizador','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ORGANIZADOR')");
        sql("INSERT INTO Proveedor (id_proveedor,id_usuario,nombre,correo,estado) VALUES (1,1,'Proveedor','proveedor@test.com','PENDIENTE_VERIFICACION')");
        sql("INSERT INTO Organizador (id_organizador,id_usuario,estado) VALUES (1,20,'ACTIVO')");
        sql("INSERT INTO Rol (id_rol,nombre,estado) VALUES (1,'ADMINISTRADOR','ACTIVO')");
        sql("INSERT INTO Usuario_rol (id_usuario,id_rol) VALUES (10,1)");
        sql("INSERT INTO Documento_Proveedor (id_proveedor,tipo_documento,nombre_archivo,ruta_archivo,fecha_vencimiento,estado) "
                +"VALUES (1,'IDENTIFICACION','documento.pdf','/pruebas/documento.pdf',DATEADD('DAY',30,CURRENT_DATE),'VALIDADO')");
        proveedores=new RepositoryProveedor(url);
        login=new ServiceAutenticacion(new RepositoryAutenticacion(url));
        sesion=login.iniciarSesion("admin","clave");
        controller=acceso(sesion);
    }

    @Test void administradorAutenticadoApruebaYElProveedorVerificadoPuedeIngresar() {
        assertEquals(1,controller.consultarProveedoresPendientes().size());
        controller.aprobarProveedor(1);
        assertEquals("VERIFICADO",proveedores.buscarProveedor(1).getEstado());
        assertEquals("VERIFICADO",login.iniciarSesion("proveedor","clave").getEstadoActor());
        assertEquals(1,controller.consultarNotificacionesPorUsuario(1).size());
    }

    @Test void conservaIngresoDeAprobadosAnterioresYRechazaPendientesRechazadosYUsuariosSuspendidos() throws Exception {
        sql("UPDATE Proveedor SET estado='APROBADO' WHERE id_proveedor=1");
        assertEquals("APROBADO",login.iniciarSesion("proveedor","clave").getEstadoActor());
        for(String estado:new String[]{"PENDIENTE_VERIFICACION","RECHAZADO"}) {
            sql("UPDATE Proveedor SET estado='"+estado+"' WHERE id_proveedor=1");
            assertThrows(IllegalStateException.class,()->login.iniciarSesion("proveedor","clave"));
        }
        sql("UPDATE Proveedor SET estado='VERIFICADO' WHERE id_proveedor=1");
        sql("UPDATE Usuario SET estado='INACTIVO' WHERE id_usuario=1");
        assertThrows(IllegalStateException.class,()->login.iniciarSesion("proveedor","clave"));
    }

    @Test void sinSesionNoPermiteConsultarModificarDocumentosNiRechazarProveedores() {
        comprobarBloqueo(new ControllerAdministracion(new ServiceAdministracion(proveedores)));
    }
    @Test void organizadorYProveedorNoPuedenAdministrar() throws Exception {
        comprobarBloqueo(acceso(login.iniciarSesion("organizador","clave")));
        sql("UPDATE Proveedor SET estado='VERIFICADO' WHERE id_proveedor=1");
        ControllerAdministracion proveedor=acceso(login.iniciarSesion("proveedor","clave"));
        assertThrows(IllegalStateException.class,proveedor::consultarProveedoresPendientes);
        assertThrows(IllegalStateException.class,()->proveedor.rechazarProveedor(1,"Sin permiso"));
        assertEquals("VERIFICADO",proveedores.buscarProveedor(1).getEstado());
    }
    @Test void cerrarSesionInvalidaElControladorAnterior() {
        controller.consultarProveedoresPendientes();
        sesion.cerrar(); comprobarBloqueo(controller);
    }
    @Test void suspenderUsuarioDespuesDelLoginBloqueaLasOperaciones() throws Exception {
        sql("UPDATE Usuario SET estado='INACTIVO' WHERE id_usuario=10"); comprobarBloqueo(controller);
    }
    @Test void retirarElRolDespuesDelLoginBloqueaLasOperaciones() throws Exception {
        sql("DELETE FROM Usuario_rol WHERE id_usuario=10"); comprobarBloqueo(controller);
    }
    @Test void desactivarElRolDespuesDelLoginBloqueaLasOperaciones() throws Exception {
        sql("UPDATE Rol SET estado='INACTIVO' WHERE id_rol=1"); comprobarBloqueo(controller);
    }
    @Test void noBastaCambiarElTipoDeLaSesionEnMemoria() {
        SesionUsuario organizador=login.iniciarSesion("organizador","clave");
        organizador.setTipoUsuario("ADMINISTRADOR"); comprobarBloqueo(acceso(organizador));
    }
    @Test void cambiarElTipoDeLaCuentaRealRevocaLaAdministracion() throws Exception {
        sql("UPDATE Usuario SET tipo_usuario='CLIENTE' WHERE id_usuario=10"); comprobarBloqueo(controller);
    }
    @Test void errorDeNotificacionRevierteLaAprobacion() throws Exception {
        sql("ALTER TABLE Notificacion_Proveedor ADD CONSTRAINT fallo CHECK (id_proveedor<>1)");
        assertThrows(RuntimeException.class,()->controller.aprobarProveedor(1));
        assertEquals("PENDIENTE_VERIFICACION",proveedores.buscarProveedor(1).getEstado());
        assertTrue(controller.consultarNotificacionesPorUsuario(1).isEmpty());
    }

    private ControllerAdministracion acceso(SesionUsuario s) {
        return new ControllerAdministracion(new ServiceAdministracion(proveedores,s));
    }
    private void comprobarBloqueo(ControllerAdministracion c) {
        assertThrows(IllegalStateException.class,c::consultarProveedoresPendientes);
        assertThrows(IllegalStateException.class,()->c.consultarProveedor(1));
        assertThrows(IllegalStateException.class,()->c.aprobarProveedor(1));
        assertThrows(IllegalStateException.class,()->c.rechazarProveedor(1,"Sin permiso"));
        assertThrows(IllegalStateException.class,()->c.solicitarCorrecciones(1,"Sin permiso"));
        assertThrows(IllegalStateException.class,()->c.marcarDocumentoValidado(1,1));
        assertThrows(IllegalStateException.class,()->c.consultarNotificacionesPorUsuario(1));
        assertEquals("PENDIENTE_VERIFICACION",proveedores.buscarProveedor(1).getEstado());
    }
    private void sql(String texto) throws Exception {
        try(Connection c=DatabaseConnection.getConnection(url); Statement s=c.createStatement()) { s.execute(texto); }
    }
}
