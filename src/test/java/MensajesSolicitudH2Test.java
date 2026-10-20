
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.DatabaseConnection;
import entities.MensajeSolicitud;
import entities.SesionUsuario;
import repositories.RepositorySolicitudEjecucion;
import repositories.RepositoryPropuestaServicio;
import services.ServiceSolicitudes;

public class MensajesSolicitudH2Test {

    private static final String URL =
            "jdbc:h2:mem:cu12mensajes;DB_CLOSE_DELAY=-1";

    private ServiceSolicitudes organizador;
    private ServiceSolicitudes proveedor;

    @BeforeEach
    void preparar() throws Exception {

        System.setProperty("nexteventux.db.url", URL);

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                    INSERT INTO Usuario
                    (id_usuario, nombre, usuario, contraseña,
                     estado, fecha_registro, acepta_marketing, tipo_usuario)
                    VALUES
                    (9201, 'Organizador prueba', 'orgcu12', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR'),
                    (9202, 'Proveedor prueba', 'provcu12', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'PROVEEDOR'),
                    (9203, 'Usuario ajeno', 'ajenocu12', 'clave',
                     'ACTIVO', CURRENT_TIMESTAMP, FALSE, 'ORGANIZADOR')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Organizador
                    (id_organizador, id_usuario, empresa, estado)
                    VALUES
                    (9201, 9201, 'Eventos prueba', 'ACTIVO'),
                    (9203, 9203, 'Otro organizador', 'ACTIVO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Proveedor
                    (id_proveedor, id_usuario, nombre, estado)
                    VALUES
                    (9202, 9202, 'Proveedor CU12', 'APROBADO')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Evento
                    (id_evento, id_organizador, nombre,
                     fecha_inicio, fecha_fin, estado, fecha_creacion)
                    VALUES
                    (9201, 9201, 'Evento CU12',
                     TIMESTAMP '2026-11-20 10:00:00',
                     TIMESTAMP '2026-11-20 18:00:00',
                     'PLANEADO', CURRENT_TIMESTAMP),
                    (9203, 9203, 'Evento ajeno',
                     TIMESTAMP '2026-11-21 10:00:00',
                     TIMESTAMP '2026-11-21 18:00:00',
                     'PLANEADO', CURRENT_TIMESTAMP)
                    """);

            statement.executeUpdate("""
                    INSERT INTO Servicio
                    (id_servicio, id_proveedor, nombre, estado)
                    VALUES
                    (9201, 9202, 'Fotografia CU12', 'DISPONIBLE')
                    """);

            statement.executeUpdate("""
                    INSERT INTO Solicitud_Ejecucion
                    (id_solicitud, id_proveedor, id_evento,
                     id_servicio, fecha_solicitud, estado)
                    VALUES
                    (9201, 9202, 9201, 9201,
                     CURRENT_TIMESTAMP, 'EN_NEGOCIACION'),
                    (9202, 9202, 9203, 9201,
                     CURRENT_TIMESTAMP, 'EN_NEGOCIACION')
                    """);
        }

        organizador = crearServicio(
                sesion("ORGANIZADOR", 9201, 9201));
        proveedor = crearServicio(
                sesion("PROVEEDOR", 9202, 9202));
    }

    private ServiceSolicitudes crearServicio(SesionUsuario sesion) {
        return new ServiceSolicitudes(
                new RepositorySolicitudEjecucion(),
                new RepositoryPropuestaServicio(),
                sesion);
    }

    private SesionUsuario sesion(
            String rol,
            int idActor,
            int idUsuario) {

        SesionUsuario sesion = new SesionUsuario();
        sesion.setTipoUsuario(rol);
        sesion.setIdActor(idActor);
        sesion.setIdUsuario(idUsuario);
        sesion.setEstadoUsuario("ACTIVO");
        sesion.setEstadoActor(
                "PROVEEDOR".equals(rol) ? "APROBADO" : "ACTIVO");
        return sesion;
    }

    @AfterEach
    void limpiar() throws Exception {

        try {
            try (Connection connection =
                         DriverManager.getConnection(URL, "sa", "");
                 Statement statement = connection.createStatement()) {

                statement.execute("DROP ALL OBJECTS");
            }
        } finally {
            System.clearProperty("nexteventux.db.url");
        }
    }

    @Test
    void organizadorYProveedorPuedenConversar() {

        organizador.enviarMensaje(
                9201, 9201,
                "Necesito fotografia para 100 personas.",
                null);

        proveedor.enviarMensaje(
                9201, 9202,
                "Claro, puedo enviarte una propuesta.",
                null);

        List<MensajeSolicitud> mensajes =
                organizador.listarMensajes(9201, 9201);

        assertEquals(2, mensajes.size());
        assertEquals(9201, mensajes.get(0).getIdUsuarioRemitente());
        assertEquals(9202, mensajes.get(1).getIdUsuarioRemitente());
    }

    @Test
    void usuarioAjenoNoPuedeLeerMensajes() {

        organizador.enviarMensaje(
                9201, 9201, "Mensaje privado.", null);

        assertThrows(
                IllegalStateException.class,
                () -> organizador.listarMensajes(9201, 9203));
        assertThrows(
                IllegalStateException.class,
                () -> organizador.listarMensajes(9202, 9201));
    }

    @Test
    void usuarioAjenoNoPuedeEnviarMensajes() {

        assertThrows(
                IllegalStateException.class,
                () -> organizador.enviarMensaje(
                        9201, 9203,
                        "No pertenezco a esta conversacion.",
                        null));
    }

    @Test
    void solicitudCerradaNoPermiteNuevosMensajes() throws Exception {

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                    UPDATE Solicitud_Ejecucion
                    SET estado = 'PROPUESTA_RECHAZADA'
                    WHERE id_solicitud = 9201
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> organizador.enviarMensaje(
                        9201, 9201, "Mensaje tardio.", null));

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE Solicitud_Ejecucion
                    SET estado = 'RECHAZADA'
                    WHERE id_solicitud = 9201
                    """);
        }

        assertThrows(
                IllegalStateException.class,
                () -> organizador.enviarMensaje(
                        9201, 9201, "Mensaje posterior al rechazo.", null));
    }

    @Test
    void mensajeValidaTextoYConservaUnicodeSaltosYCaracteres()
            throws Exception {

        String contenido = "Hola,\n¡confirmo la decoración! 🎪 <ok> 'sí'.";
        organizador.enviarMensaje(
                9201,
                9201,
                contenido,
                "  https://ejemplo.test/referencia  ");

        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT contenido, adjunto_url
                        FROM Mensaje
                        """)) {

            assertTrue(result.next());
            assertEquals(contenido, result.getString("contenido"));
            assertEquals(
                    "https://ejemplo.test/referencia",
                    result.getString("adjunto_url"));
        }

        assertEquals(
                contenido,
                organizador.listarMensajes(9201, 9201)
                        .get(0)
                        .getContenido());
    }

    @Test
    void mensajeVacioYLargoSeRechazaSinInsertar() throws Exception {

        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.enviarMensaje(
                        9201, 9201, null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.enviarMensaje(
                        9201, 9201, "   ", null));
        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.enviarMensaje(
                        9201, 9201, "x".repeat(1001), null));
        assertThrows(
                IllegalArgumentException.class,
                () -> organizador.enviarMensaje(
                        9201, 9201, "mensaje", "x".repeat(1001)));

        assertEquals(
                0,
                organizador.listarMensajes(9201, 9201).size());
    }

    @Test
    void sesionDeProveedorNoPuedeUsarIdDeUsuarioOrganizador() {

        assertThrows(
                IllegalStateException.class,
                () -> proveedor.enviarMensaje(
                        9201, 9201, "Intento de suplantacion.", null));
        assertThrows(
                IllegalStateException.class,
                () -> proveedor.listarMensajes(9201, 9201));
    }

    @Test
    void enviarYConsultarMensajesRequiereSesionAutenticada() {

        assertThrows(
                IllegalArgumentException.class,
                () -> crearServicio(null));
    }
}
