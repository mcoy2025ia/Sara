
import java.sql.Connection;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.DatabaseConnection;
import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;
import services.ServiceAutenticacion;

public class ServiceAutenticacionH2Test {

    private static final String TEST_URL
            = "jdbc:h2:mem:auth;DB_CLOSE_DELAY=-1";

    private ServiceAutenticacion service;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty(
                "nexteventux.db.url",
                TEST_URL);

        try (Connection connection
                = DatabaseConnection.getConnection(TEST_URL); Statement statement
                = connection.createStatement()) {

            assertTrue(
                    connection.getMetaData()
                            .getURL()
                            .startsWith("jdbc:h2:mem:auth"),
                    "La prueba debe utilizar exclusivamente H2 en memoria.");

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES
                    (8001, 'Proveedor prueba',
                     'proveedor.prueba', 'clave1',
                     'ACTIVO', CURRENT_TIMESTAMP,
                     FALSE, 'PROVEEDOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre,
                     empresa, correo, telefono,
                     direccion, estado)
                    VALUES
                    (8101, 8001, 'Proveedor prueba',
                     'Empresa prueba',
                     'proveedor@prueba.com',
                     '3000000000',
                     'Bogota',
                     'APROBADO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES
                    (8002, 'Organizador prueba',
                     'organizador.prueba', 'clave2',
                     'ACTIVO', CURRENT_TIMESTAMP,
                     FALSE, 'ORGANIZADOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario,
                     empresa, telefono, estado)
                    VALUES
                    (8201, 8002,
                     'Eventos prueba',
                     '3000000001',
                     'ACTIVO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES
                    (8003, 'Proveedor pendiente',
                     'proveedor.pendiente', 'clave3',
                     'ACTIVO', CURRENT_TIMESTAMP,
                     FALSE, 'PROVEEDOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre,
                     empresa, correo, telefono,
                     direccion, estado)
                    VALUES
                    (8102, 8003, 'Proveedor pendiente',
                     'Empresa pendiente',
                     'pendiente@prueba.com',
                     '3000000002',
                     'Bogota',
                     'PENDIENTE')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro,
                     acepta_marketing, tipo_usuario)
                    VALUES
                    (8004, 'Administrador prueba',
                     'admin.prueba', 'claveadmin',
                     'ACTIVO', CURRENT_TIMESTAMP,
                     FALSE, 'ADMINISTRADOR')
                    """);
        }

        service
                = new ServiceAutenticacion(
                        new RepositoryAutenticacion());
    }

    @AfterEach
    void limpiar() throws Exception {

        try {
            try (Connection connection
                    = DatabaseConnection.getConnection(TEST_URL); Statement statement
                    = connection.createStatement()) {

                assertTrue(
                        connection.getMetaData()
                                .getURL()
                                .startsWith("jdbc:h2:mem:auth"),
                        "La limpieza debe utilizar H2 en memoria.");

                statement.execute(
                        "DROP ALL OBJECTS");
            }
        } finally {
            System.clearProperty(
                    "nexteventux.db.url");
        }
    }

    @Test
    void proveedorAprobadoPuedeIngresar() {

        SesionUsuario sesion
                = service.iniciarSesion(
                        "proveedor.prueba",
                        "clave1");

        assertEquals(
                "PROVEEDOR",
                sesion.getTipoUsuario());

        assertEquals(
                8101,
                sesion.getIdActor());
    }

    @Test
    void organizadorActivoPuedeIngresar() {

        SesionUsuario sesion
                = service.iniciarSesion(
                        "organizador.prueba",
                        "clave2");

        assertEquals(
                "ORGANIZADOR",
                sesion.getTipoUsuario());

        assertEquals(
                8201,
                sesion.getIdActor());
    }

    @Test
    void proveedorPendienteNoPuedeIngresar() {

        IllegalStateException error
                = assertThrows(
                        IllegalStateException.class,
                        () -> service.iniciarSesion(
                                "proveedor.pendiente",
                                "clave3"));

        assertEquals(
                "El proveedor aun no esta aprobado.",
                error.getMessage());
    }

    @Test
    void credencialesIncorrectasNoPermitenIngresar() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.iniciarSesion(
                        "proveedor.prueba",
                        "incorrecta"));
    }

    @Test
    void administradorActivoPuedeIngresar() {

        SesionUsuario sesion
                = service.iniciarSesion(
                        "admin.prueba",
                        "claveadmin");

        assertEquals(
                "ADMINISTRADOR",
                sesion.getTipoUsuario());

        assertEquals(
                8004,
                sesion.getIdUsuario());
    }

    @Test
    void administradorInactivoNoPuedeIngresar() throws Exception {

        try (Connection connection
                = DatabaseConnection.getConnection(TEST_URL); Statement statement
                = connection.createStatement()) {

            statement.executeUpdate("""
                    UPDATE Usuario
                    SET estado = 'INACTIVO'
                    WHERE id_usuario = 8004
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> service.iniciarSesion(
                        "admin.prueba",
                        "claveadmin"));
    }
}
