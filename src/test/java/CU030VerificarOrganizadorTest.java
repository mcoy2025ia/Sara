import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import config.CargarDatosEjemplo;
import config.CargarOrganizadoresPruebaCU30;
import config.DatabaseConnection;
import controllers.ControllerVerificacionOrganizador;
import entities.EvaluacionOrganizador;
import entities.Organizador;
import entities.SesionUsuario;
import repositories.RepositoryAutenticacion;
import repositories.RepositoryOrganizador;
import services.ServiceAutenticacion;
import services.ServiceVerificacionOrganizador;

/**
 * CU-030: verificar, categorizar y aprobar organizador con H2 real.
 * Comprueba también que un organizador aprobado queda disponible para
 * CU-026 (contratar organizador), que filtra por estado VERIFICADO.
 */
class CU030VerificarOrganizadorTest {

    private String url;

    @BeforeEach
    void preparar() throws Exception {
        Path directorio = Files.createTempDirectory("cu30-");
        url = "jdbc:h2:" + directorio.resolve("verificacion");
        System.setProperty("nexteventux.db.url", url);
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarDatosEjemplo.cargar(connection);
            CargarOrganizadoresPruebaCU30.cargar(connection);
        }
    }

    @AfterEach
    void limpiar() {
        System.clearProperty("nexteventux.db.url");
    }

    private SesionUsuario ingresar(String usuario, String clave) {
        return new ServiceAutenticacion(new RepositoryAutenticacion()).iniciarSesion(usuario, clave);
    }

    private ControllerVerificacionOrganizador controlador(SesionUsuario sesion) {
        return new ControllerVerificacionOrganizador(
                new ServiceVerificacionOrganizador(new RepositoryOrganizador(url), sesion.getIdUsuario()),
                sesion);
    }

    private int idPendiente(ControllerVerificacionOrganizador controller, String nombre) {
        return controller.consultarOrganizadoresPendientes().stream()
                .filter(o -> o.nombre().equals(nombre))
                .mapToInt(Organizador::id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No está pendiente: " + nombre));
    }

    private String estadoVerificacion(int idOrganizador) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT estado_verificacion FROM Organizador WHERE id_organizador = ?")) {
            statement.setInt(1, idOrganizador);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString(1);
            }
        }
    }

    @Test
    void administradorApruebaOrganizadorYQuedaDisponibleParaContratar() throws Exception {
        ControllerVerificacionOrganizador controller = controlador(ingresar("admin.cu030", "AdminCU0302026"));
        int id = idPendiente(controller, "Valentina Eventos");

        assertTrue(controller.verificarPerfilParaAprobacion(id));
        EvaluacionOrganizador evaluacion = controller.evaluarOrganizador(id);
        assertNotNull(evaluacion.getCategoriaSugerida());
        if (evaluacion.isRequiereRevisionPresupuesto()) {
            controller.validarPresupuesto(evaluacion, true);
        }
        controller.aprobarOrganizador(evaluacion);

        assertEquals("VERIFICADO", estadoVerificacion(id));
        List<Organizador> pendientes = controller.consultarOrganizadoresPendientes();
        assertFalse(pendientes.stream().anyMatch(o -> o.id() == id));
        // Integración con CU-026: el cliente ya puede verlo y contratarlo.
        assertTrue(new RepositoryOrganizador(url).organizadorActivoVerificado(id));
    }

    @Test
    void rechazoExigeMotivoYRegistraElEstado() throws Exception {
        ControllerVerificacionOrganizador controller = controlador(ingresar("admin.cu030", "AdminCU0302026"));
        int id = idPendiente(controller, "Andrés Celebraciones");

        EvaluacionOrganizador sinMotivo = controller.evaluarOrganizador(id);
        assertThrows(IllegalArgumentException.class, () -> controller.rechazarOrganizador(sinMotivo, "  "));
        assertEquals("PENDIENTE", estadoVerificacion(id));

        controller.rechazarOrganizador(controller.evaluarOrganizador(id), "Portafolio insuficiente para la categoría.");
        assertEquals("RECHAZADO", estadoVerificacion(id));
        assertFalse(new RepositoryOrganizador(url).organizadorActivoVerificado(id));
    }

    @Test
    void noSePuedeDecidirDosVecesConLaMismaEvaluacion() throws Exception {
        ControllerVerificacionOrganizador controller = controlador(ingresar("admin.cu030", "AdminCU0302026"));
        int id = idPendiente(controller, "Valentina Eventos");
        EvaluacionOrganizador evaluacion = controller.evaluarOrganizador(id);
        if (evaluacion.isRequiereRevisionPresupuesto()) {
            controller.validarPresupuesto(evaluacion, true);
        }
        controller.aprobarOrganizador(evaluacion);

        assertThrows(RuntimeException.class,
                () -> controller.rechazarOrganizador(evaluacion, "Intento posterior a la aprobación."));
        assertEquals("VERIFICADO", estadoVerificacion(id));
    }

    @Test
    void unaSesionQueNoEsAdministrativaNoPuedeVerificar() {
        SesionUsuario organizador = ingresar("cu30.valentina", "PruebasCU30");
        assertEquals("ORGANIZADOR", organizador.getTipoUsuario());
        ControllerVerificacionOrganizador controller = controlador(organizador);

        assertThrows(IllegalStateException.class, controller::consultarOrganizadoresPendientes);
        assertThrows(IllegalStateException.class, () -> controller.aprobarOrganizador(1));
    }

    @Test
    void elCargadorDeDatosEsIdempotente() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection(url)) {
            CargarOrganizadoresPruebaCU30.cargar(connection);
        }
        ControllerVerificacionOrganizador controller = controlador(ingresar("admin.cu030", "AdminCU0302026"));
        long valentinas = controller.consultarOrganizadoresPendientes().stream()
                .filter(o -> o.nombre().equals("Valentina Eventos")).count();
        assertEquals(1, valentinas);
    }
}
