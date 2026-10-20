import config.DatabaseConnection;
import controllers.ControllerAdministracion;
import entities.NotificacionInterna;
import entities.Proveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import repositories.RepositoryProveedor;
import services.ServiceAdministracion;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAdministracionTest {

    @TempDir
    Path directorio;

    private RepositoryProveedor repository;
    private ServiceAdministracion service;
    private ControllerAdministracion controller;

    @BeforeEach
    void prepararBase() throws Exception {

        String url = "jdbc:h2:" + directorio.resolve(
                "administracion_" + UUID.randomUUID());

        try (Connection connection = DatabaseConnection.getConnection(url);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "INSERT INTO Usuario "
                    + "(id_usuario, nombre, usuario, contraseña, estado, "
                    + "fecha_registro, acepta_marketing, tipo_usuario) VALUES "
                    + "(1, 'Ana Proveedor', 'ana_proveedor', 'solo-prueba', "
                    + "'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR'), "
                    + "(2, 'Correo invalido', 'correo_invalido', 'solo-prueba', "
                    + "'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR'), "
                    + "(3, 'Proveedor sin documentos', 'sin_documentos', "
                    + "'solo-prueba', 'ACTIVO', CURRENT_TIMESTAMP, FALSE, "
                    + "'PROVEEDOR')"
            );

            statement.executeUpdate(
                    "INSERT INTO Proveedor "
                    + "(id_proveedor, id_usuario, nombre, empresa, correo, "
                    + "telefono, direccion, estado) VALUES "
                    + "(1, 1, 'Ana Proveedor', 'Flores Ana', "
                    + "'ana@flores.test', '3001234567', 'Bogotá', "
                    + "'PENDIENTE_VERIFICACION'), "
                    + "(2, 2, 'Proveedor Correo Inválido', 'Empresa', "
                    + "'no-es-correo', '3101234567', 'Bogotá', "
                    + "'PENDIENTE_VERIFICACION'), "
                    + "(3, 3, 'Proveedor sin documentos', 'Empresa Tres', "
                    + "'tercero@empresa.test', '3000000000', 'Bogotá', "
                    + "'PENDIENTE_VERIFICACION')"
            );

            statement.executeUpdate("INSERT INTO Usuario (id_usuario,nombre,usuario,contraseña,estado,fecha_registro,acepta_marketing,tipo_usuario) VALUES (10,'Admin pruebas','admin_prueba','clave','ACTIVO',CURRENT_TIMESTAMP,FALSE,'ADMINISTRADOR')");
            statement.executeUpdate("INSERT INTO Rol (id_rol,nombre,estado) VALUES (1,'ADMINISTRADOR','ACTIVO')");
            statement.executeUpdate("INSERT INTO Usuario_rol (id_usuario,id_rol) VALUES (10,1)");

            // Ana tiene un documento validado y vigente.
            // Esto permite probar el flujo principal de aprobación.
            statement.executeUpdate(
                    "INSERT INTO Documento_Proveedor "
                    + "(id_proveedor, tipo_documento, nombre_archivo, "
                    + "ruta_archivo, fecha_vencimiento, estado, observacion) "
                    + "VALUES (1, 'IDENTIFICACION', 'cedula-ana.pdf', "
                    + "'/pruebas/cedula-ana.pdf', "
                    + "DATEADD('DAY', 30, CURRENT_DATE), "
                    + "'VALIDADO', NULL)"
            );
        }

        repository = new RepositoryProveedor(url);
        service = new ServiceAdministracion(repository,new services.ServiceAutenticacion(
                new repositories.RepositoryAutenticacion(url)).iniciarSesion("admin_prueba","clave"));
        controller = new ControllerAdministracion(service);
    }

    @Test
    void flujoPrincipalValidaYApruebaProveedor() {

        assertTrue(controller.verificarInformacionProveedor(1));

        controller.aprobarProveedor(1);

        Proveedor proveedor = repository.buscarProveedor(1);

        assertEquals(
                Proveedor.ESTADO_VERIFICADO,
                proveedor.getEstado()
        );

        assertTrue(
                controller.consultarProveedoresPendientes()
                        .stream()
                        .noneMatch(p -> p.getIdProveedor() == 1)
        );

        List<NotificacionInterna> notificaciones =
                repository.listarNotificacionesPorUsuario(1);

        assertEquals(1, notificaciones.size());
        assertEquals(
                NotificacionInterna.TIPO_APROBACION,
                notificaciones.get(0).getTipo()
        );
    }

    @Test
    void noApruebaProveedorConCorreoMalFormado() {

        assertFalse(service.verificarInformacionProveedor(2));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.aprobarProveedor(2)
        );

        assertTrue(
                error.getMessage().contains("No se puede aprobar")
        );

        assertEquals(
                Proveedor.ESTADO_PENDIENTE_VERIFICACION,
                repository.buscarProveedor(2).getEstado()
        );

        assertTrue(
                repository.listarNotificacionesPorUsuario(2).isEmpty()
        );
    }

    @Test
    void noApruebaProveedorSinDocumentos() {

        assertFalse(service.verificarInformacionProveedor(3));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.aprobarProveedor(3)
        );

        assertEquals(
                Proveedor.ESTADO_PENDIENTE_VERIFICACION,
                repository.buscarProveedor(3).getEstado()
        );
    }

    @Test
    void rechazaProveedorSiElMotivoEstaVacio() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rechazarProveedor(1, "  ")
        );

        assertEquals(
                Proveedor.ESTADO_PENDIENTE_VERIFICACION,
                repository.buscarProveedor(1).getEstado()
        );

        assertTrue(
                repository.listarNotificacionesPorUsuario(1).isEmpty()
        );
    }

    @Test
    void rechazaProveedorYConservaElMotivo() {

        String motivo =
                "La documentación no coincide con los datos registrados.";

        controller.rechazarProveedor(1, motivo);

        Proveedor proveedor = repository.buscarProveedor(1);

        assertEquals(
                Proveedor.ESTADO_RECHAZADO,
                proveedor.getEstado()
        );

        assertEquals(
                motivo,
                proveedor.getMotivoRechazo()
        );

        List<NotificacionInterna> notificaciones =
                repository.listarNotificacionesPorUsuario(1);

        assertEquals(1, notificaciones.size());

        assertEquals(
                NotificacionInterna.TIPO_RECHAZO,
                notificaciones.get(0).getTipo()
        );

        assertTrue(
                notificaciones.get(0).getMensaje().contains(motivo)
        );
    }

    @Test
    void solicitarCorreccionesMantieneProveedorPendienteYNotifica() {

        String observacion =
                "Se debe corregir la información de contacto.";

        service.solicitarCorrecciones(2, observacion);

        assertEquals(
                Proveedor.ESTADO_PENDIENTE_VERIFICACION,
                repository.buscarProveedor(2).getEstado()
        );

        List<NotificacionInterna> notificaciones =
                repository.listarNotificacionesPorUsuario(2);

        assertEquals(1, notificaciones.size());

        assertEquals(
                NotificacionInterna.TIPO_CORRECCION,
                notificaciones.get(0).getTipo()
        );

        assertEquals(
                observacion,
                notificaciones.get(0).getMensaje()
        );
    }

    @Test
    void noPermiteAprobarUnIdentificadorInexistente() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.aprobarProveedor(999)
        );
    }

    @Test
    void limitaLaLongitudDelMotivoDeRechazo() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rechazarProveedor(1, "x".repeat(501))
        );

        assertEquals(
                Proveedor.ESTADO_PENDIENTE_VERIFICACION,
                repository.buscarProveedor(1).getEstado()
        );
    }
}